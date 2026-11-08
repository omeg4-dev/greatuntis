#!/usr/bin/env python3
"""Reference WebUntis client for GreatUntis (stdlib + requests).

Used to (1) explore the API, (2) record anonymised test fixtures for the Kotlin app,
(3) compute reference absence statistics that the Kotlin `AbsenceStats` must reproduce.

Credentials: ~/.config/greatuntis/account.json (never in the repo), keys
url, school, user, key  — the same fields the Untis QR code carries.

  python3 tools/untis_probe.py login
  python3 tools/untis_probe.py timetable 2026-11-03 [END]
  python3 tools/untis_probe.py absences 2026-08-31 2026-11-06
  python3 tools/untis_probe.py record            # writes app/src/test/resources/fixtures/*.json
  python3 tools/untis_probe.py resynth           # offline: synthetic absences + expected stats
  python3 tools/untis_probe.py stats FIXTURE_TT FIXTURE_ABS FIXTURE_USERDATA   # offline, prints JSON
"""
from __future__ import annotations

import base64
import datetime as dt
import hashlib
import hmac
import json
import struct
import sys
import time
from collections import Counter, defaultdict
from pathlib import Path

ACCOUNT = Path.home() / ".config/greatuntis/account.json"
REPO = Path(__file__).resolve().parent.parent
FIXTURES = REPO / "app/src/test/resources/fixtures"

# ---------------------------------------------------------------- auth / client


