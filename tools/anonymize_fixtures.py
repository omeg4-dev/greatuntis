#!/usr/bin/env python3
"""Anonymises test fixtures for GreatUntis (deterministic, idempotent, stdlib only).

Replaces real school data with synthetic data:
- School identity -> Musterschule Beispielstadt / demo-schule / demo.webuntis.com / 1234567
- Subject prefixes -> fixed permutation (seeded random.Random(4711).shuffle over sorted set)
  with zero course name collisions.
- Rooms -> R101..R126 in sorted order; AULA -> Aula; Foyer unchanged.
- userdata.json -> master rooms/subjects reduced to entries referenced in timetable fixtures.
- schoolsearch.json -> 3 synthetic schools.
- Writes a mapping report to ~/.cache/greatuntis/anon-map.txt.
"""
from __future__ import annotations

import json
import random
import re
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
FIXTURES = REPO / "app/src/test/resources/fixtures"
REPORT_PATH = Path.home() / ".cache/greatuntis/anon-map.txt"

GERMAN_TABLE = {
    "M": "Mathematik",
    "D": "Deutsch",
    "E": "Englisch",
    "IF": "Informatik",
    "PH": "Physik",
    "EK": "Erdkunde",
    "KR": "Kath. Religion",
    "SW": "Sozialwissenschaften",
    "SP": "Sport",
    "KU": "Kunst",
    "GE": "Geschichte",
    "BI": "Biologie",
    "CH": "Chemie",
    "F": "Französisch",
    "L": "Latein",
    "PL": "Philosophie",
    "MU": "Musik",
    "ER": "Ev. Religion",
    "S": "Spanisch",
    "PA": "Pädagogik",
}

SCHOOL_MAP = {
    "displayName": "Musterschule Beispielstadt",
    "loginName": "demo-schule",
    "host": "demo.webuntis.com",
    "schoolNumber": "1234567",
    "address": "Musterstraße 1, 12345 Beispielstadt",
}


def build_course_long_name(mapped_prefix: str, suffix: str) -> str:
    base = GERMAN_TABLE.get(mapped_prefix, mapped_prefix)
    if suffix.startswith("LK"):
        return f"{base} LK"
    elif suffix.startswith("GK"):
        return f"{base} GK"
    elif suffix.startswith("ZK"):
        return f"{base} ZK"
    elif suffix.startswith("PJK"):
        return f"{base} PJK"
    return f"{base} {suffix}"


def compute_mappings() -> tuple[dict[str, str], dict[str, tuple[str, str]], dict[str, tuple[str, str]]]:
    """Computes (prefix_map, course_map, room_map).

    course_map: orig_course -> (mapped_course, mapped_long_name)
    room_map: orig_room -> (mapped_room, mapped_long_name)
    """
    # 1. Subject prefixes & courses
    prefixes = sorted(["D", "E", "EK", "GE", "IF", "KR", "KU", "M", "PH", "PKI", "SP", "SW"])
    all_orig_courses = sorted([
        "D-GK4", "E-GK4", "EK-GK1", "EK-GK2", "EK-GK3", "GE-GK1", "GE-LK1",
        "IF-LK1", "KR-GK2", "KU-GK1", "M-LK2", "PH-GK1", "PKI-PJK1", "SP-GK1", "SW-ZK1"
    ])

    rnd = random.Random(4711)
    shuffled = prefixes[:]
    while True:
        rnd.shuffle(shuffled)
        if any(a == b for a, b in zip(prefixes, shuffled)):
            continue
        p_map = dict(zip(prefixes, shuffled))
        mapped_courses = {f"{p_map[c.split('-', 1)[0]]}-{c.split('-', 1)[1]}" for c in all_orig_courses}
        if not mapped_courses.intersection(all_orig_courses):
            break

    prefix_map = p_map
    course_map: dict[str, tuple[str, str]] = {}
    for c in all_orig_courses:
        p, s = c.split("-", 1)
        mp = prefix_map[p]
        ms = f"{mp}-{s}"
        course_map[c] = (ms, build_course_long_name(mp, s))

    # 2. Rooms
    # Rooms in timetable fixtures:
    # 010, 012, 020, 033, 035, 119, 131, 136, 137, 138, 147, 151, 154, 159,
    # 203, 204, 206, 208, 209, 211, 213, 214, 215, 216, AULA, Foyer, SPH 1, SPH 3
    orig_numbered_rooms = sorted([
        "010", "012", "020", "033", "035", "119", "131", "136", "137", "138",
        "147", "151", "154", "159", "203", "204", "206", "208", "209", "211",
        "213", "214", "215", "216", "SPH 1", "SPH 3"
    ])

    room_map: dict[str, tuple[str, str]] = {}
    for i, r in enumerate(orig_numbered_rooms, start=101):
        m_name = f"R{i}"
        room_map[r] = (m_name, f"Raum {m_name}")
    room_map["AULA"] = ("Aula", "Aula")
    room_map["Foyer"] = ("Foyer", "Foyer")

    return prefix_map, course_map, room_map


