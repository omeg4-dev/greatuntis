# GreatUntis

Android-App für WebUntis: **sofort benachrichtigt, wenn eine Stunde ins Foyer verlegt wird oder entfällt** –
auch wenn die Änderung erst kurz vor Schulbeginn eingetragen wird.

*Android companion app for WebUntis students: minute-by-minute change alerts (cancellations, room moves),
a clean timetable and absence statistics. UI in German.*

## Funktionen

- **Änderungs-Alarm** – prüft während der Schulzeit jede Minute deinen Stundenplan.
  - Laut: Stunde ins **Foyer** verlegt, **Entfall**, **Unterricht beginnt später**
  - Leise: Raumänderung, Vertretung, Klausuren, Zusatzstunden, „wieder regulär“
  - Jede Art einzeln abschaltbar.
- **Stundenplan** – „Heute“ mit Live-Countdown („noch 23 min“, Pause, „Geschafft“) und „Woche“ als Raster mit
  Pausen, Jetzt-Linie und parallelen Stunden nebeneinander. Was du frei bekommst, ist grün markiert:
  Entfall, späterer Beginn („Beginn 09:55 statt 08:00“), früher Schluss, ganze freie Tage (z. B.
  Pädagogischer Tag). Tage, die die Schule noch nicht freigegeben hat, werden aus den letzten Wochen
  vorhergesagt („Prognose“).
- **Frei** – Countdown bis zu den nächsten Ferien in Schultagen, Schuljahres-Fortschritt, alle Ferien,
  Feiertage und Brückentage des Schuljahres, lange Wochenenden und was in den nächsten Tagen entfällt.
  Ferien und Feiertage kommen aus WebUntis und – wo die Schule nichts eingetragen hat – von
  [openholidaysapi.org](https://www.openholidaysapi.org) für dein Bundesland.
- **Fehlzeiten-Statistik** – Fehlstunden, Quote pro Fach, offene Entschuldigungen, Verspätungen, Verlauf
  pro Woche, Wochentag und Stunde, dazu Entfall- und Foyer-Statistik.
- **Anmeldung per QR-Code** – scannen oder aus einem Bild (Screenshot) laden. Kein Passwort nötig.
- **Widget** – aktuelle bzw. nächste Stunde, was heute frei ist, und die Tage bis zu den Ferien.
- **Updates in der App** – Einstellungen → „Nach Updates suchen“ lädt neue Versionen von GitHub.
- **Material You** – Material 3 Expressive, Farben passend zu deinem Hintergrundbild, hell und dunkel.

## Installation

1. Lade die neueste `GreatUntis-*.apk` unter [Releases](../../releases) herunter und installiere sie
   (Installation aus unbekannten Quellen erlauben).
   Automatische Updates: in [Obtainium](https://github.com/ImranR98/Obtainium) diese Repo-URL hinzufügen.
2. In WebUntis (Browser): **Profil → Freigaben → Zugriff über Untis Mobile → Anzeigen**. Dort steht ein
   QR-Code.
3. In GreatUntis **QR-Code scannen** (oder einen Screenshot davon mit **QR-Code aus Bild wählen** öffnen).
4. Benachrichtigungen erlauben und unter *Einstellungen → Akku-Optimierung deaktivieren* tippen, damit
   die minütliche Prüfung zuverlässig läuft.

Während der Schulzeit zeigt die App eine dauerhafte, stille Benachrichtigung („Überwachung aktiv“) –
Android verlangt das, damit eine App jede Minute prüfen darf.

## Datenschutz

- Deine Zugangsdaten bleiben **nur auf deinem Handy** (verschlüsselt im Android-Keystore).
- Die App spricht mit dem WebUntis-Server deiner Schule, mit openholidaysapi.org (nur das Bundesland,
  z. B. „NW“, für Ferien und Feiertage) und – beim Suchen nach Updates – mit GitHub. Kein eigener Server,
  keine Werbung, kein Tracking.
- Die Prüfung liest nur deinen eigenen Stundenplan und deine eigenen Fehlzeiten.

## Hinweise

- Inoffizielles Projekt, nicht mit Untis GmbH verbunden. Es nutzt dieselben Schnittstellen wie die
  WebUntis-Weboberfläche und die Untis-Mobile-App.
- Was sichtbar ist (z. B. wie weit im Voraus der Stundenplan freigegeben ist, ob Fehlzeiten angezeigt
  werden), legt deine Schule fest.

## Entwicklung

- Kotlin, Jetpack Compose (Material 3 Expressive, material3 1.5), Glance, OkHttp, kotlinx.serialization,
  WorkManager. Mindestens Android 8.0; gebaut gegen SDK 37.
- Bauen: `./gradlew assembleDebug`, Tests: `./gradlew testDebugUnitTest`.
- `tools/untis_probe.py` – Python-Referenzclient (Login, Abruf, Statistik-Referenz).
  `tools/mock_untis.py` – Mock-Server für End-to-End-Tests des Änderungs-Alarms.
  `tools/shots.sh` – Screenshot-Matrix aller Ansichten (Emulator, Demo-Daten, hell/dunkel, Schriftgrößen).
- Testdaten unter `app/src/test/resources/fixtures/` sind synthetisch (Schule, Räume, Kurse, Fehlzeiten;
  `tools/anonymize_fixtures.py`).

Lizenz: GPL-3.0
