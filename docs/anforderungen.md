# Anforderungen und Rückverfolgbarkeit

Das Pflichtenheft [`konzept/SQL_Detective.pdf`](konzept/SQL_Detective.pdf) nummeriert jede Anforderung von
**F01 bis F137** durch. Jede Jira-Story verweist in ihrer Beschreibung auf die Anforderungen, die sie
umsetzt.

**Nachschlagen:** Tipp die Nummer in die Jira-Suche ein (z. B. `F50`), und du bekommst alle Stories, die
diese Anforderung umsetzen. Umgekehrt steht bei jeder Story, woher sie kommt.

## Epics und abgedeckte Anforderungen

| Epic | Inhalt | Anforderungen |
|---|---|---|
| E01 | Benutzer und Profil | F01–F05 |
| E02 | Dashboard und Spielstand | F06–F08, F105–F107 |
| E03 | Fallübersicht und Fallablauf | F09–F14 |
| E04 | Ermittlungsdatenbank und Fall-Content | F15–F19, F136 |
| E05 | Database Explorer | F20–F23 |
| E06 | SQL-Editor | F24–F30 |
| E07 | Sichere SQL-Ausführung | F31–F35, F108–F116 |
| E08 | Notizbuch und Beweise | F36–F42 |
| E09 | Missionen und Bewertung | F43–F45, F79–F81 |
| E10 | Lernpfad und Learning Mode | F46–F54 |
| E11 | KI-Detektiv und Hint-System | F55–F68 |
| E12 | MCP-Server | F69–F75, F117–F119 |
| E13 | Gamification | F82–F87 |
| E14 | Adaptive Learning und Analytics | F76–F78, F120–F123 |
| E15 | Teacher Mode und Case Editor | F88–F100 |
| E16 | Challenge- und Prüfungsmodus | F101–F104 |
| E17 | API, Deployment und Tests | F124–F137 |
| E18 | Kriminalfälle | Kapitel 41 |

## Lernpfad (F46–F51)

Diese Anforderungen werden nicht programmiert, sondern über die sechs Fälle umgesetzt:

| Fall | Schwerpunkt | Anforderung |
|---|---|---|
| #001 Der verschwundene Laptop | SELECT, WHERE | F46 |
| #002 Der manipulierte Stundenplan | JOIN | F48 |
| #003 Das verschwundene Preisgeld | GROUP BY, Aggregation | F47 |
| #004 Der geheimnisvolle Login | Subqueries | F49 |
| #005 Die Phantom-Bestellungen | CTE | F50 |
| #006 Der Insider | Window Functions | F51 |

## Priorisierung

| Priorität | Bedeutung | Anzahl Stories |
|---|---|---|
| MUST | für die Projektabnahme erforderlich | 38 |
| SHOULD | sollte umgesetzt werden | 25 |
| COULD | Erweiterung | 12 |

Die MUST-Liste des Konzepts umfasst unter anderem: Login und Fortschritt, **mindestens sechs vollständig
spielbare Fälle** (darunter einer mit CTE), SQL-Editor, sichere Read-only-Sandbox, Missionssystem mit
automatischer Ergebnisprüfung, KI-Detektiv mit mehrstufigem Hint-System, AI SQL Error Coach, **eigener
MCP-Server**, REST API, Docker und automatisierte Tests inklusive Security-Tests.

## Backlog-Import

`backlog-import.csv` ist der Stand, mit dem das Jira-Backlog angelegt wurde (18 Epics, 81 Vorgänge).
Ab dem Import ist **Jira die führende Quelle** – die Datei wird nicht mitgepflegt und dient nur der
Nachvollziehbarkeit.