def build_token_replacer(prefix_map: dict[str, str], course_map: dict[str, tuple[str, str]]):
    token_map = {orig: mapped[0] for orig, mapped in course_map.items()}
    token_map.update(prefix_map)
    sorted_keys = sorted(token_map.keys(), key=len, reverse=True)
    pattern = re.compile(r"\b(" + "|".join(re.escape(k) for k in sorted_keys) + r")\b")

    def replacer(text: str) -> str:
        if not text:
            return text
        return pattern.sub(lambda m: token_map[m.group(1)], text)

    return replacer


def write_json(path: Path, data: dict | list) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")


def write_report(prefix_map: dict[str, str], course_map: dict[str, tuple[str, str]], room_map: dict[str, tuple[str, str]]) -> None:
    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        "============================================================",
        "GreatUntis Fixture Anonymisation Report",
        "============================================================",
        "",
        "--- School ---",
        "Name:    Musterschule Beispielstadt",
        "Login:   demo-schule",
        "Host:    demo.webuntis.com",
        "Number:  1234567",
        "Address: Musterstraße 1, 12345 Beispielstadt",
        "",
        "--- Subject Prefixes (fixed permutation seed 4711, 0 course collisions) ---",
    ]
    for p, mp in sorted(prefix_map.items()):
        lines.append(f"  {p:4s} -> {mp}")

    lines.append("")
    lines.append("--- Courses ---")
    for orig, (mapped, long_name) in sorted(course_map.items()):
        lines.append(f"  {orig:10s} -> {mapped:10s} ({long_name})")

    lines.append("")
    lines.append("--- Rooms ---")
    for orig, (mapped, long_name) in sorted(room_map.items()):
        lines.append(f"  {orig:10s} -> {mapped:10s} ({long_name})")

    lines.append("")
    REPORT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote mapping report to {REPORT_PATH}")


def anonymize_element(el: dict, course_map: dict[str, tuple[str, str]], room_map: dict[str, tuple[str, str]], replace_tokens) -> dict:
    el = dict(el)
    el_type = el.get("type")
    sn = el.get("shortName", "")

    if el_type == "SUBJECT":
        if sn in course_map:
            mapped_sn, mapped_ln = course_map[sn]
            el["shortName"] = mapped_sn
            el["longName"] = mapped_ln
            el["displayName"] = mapped_sn
    elif el_type == "ROOM":
        if sn in room_map:
            mapped_sn, mapped_ln = room_map[sn]
            el["shortName"] = mapped_sn
            el["longName"] = mapped_ln
            el["displayName"] = mapped_sn
        elif not sn.startswith("Foyer"):
            el["shortName"] = sn
    elif el_type == "INFO":
        mapped_text = replace_tokens(sn)
        el["shortName"] = mapped_text
        el["longName"] = replace_tokens(el.get("longName", mapped_text))
        el["displayName"] = mapped_text

    return el


def anonymize_slot(slot: dict | None, course_map: dict[str, tuple[str, str]], room_map: dict[str, tuple[str, str]], replace_tokens) -> dict | None:
    if not slot:
        return slot
    out = dict(slot)
    if "current" in out and out["current"]:
        out["current"] = anonymize_element(out["current"], course_map, room_map, replace_tokens)
    if "removed" in out and out["removed"]:
        out["removed"] = anonymize_element(out["removed"], course_map, room_map, replace_tokens)
    return out


def anonymize_timetables(course_map: dict[str, tuple[str, str]], room_map: dict[str, tuple[str, str]], replace_tokens) -> None:
    tt_files = [
        "timetable_2025.json",
        "timetable_2026.json",
        "timetable_day_foyer.json",
        "timetable_window.json",
    ]
    for fn in tt_files:
        path = FIXTURES / fn
        data = json.loads(path.read_text(encoding="utf-8"))
        for day in data.get("days", []):
            for g in day.get("gridEntries", []):
                for k in list(g.keys()):
                    if k.startswith("position") and g[k]:
                        g[k] = [anonymize_slot(s, course_map, room_map, replace_tokens) for s in g[k]]
                for text_field in ("lessonInfo", "lessonText", "substitutionText", "notesAll", "name"):
                    if g.get(text_field):
                        g[text_field] = replace_tokens(g[text_field])
                if g.get("texts"):
                    for t_obj in g["texts"]:
                        if isinstance(t_obj, dict) and t_obj.get("text"):
                            t_obj["text"] = replace_tokens(t_obj["text"])
        write_json(path, data)
        print(f"anonymised {fn}")


