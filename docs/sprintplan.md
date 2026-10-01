# Sprintplan

## Termine

| Sprint | Zeitraum | Dauer | Anmerkung |
|---|---|---|---|
| Sprint 0 | 24.09. – 01.10.2026 | 1 Woche | Vorbereitung, Pitch |
| Sprint 1 | 01.10. – 22.10.2026 | 3 Wochen | |
| Sprint 2 | 22.10. – 19.11.2026 | 4 Wochen | |
| Sprint 3 | 19.11. – 10.12.2026 | 3 Wochen | |
| Sprint 4 | 10.12.2026 – 14.01.2027 | 5 Wochen | Weihnachtsferien → effektiv ~3 Wochen |
| **Präsentation Wintersemester** | 14.01. / 21.01.2027 | | |
| Sprint 5 | 28.01. – 25.02.2027 | 4 Wochen | Semesterferien → effektiv ~3 Wochen |
| Sprint 6 | 25.02. – 18.03.2027 | 3 Wochen | **Feature Freeze am 18.03.** |
| Sprint 7 | 18.03. – 08.04.2027 | 3 Wochen | Osterferien (Ostersonntag 28.03.) → effektiv ~1 Woche |
| **Schlusspräsentation** | 08.04. / 15.04. / 22.04.2027 | | |

## Kapazität

Arbeitszeit ist **Donnerstagnachmittag, 4 Schulstunden**.

| | |
|---|---|
| 4 Schulstunden à 50 min | 3 h 20 min pro Person und Woche |
| 3 Personen | **10 Personenstunden pro Donnerstag** |
| 20 effektive Wochen (ohne Ferien) | **≈ 200 Personenstunden reine Schulzeit** |

Der MUST-Umfang des Konzepts liegt realistisch deutlich darüber – allein die sechs Pflichtfälle kosten je
15 bis 20 Stunden Inhaltsarbeit. Deshalb rechnen wir zusätzlich mit **2 bis 3 Stunden Heimarbeit pro Person
und Woche**. Reicht das nicht, wird der Umfang mit der Betreuungsperson nachverhandelt, nicht still
überzogen.

Für die Umrechnung gilt als Startannahme **1 Story Point ≈ 1,7 Stunden**. Diese Zahl wird nach Sprint 1
durch die gemessene Velocity ersetzt.

## Zwei Regeln, die aus dem Kalender folgen

1. **Sprint 7 ist kein Entwicklungssprint.** Er liegt fast vollständig in den Osterferien. Feature Freeze ist
   das Ende von Sprint 6, der **18.03.2027**. Sprint 7 dient Stabilisierung, Dokumentation und der
   Schlusspräsentation.
2. **Sprint 4 endet am Tag der Wintersemesterpräsentation.** Interner Feature Freeze deshalb schon am
   **07.01.2027**, die letzte Woche nur Bugfixing und Demo-Proben.

## Sprint-Ziele

| Sprint | Ziel in einem Satz |
|---|---|
| 1 | Ein angemeldeter Benutzer kann eine SELECT-Abfrage ausführen und das Ergebnis als Tabelle sehen – schreibende Befehle werden zuverlässig abgewiesen. |
| 2 | Fall #001 ist ohne KI vollständig spielbar. |
| 3 | Der KI-Detektiv hilft über den MCP-Server, ohne die Lösung zu verraten. |
| 4 | Wir können das Projekt live vorführen. |
| 5 | Der Lernpfad reicht bis CTE, das System kennt den Wissensstand des Spielers. |
| 6 | Lehrpersonen können Klassen führen und eigene Fälle anlegen. |
| 7 | Alles ist stabil, dokumentiert und vorführbar. |

Der Aufbau folgt dem Prinzip **Walking Skeleton**: Sprint 1 baut das dünnste lauffähige Gerüst durch alle
Schichten. Die beiden größten Risiken – sichere SQL-Sandbox und KI/MCP – liegen bewusst früh, in Sprint 1
und Sprint 3.

## Sprint 1 im Detail

**Ziel:** Ein angemeldeter Benutzer kann eine SELECT-Abfrage ausführen und das Ergebnis als Tabelle sehen –
schreibende Befehle werden zuverlässig abgewiesen.

**Kapazität:** 4 Donnerstage abzüglich Planning und Review ≈ 31 Personenstunden Schulzeit, plus ca. 18 Stunden
Heimarbeit ≈ **49 Personenstunden**.

| Vorgang | Titel | SP | Beauftragt |
|---|---|---|---|
| SCRUM-23 | Projektorganisation aufsetzen | 3 | Wieser |
| SCRUM-24 | Entwicklungsumgebung und Docker-Grundgerüst | 5 | Wieser |
| SCRUM-25 | Getrennte Datenbanken mit Read-only-Benutzer | 3 | Weberndorfer |
| SCRUM-26 | Registrierung und Login | 5 | Anzengruber |
| SCRUM-27 | SQL-Abfrage über die API ausführen | 5 | Weberndorfer |
| SCRUM-28 | Schreibende Statements zuverlässig abweisen | 3 | Weberndorfer |
| SCRUM-29 | SQL-Editor mit Ergebnistabelle | 5 | Anzengruber |
| SCRUM-30 | Query-Timeout und Zeilenlimit *(Stretch)* | 2 | Wieser |

**Commitment:** 29 Story Points. SCRUM-30 ist Puffer und fällt als Erstes weg, wenn es eng wird.

**Verteilung:** Anzengruber 10 · Weberndorfer 11 · Wieser 10 (davon 2 Stretch)

**Abhängigkeiten:** SCRUM-24 blockiert alles andere, also zuerst. SCRUM-27 braucht SCRUM-25.
SCRUM-29 braucht SCRUM-27. SCRUM-26 läuft unabhängig daneben.

## Ablauf je Sprint

| Termin | Dauer | Inhalt |
|---|---|---|
| Sprint Planning | erster Donnerstag, ~45 min | Ziel formulieren, Stories ziehen, gemeinsam schätzen, Verantwortliche festlegen |
| Weekly | jeden Donnerstag, 15 min | erledigt / als Nächstes / blockiert |
| Sprint Review | letzter Donnerstag | laufende Software zeigen, keine Folien |
| Retrospektive | letzter Donnerstag | was lief gut, was ändern wir konkret – Ergebnis nach `docs/sprints/` |
