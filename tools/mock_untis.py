#!/usr/bin/env python3
"""Mock WebUntis server for end-to-end tests of the change watcher (stdlib only).

Serves the endpoints GreatUntis uses, built from the anonymised fixtures. Every requested date gets the
lessons of the fixture day 2026-11-03 (re-dated), so "today" always has lessons. The state file decides
whether the SP-LK2 lesson is regular or moved to the Foyer:

  python3 tools/mock_untis.py --port 8777 --state /tmp/gu-mock-state      # run
  echo foyer   > /tmp/gu-mock-state     # SP-LK2 → Foyer, teacher removed
  echo regular > /tmp/gu-mock-state     # back to normal
  echo cancel  > /tmp/gu-mock-state     # SP-LK2 cancelled

In the app (debug build) log in with host http://10.0.2.2:8777, school "mock", user "Muster_Max_20080101",
key JBSWY3DPEHPK3PXP. Any OTP is accepted except for user "fail".
"""
from __future__ import annotations

import argparse
import copy
import datetime as dt
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

FIX = Path(__file__).resolve().parent.parent / "app/src/test/resources/fixtures"
USERDATA = json.loads((FIX / "userdata.json").read_text())
TEMPLATE = json.loads((FIX / "timetable_day_foyer.json").read_text())["days"][0]
ABSENCES = json.loads((FIX / "absences_2026.json").read_text())


def _shift(s: str, day: dt.date) -> str:
    return day.isoformat() + s[10:]


def make_day(day: dt.date, mode: str) -> dict:
    d = copy.deepcopy(TEMPLATE)
    d["date"] = day.isoformat()
    if day.weekday() >= 5:
        d["gridEntries"] = []
        d["status"] = "NO_DATA"
        return d
    for g in d["gridEntries"]:
        g["duration"] = {k: _shift(v, day) for k, v in g["duration"].items()}
        g["ids"] = [int(day.strftime("%m%d")) * 1000 + i for i in g["ids"]][:len(g["ids"])]
        subj = (g["position1"] or [{}])[0].get("current", {}) or {}
        if subj.get("shortName") == "SP-LK2":
            room = {"type": "ROOM", "status": "REGULAR", "shortName": "R106", "longName": "Raum R106",
                    "displayName": "R106", "displayNameLabel": "NAME"}
            teacher = {"type": "TEACHER", "status": "REGULAR", "shortName": "T07", "longName": "Lehrkraft T07",
                       "displayName": "T07", "displayNameLabel": "NAME_LONGNAME"}
            if mode == "foyer":
                g["status"] = "CHANGED"
                g["position2"] = [{"current": None, "removed": dict(teacher, status="REMOVED")}]
                g["position3"] = [{"current": dict(room, shortName="Foyer", longName="Foyer", displayName="Foyer",
                                                   status="ADDED"),
                                   "removed": dict(room, status="REMOVED")}]
                g["substitutionText"] = "eigenverantwortliches Arbeiten"
            else:
                g["status"] = "CANCELLED" if mode == "cancel" else "REGULAR"
                g["position2"] = [{"current": teacher, "removed": None}]
                g["position3"] = [{"current": room, "removed": None}]
                g["substitutionText"] = "eigenverantwortliches Arbeiten" if mode == "cancel" else ""
    return d


class Handler(BaseHTTPRequestHandler):
    state: Path

    def _send(self, code: int, body, ctype="application/json"):
        data = body.encode() if isinstance(body, str) else json.dumps(body, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header("Content-Type", ctype + "; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        if self.path.startswith("/WebUntis/jsonrpc_intern.do"):
            self.send_header("Set-Cookie", "JSESSIONID=mock; Path=/WebUntis; HttpOnly")
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, fmt, *args):
        print(f"{dt.datetime.now():%H:%M:%S} {self.command} {self.path[:90]}", flush=True)

    def mode(self) -> str:
        try:
            return self.state.read_text().strip() or "regular"
        except FileNotFoundError:
            return "regular"

    def do_POST(self):
        u = urlparse(self.path)
        n = int(self.headers.get("Content-Length", 0))
        req = json.loads(self.rfile.read(n) or b"{}")
        if u.path == "/WebUntis/jsonrpc_intern.do":
            user = req["params"][0]["auth"]["user"]
            if user == "fail":
                return self._send(200, {"jsonrpc": "2.0", "id": req.get("id"),
                                        "error": {"code": -8504, "message": "bad credentials"}})
            return self._send(200, {"jsonrpc": "2.0", "id": req.get("id"), "result": USERDATA})
        if u.path == "/ms/schoolquery2":
            return self._send(200, json.loads((FIX / "schoolsearch.json").read_text()))
        self._send(404, {"error": "not found"})

    def do_GET(self):
        u = urlparse(self.path)
        q = {k: v[0] for k, v in parse_qs(u.query, keep_blank_values=True).items()}
        if u.path == "/WebUntis/api/token/new":
            if "JSESSIONID=mock" not in (self.headers.get("Cookie") or ""):
                return self._send(401, {"error": "no session"})
            return self._send(200, "mock.jwt.token", "text/plain")
        if not (self.headers.get("Authorization") or "").startswith("Bearer "):
            return self._send(401, {"error": "no token"})
        if u.path == "/WebUntis/api/rest/view/v1/timetable/entries":
            a, b = dt.date.fromisoformat(q["start"]), dt.date.fromisoformat(q["end"])
            days, d = [], a
            while d <= b:
                days.append(make_day(d, self.mode()))
                d += dt.timedelta(days=1)
            return self._send(200, {"format": 6, "days": days})
        if u.path == "/WebUntis/api/classreg/absences/students":
            return self._send(200, ABSENCES)
        self._send(404, {"error": "not found"})


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=8777)
    ap.add_argument("--state", type=Path, default=Path("/tmp/gu-mock-state"))
    a = ap.parse_args()
    Handler.state = a.state
    srv = ThreadingHTTPServer(("127.0.0.1", a.port), Handler)
    print(f"mock untis on http://127.0.0.1:{a.port} (state file {a.state})", flush=True)
    srv.serve_forever()


if __name__ == "__main__":
    main()