def anonymize_exams(replace_tokens) -> None:
    path = FIXTURES / "exams_2026.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    for ex in data.get("exams", []):
        if "name" in ex and ex["name"]:
            ex["name"] = replace_tokens(ex["name"])
        if "text" in ex and ex["text"]:
            ex["text"] = replace_tokens(ex["text"])
    write_json(path, data)
    print("anonymised exams_2026.json")


def anonymize_userdata(course_map: dict[str, tuple[str, str]], room_map: dict[str, tuple[str, str]]) -> None:
    path = FIXTURES / "userdata.json"
    data = json.loads(path.read_text(encoding="utf-8"))

    # userData
    data["userData"]["schoolName"] = SCHOOL_MAP["displayName"]

    # masterData
    md = data["masterData"]

    # Reduce masterData subjects to only the 13 referenced subjects and map them
    orig_subjs = md.get("subjects", [])
    kept_subjs = []
    for s in orig_subjs:
        name = s.get("name")
        if name in course_map:
            mapped_sn, mapped_ln = course_map[name]
            s_copy = dict(s)
            s_copy["name"] = mapped_sn
            s_copy["longName"] = mapped_ln
            kept_subjs.append(s_copy)
    md["subjects"] = kept_subjs

    # Reduce masterData rooms to only the 28 referenced rooms and map them
    orig_rooms = md.get("rooms", [])
    kept_rooms = []
    for r in orig_rooms:
        name = r.get("name")
        if name in room_map:
            mapped_sn, mapped_ln = room_map[name]
            r_copy = dict(r)
            r_copy["name"] = mapped_sn
            r_copy["longName"] = mapped_ln
            kept_rooms.append(r_copy)
    md["rooms"] = kept_rooms

    md["teachers"] = []
    md["klassen"] = []

    write_json(path, data)
    print("anonymised userdata.json (reduced rooms to 28, subjects to 13)")


def anonymize_schoolsearch() -> None:
    path = FIXTURES / "schoolsearch.json"
    schools = [
        {
            "server": SCHOOL_MAP["host"],
            "useMobileServiceUrlAndroid": False,
            "address": SCHOOL_MAP["address"],
            "displayName": SCHOOL_MAP["displayName"],
            "loginName": SCHOOL_MAP["loginName"],
            "schoolId": int(SCHOOL_MAP["schoolNumber"]),
            "useMobileServiceUrlIos": False,
            "serverUrl": f"https://{SCHOOL_MAP['host']}/WebUntis/?school={SCHOOL_MAP['loginName']}",
            "tenantId": SCHOOL_MAP["schoolNumber"],
            "mobileServiceUrl": None,
        },
        {
            "server": "gym-nordstadt.webuntis.com",
            "useMobileServiceUrlAndroid": False,
            "address": "Nordstraße 10, 12345 Nordstadt",
            "displayName": "Gymnasium Nordstadt",
            "loginName": "gym-nordstadt",
            "schoolId": 2345678,
            "useMobileServiceUrlIos": False,
            "serverUrl": "https://gym-nordstadt.webuntis.com/WebUntis/?school=gym-nordstadt",
            "tenantId": "2345678",
            "mobileServiceUrl": None,
        },
        {
            "server": "ge-sued.webuntis.com",
            "useMobileServiceUrlAndroid": False,
            "address": "Südstraße 20, 12345 Südstadt",
            "displayName": "Gesamtschule Süd",
            "loginName": "ge-sued",
            "schoolId": 3456789,
            "useMobileServiceUrlIos": False,
            "serverUrl": "https://ge-sued.webuntis.com/WebUntis/?school=ge-sued",
            "tenantId": "3456789",
            "mobileServiceUrl": None,
        },
    ]
    data = {
        "result": {
            "size": len(schools),
            "schools": schools,
        },
        "id": "1",
        "jsonrpc": "2.0",
    }
    write_json(path, data)
    print("anonymised schoolsearch.json (3 synthetic schools)")


def is_already_anonymized() -> bool:
    ud_path = FIXTURES / "userdata.json"
    if not ud_path.exists():
        return False
    try:
        ud = json.loads(ud_path.read_text(encoding="utf-8"))
        return ud.get("userData", {}).get("schoolName") == SCHOOL_MAP["displayName"]
    except Exception:
        return False


def main() -> None:
    prefix_map, course_map, room_map = compute_mappings()
    write_report(prefix_map, course_map, room_map)

    if is_already_anonymized():
        print("Fixtures are already anonymised; nothing to do.")
        return

    replace_tokens = build_token_replacer(prefix_map, course_map)
    anonymize_schoolsearch()
    anonymize_userdata(course_map, room_map)
    anonymize_exams(replace_tokens)
    anonymize_timetables(course_map, room_map, replace_tokens)
    print("All fixtures anonymised successfully.")


if __name__ == "__main__":
    main()