def totp(secret: str, at: float | None = None, step: int = 30, digits: int = 6) -> int:
    """RFC 6238 TOTP, SHA1. Secret is base32 (padding optional, case-insensitive)."""
    s = secret.strip().replace(" ", "").upper()
    key = base64.b32decode(s + "=" * ((8 - len(s) % 8) % 8))
    counter = int((time.time() if at is None else at) // step)
    h = hmac.new(key, struct.pack(">Q", counter), hashlib.sha1).digest()
    o = h[-1] & 0x0F
    return (struct.unpack(">I", h[o : o + 4])[0] & 0x7FFFFFFF) % (10**digits)


class Untis:
    def __init__(self, acc: dict):
        import requests  # only needed for live commands

        self.acc = acc
        self.host = "https://" + acc["url"].removeprefix("https://").strip("/")
        self.s = requests.Session()
        self.token = None
        self.token_at = 0.0
        self.user_data = None

    def _auth(self) -> dict:
        return {"clientTime": int(time.time() * 1000), "user": self.acc["user"], "otp": totp(self.acc["key"])}

    def rpc(self, method: str, params: dict) -> dict:
        p = dict(params, auth=self._auth())
        r = self.s.post(
            f"{self.host}/WebUntis/jsonrpc_intern.do",
            params={"m": method, "school": self.acc["school"], "v": "i2.2"},
            json={"id": "1", "jsonrpc": "2.0", "method": method, "params": [p]},
            timeout=20,
        )
        r.raise_for_status()
        j = r.json()
        if "error" in j:
            raise RuntimeError(f"{method}: {j['error']}")
        return j["result"]

    def login(self) -> dict:
        self.user_data = self.rpc("getUserData2017", {})
        return self.user_data

    def bearer(self) -> str:
        if self.token is None or time.time() - self.token_at > 12 * 60:
            r = self.s.get(f"{self.host}/WebUntis/api/token/new", timeout=20)
            r.raise_for_status()
            self.token, self.token_at = r.text.strip(), time.time()
        return self.token

    def rest(self, path: str, **params) -> dict:
        r = self.s.get(self.host + path, params=params, headers={"Authorization": "Bearer " + self.bearer()}, timeout=60)
        r.raise_for_status()
        return r.json()

    @property
    def person_id(self) -> int:
        return self.user_data["userData"]["elemId"]

    def timetable(self, start: str, end: str) -> dict:
        return self.rest(
            "/WebUntis/api/rest/view/v1/timetable/entries",
            start=start, end=end, format=2, resourceType="STUDENT", resources=self.person_id,
            periodTypes="", timetableType="MY_TIMETABLE",
        )

    def absences(self, start: str, end: str) -> dict:
        return self.rest(
            "/WebUntis/api/classreg/absences/students",
            startDate=start.replace("-", ""), endDate=end.replace("-", ""),
            studentId=self.person_id, excuseStatusId=-1,
        )

    def exams(self, start: str, end: str) -> dict:
        return self.rpc("getExams2017", {"id": self.person_id, "type": "STUDENT", "startDate": start, "endDate": end})


def load_account() -> dict:
    return json.loads(ACCOUNT.read_text())


# ---------------------------------------------------------------- anonymisation


class Anon:
    """Replaces person names (teachers, users, the student) with stable placeholders."""

    def __init__(self):
        self.teachers: dict[str, str] = {}

    def teacher(self, el: dict | None) -> dict | None:
        if not el:
            return el
        code = self.teachers.setdefault(el["shortName"], f"T{len(self.teachers) + 1:02d}")
        return dict(el, shortName=code, longName=f"Lehrkraft {code}", displayName=f"{code} (Lehrkraft {code})")

    def timetable(self, tt: dict) -> dict:
        tt = json.loads(json.dumps(tt))

        def walk(o):
            if isinstance(o, dict):
                if o.get("type") == "TEACHER" and "shortName" in o:
                    return self.teacher(o)
                if o.get("type") == "STUDENT" and "shortName" in o:
                    return dict(o, shortName="student", longName="Student", displayName="student")
                out = {k: walk(v) for k, v in o.items()}
                if "userName" in out:
                    out["userName"] = None
                return out
            if isinstance(o, list):
                return [walk(x) for x in o]
            return o

        tt = walk(tt)
        for d in tt["days"]:
            d["resource"] = {"id": 1, "shortName": "student", "longName": "Student", "displayName": ""}
            # backEntries overlay the student's own absences (sick days, doctor visits) -> never keep them
            d["backEntries"] = []
        return tt

    @staticmethod
    def user_data(ud: dict) -> dict:
        md = ud["masterData"]
        keep = ("timeStamp", "absenceReasons", "excuseStatuses", "holidays", "rooms", "subjects", "schoolyears", "timeGrid")
        md = dict(md, rooms=[dict(r, longName=r["name"]) for r in md.get("rooms", [])])
        return {
            "masterData": {k: md[k] for k in keep if k in md} | {"teachers": [], "klassen": []},
            "userData": dict(ud["userData"], elemId=1, displayName="Student"),
            "settings": ud.get("settings", {}),
        }


# ---------------------------------------------------------------- synthetic absences
#
# Real absences (sick days, doctor visits) are health data and must never be committed. Fixtures get
# synthetic absences generated from the (public-safe) timetable instead; the shape matches the REST API.

SYNTH_PLAN = [
    # (reason, excuseStatus, kind)  kind: day | days3 | units2 | late
    ("Erkrankung", "entschuldigt", "day"),
    ("Erkrankung", "entschuldigt", "day"),
    ("Erkrankung", "entschuldigt", "days3"),
    ("Erkrankung", "nicht entschuldigt", "day"),
    ("Erkrankung", None, "day"),
    ("Arzttermin", "entschuldigt", "units2"),
    ("Arzttermin", "entschuldigt", "units2"),
    ("Schultermin", "zählt nicht", "day"),
    ("Schultermin", "zählt nicht", "units2"),
    ("Abwesend", None, "units2"),
    ("Abwesend", "nicht akzeptiert", "units2"),
    ("Verspätung", "nicht entschuldbar", "late"),
    ("Verspätung", "nicht entschuldbar", "late"),
    ("Verspätung", "entschuldigt", "late"),
]


def synth_absences(tt: dict, seed: int) -> dict:
    import random

    rnd = random.Random(seed)
    school_days = sorted(
        d["date"] for d in tt["days"]
        if d["status"] == "REGULAR" and any(g["status"] != "CANCELLED" for g in d["gridEntries"])
    )
    by_date = {d["date"]: d for d in tt["days"]}
    picks = sorted(rnd.sample(school_days, min(len(SYNTH_PLAN), len(school_days))))
    plan = SYNTH_PLAN[:]
    rnd.shuffle(plan)
    out = []
    for i, (date, (reason, status, kind)) in enumerate(zip(picks, plan)):
        lessons = sorted((g for g in by_date[date]["gridEntries"] if g["status"] != "CANCELLED"),
                         key=lambda g: g["duration"]["start"])
        t = lambda iso: int(iso[11:13]) * 100 + int(iso[14:16])
        d0 = d1 = int(date.replace("-", ""))
        if kind == "late":
            st = t(lessons[0]["duration"]["start"]); et = st + rnd.randint(3, 9)
        elif kind == "units2":
            g = lessons[rnd.randrange(len(lessons))]
            st, et = t(g["duration"]["start"]), t(g["duration"]["end"])
        else:
            st, et = 800, max(t(g["duration"]["end"]) for g in lessons)
            if kind == "days3":
                later = [x for x in school_days if x > date][:2]
                if later:
                    d1 = int(later[-1].replace("-", ""))
        out.append({
            "id": 900000 + i, "startDate": d0, "endDate": d1, "startTime": st, "endTime": et,
            "createDate": 0, "lastUpdate": 0, "createdUser": "user", "updatedUser": "user",
            "reasonId": 0, "reason": reason, "text": "", "interruptions": [], "canEdit": False,
            "studentName": "Student", "excuseStatus": status, "isExcused": status in ("entschuldigt", "zählt nicht"),
            "excuse": None if status is None else {"id": 800000 + i, "text": "", "excuseDate": d0,
                                                   "excuseStatus": status, "userId": 0, "username": "user"},
        })
    return {"data": {"absences": out, "absenceReasons": [], "excuseStatuses": None,
                     "showAbsenceReasonChange": False, "showCreateAbsence": False}}


# ---------------------------------------------------------------- statistics (reference implementation)
#
# Definitions the Kotlin AbsenceStats must reproduce exactly:
# * A "lesson unit" is one 45-min period of the school timeGrid. A gridEntry covers the units whose
#   [start,end] lies inside entry.duration (normally len(entry.ids) units).
# * held unit: entry.status != "CANCELLED" and entry.type != "EVENT".
# * subject of an entry: position1[0].current.shortName, else position1[0].removed.shortName, else entry.type.
# * Absences with reason "Verspätung" are LATENESS: excluded from missed units, counted in lateness
#   (minutes = end - start on that day).
# * Other absences cover [startDate startTime, endDate endTime] (local time; HHMM ints). A held unit is
#   MISSED if it overlaps the absence interval by >= 1 minute. A unit missed by several absences counts once
#   and takes the category of the first absence (sorted by start) that covers it.
# * Category from excuseStatus: "entschuldigt" -> excused, "zählt nicht" -> not_counted, None -> open,
#   everything else ("nicht entschuldigt", "nicht akzeptiert", "nicht entschuldbar") -> unexcused.


def hhmm(v: int) -> dt.time:
    return dt.time(v // 100, v % 100)


def parse_iso_local(s: str) -> dt.datetime:
    return dt.datetime.fromisoformat(s[:16])


def units_of_entry(g: dict, grid: list[tuple[dt.time, dt.time]]) -> list[tuple[dt.datetime, dt.datetime]]:
    a, b = parse_iso_local(g["duration"]["start"]), parse_iso_local(g["duration"]["end"])
    out = []
    for us, ue in grid:
        s, e = dt.datetime.combine(a.date(), us), dt.datetime.combine(a.date(), ue)
        if s >= a and e <= b:
            out.append((s, e))
    return out or [(a, b)]


def subject_of(g: dict) -> str:
    p = (g.get("position1") or [None])[0]
    if p:
        el = p.get("current") or p.get("removed")
        if el:
            return el["shortName"]
    return g["type"]


def category(status: str | None) -> str:
    if status is None:
        return "open"
    if status == "entschuldigt":
        return "excused"
    if status == "zählt nicht":
        return "not_counted"
    return "unexcused"


def absence_interval(a: dict) -> tuple[dt.datetime, dt.datetime]:
    d0 = dt.datetime.strptime(str(a["startDate"]), "%Y%m%d").date()
    d1 = dt.datetime.strptime(str(a["endDate"]), "%Y%m%d").date()
    return dt.datetime.combine(d0, hhmm(a["startTime"])), dt.datetime.combine(d1, hhmm(a["endTime"]))


def compute_stats(tt: dict, ab: dict, ud: dict) -> dict:
    grid = [
        (dt.time.fromisoformat(u["startTime"][1:]), dt.time.fromisoformat(u["endTime"][1:]))
        for u in ud["masterData"]["timeGrid"]["days"][0]["units"]
    ]
    units = []  # (start, end, subject, held)
    for d in tt["days"]:
        for g in d["gridEntries"]:
            held = g["status"] != "CANCELLED" and g["type"] != "EVENT"
            for s, e in units_of_entry(g, grid):
                units.append((s, e, subject_of(g), held))
    units.sort()

    absences = sorted(ab["data"]["absences"], key=lambda a: (a["startDate"], a["startTime"]))
    late = [a for a in absences if a["reason"] == "Verspätung"]
    real = [(absence_interval(a), category(a["excuseStatus"]), a) for a in absences if a["reason"] != "Verspätung"]

    held_by_subject = Counter(u[2] for u in units if u[3])
    missed_by_subject = Counter()
    missed_by_cat = Counter()
    missed_by_weekday = Counter()
    missed_by_period = Counter()
    missed_by_week = Counter()
    missed_days = set()
    for s, e, subj, held in units:
        if not held:
            continue
        for (a0, a1), cat, _ in real:
            overlap = (min(e, a1) - max(s, a0)).total_seconds() / 60
            if overlap >= 1:
                missed_by_subject[subj] += 1
                missed_by_cat[cat] += 1
                missed_by_weekday[s.isoweekday()] += 1
                idx = next((i + 1 for i, (us, _) in enumerate(grid) if us == s.time()), 0)
                missed_by_period[idx] += 1
                missed_by_week[(s.date() - dt.timedelta(days=s.weekday())).isoformat()] += 1
                missed_days.add(s.date().isoformat())
                break

    late_minutes = 0
    late_by_weekday = Counter()
    for a in late:
        (a0, a1) = absence_interval(a)
        late_minutes += int((a1 - a0).total_seconds() // 60)
        late_by_weekday[a0.isoweekday()] += 1

    cancelled = Counter()
    foyer = Counter()
    for d in tt["days"]:
        for g in d["gridEntries"]:
            n = len(units_of_entry(g, grid))
            if g["status"] == "CANCELLED":
                cancelled[subject_of(g)] += n
            rooms = [slot.get("current") for slot in (g.get("position3") or [])]
            if any(r and r["shortName"].startswith("Foyer") and r.get("status") == "ADDED" for r in rooms):
                foyer[subject_of(g)] += n

    return {
        "heldUnits": sum(held_by_subject.values()),
        "missedUnits": sum(missed_by_subject.values()),
        "missedDays": len(missed_days),
        "missedByCategory": dict(sorted(missed_by_cat.items())),
        "missedBySubject": dict(sorted(missed_by_subject.items())),
        "heldBySubject": dict(sorted(held_by_subject.items())),
        "missedByWeekday": {str(k): v for k, v in sorted(missed_by_weekday.items())},
        "missedByPeriod": {str(k): v for k, v in sorted(missed_by_period.items())},
        "missedByWeek": dict(sorted(missed_by_week.items())),
        "absenceCount": len(real),
        "openAbsences": sum(1 for _, c, _ in real if c == "open"),
        "lateCount": len(late),
        "lateMinutes": late_minutes,
        "lateByWeekday": {str(k): v for k, v in sorted(late_by_weekday.items())},
        "cancelledBySubject": dict(sorted(cancelled.items())),
        "foyerBySubject": dict(sorted(foyer.items())),
    }


# ---------------------------------------------------------------- commands


def write(name: str, obj) -> None:
    FIXTURES.mkdir(parents=True, exist_ok=True)
    (FIXTURES / name).write_text(json.dumps(obj, ensure_ascii=False, indent=1) + "\n")
    print("wrote", (FIXTURES / name).relative_to(REPO), f"{(FIXTURES / name).stat().st_size // 1024} KB")


def cmd_record() -> None:
    u = Untis(load_account())
    ud = u.login()
    anon = Anon()
    write("userdata.json", Anon.user_data(ud))
    write("timetable_2025.json", anon.timetable(u.timetable("2025-08-25", "2026-07-19")))
    write("timetable_2026.json", anon.timetable(u.timetable("2026-08-31", "2026-11-09")))
    resynth()
    write("timetable_day_foyer.json", anon.timetable(u.timetable("2026-11-03", "2026-11-03")))
    write("timetable_window.json", anon.timetable(u.timetable("2026-11-06", "2026-11-10")))
    ex = u.exams("2026-08-31", "2027-07-18")
    write("exams_2026.json", ex)
    import requests

    r = requests.post(
        "https://mobile.webuntis.com/ms/schoolquery2",
        json={"id": "1", "jsonrpc": "2.0", "method": "searchSchool", "params": [{"search": "demo-schule"}]},
        timeout=20,
    )
    write("schoolsearch.json", r.json())


def resynth() -> None:
    """(Re)builds synthetic absences + expected stats from the committed timetable fixtures (offline)."""
    for y, seed in (("2025", 2025), ("2026", 2026)):
        tt = json.loads((FIXTURES / f"timetable_{y}.json").read_text())
        for d in tt["days"]:
            d["backEntries"] = []
        write(f"timetable_{y}.json", tt)
        write(f"absences_{y}.json", synth_absences(tt, seed))
    for name in ("timetable_day_foyer.json", "timetable_window.json"):
        tt = json.loads((FIXTURES / name).read_text())
        for d in tt["days"]:
            d["backEntries"] = []
        write(name, tt)
    for y in ("2025", "2026"):
        st = compute_stats(
            json.loads((FIXTURES / f"timetable_{y}.json").read_text()),
            json.loads((FIXTURES / f"absences_{y}.json").read_text()),
            json.loads((FIXTURES / "userdata.json").read_text()),
        )
        write(f"stats_expected_{y}.json", st)


def main(argv: list[str]) -> None:
    cmd = argv[1] if len(argv) > 1 else "login"
    if cmd == "record":
        return cmd_record()
    if cmd == "resynth":
        return resynth()
    if cmd == "stats":
        tt, ab, ud = (json.loads(Path(p).read_text()) for p in argv[2:5])
        print(json.dumps(compute_stats(tt, ab, ud), ensure_ascii=False, indent=1))
        return
    u = Untis(load_account())
    ud = u.login()
    if cmd == "login":
        print(json.dumps(ud["userData"], ensure_ascii=False, indent=1))
    elif cmd == "timetable":
        print(json.dumps(u.timetable(argv[2], argv[3] if len(argv) > 3 else argv[2]), ensure_ascii=False, indent=1))
    elif cmd == "absences":
        print(json.dumps(u.absences(argv[2], argv[3]), ensure_ascii=False, indent=1))
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv)
