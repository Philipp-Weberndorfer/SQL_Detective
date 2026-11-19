# Sprint 2 – Review

**Zeitraum:** 22.10. – 19.11.2026 (4 Wochen)
**Ziel:** Fall #001 ist ohne KI vollständig spielbar.

## Ergebnis

| Vorgang | Titel | SP | Beauftragt ¹ | Status ² |
|---|---|---|---|---|
| SCRUM-44 | Fall als Datenpaket definieren | 13 | Wieser | umgesetzt |
| SCRUM-59 | Mission automatisch prüfen | 13 | Weberndorfer | umgesetzt |
| SCRUM-98 | Fall 001 Der verschwundene Laptop | 13 | Anzengruber | umgesetzt |
| SCRUM-60 | Fall abschließen mit Beweisführung | 8 | Weberndorfer | umgesetzt |
| SCRUM-33 | Spielfortschritt dauerhaft speichern | 5 | Weberndorfer | umgesetzt |
| SCRUM-96 | Lösbarkeitstests je Fall | 5 | Wieser | umgesetzt |
| SCRUM-55 | Hinweis als Beweis sichern | 5 | Wieser | umgesetzt |
| SCRUM-56 | Evidence Board | 5 | Wieser | umgesetzt |
| SCRUM-46 | Tabellenschema anzeigen | 5 | Anzengruber | umgesetzt |
| SCRUM-39 | Fallübersicht anzeigen | 3 | Anzengruber | umgesetzt |
| SCRUM-42 | Case Briefing lesen | 3 | Anzengruber | umgesetzt |
| SCRUM-43 | Ermittlungsziel und Teilziele sehen | 3 | Anzengruber | umgesetzt |
| SCRUM-45 | Tabellenübersicht im Explorer | 3 | Anzengruber | umgesetzt |
| SCRUM-40 | Fallstatus darstellen | 2 | Weberndorfer | umgesetzt |
| SCRUM-58 | Aktuelle Mission anzeigen | 2 | Anzengruber | umgesetzt |

**Commitment:** 88 SP
**Abgeschlossen:** 88 SP, sobald der Sprint-Branch auf `main` ist ²
**Velocity Sprint 2:** 88 SP in 4 Wochen (Sprint 1: 31 SP in 3 Wochen)

¹ Zuordnung aus der Sprint-Planung übernommen – vor dem Review mit Jira abgleichen.

² Alle 15 Stories sind auf `feature/SCRUM-sprint2` umgesetzt und gepusht. Zwei Punkte der Definition of Done
fehlen noch: **„Auf `main` eingespielt"** und **„Jira-Vorgang auf Done"**. Erst nach dem Merge gelten die
Stories als fertig.

**Zusätzlich zum Plan:**

- Zwei Nachbesserungen zu SCRUM-45 nach dem Durchklicken in 1366 px: Tabellennamen brechen im Explorer nicht
  mehr mitten im Wort um, lange Abfragen brechen im Editor um.
- Missionen haben ein optionales Feld `output` („Gesucht: Vor- und Nachname …"). Es sagt dem Spieler, welche
  Spalten erwartet werden. Ohne diese Angabe kann eine inhaltlich richtige Antwort an anderen Spaltennamen
  scheitern, weil `ResultMatcher` über die Spaltennamen zuordnet. Der Commit `64d29f5` hat keine Jira-Nummer
  und sollte in Jira einer Story zugeordnet werden.

## Sprint-Ziel erreicht?

**Ja.** Fall 001 lässt sich ohne KI von Anfang bis Ende spielen: Fallübersicht, Briefing, Explorer mit
Tabellenschema, fünf Missionen mit automatischer Prüfung, Beweise sichern, Evidence Board und Abschluss mit
Beweisführung. Der Fortschritt bleibt über Logout und Neustart erhalten.

Am 19.11. auf `feature/SCRUM-sprint2` geprüft:

- Backend: 192 Tests grün (Sprint 1: 89). Nur `SqlValidatorTest` und `ResultMatcherTest` (58 Tests) laufen
  ohne Datenbank, alle anderen Testklassen gegen echte PostgreSQL-Container (Testcontainers)
- Frontend: 31 Tests grün, Produktions-Build fehlerfrei (Vite warnt, dass das Bundle größer als 500 kB ist)
- `CaseSolvabilityTest` spielt Fall 001 automatisch durch: Referenzlösungen, je Mission zwei weitere richtige
  und eine falsche Abfrage, alle Missionen der Reihe nach, Beweise sichern, Schlussfolgerung, Status SOLVED
- `CaseSecretsTest` stellt sicher, dass kein Fall-Endpunkt Lösungen, Testabfragen, die richtige Antwort, die
  geforderten Beweise oder Texte offener Beweise ausliefert

Noch vor dem Review zu prüfen, wie in Sprint 1:

- [ ] Frisch geklonter Stand (nach dem Merge: `main`), `cp .env.example .env` und `docker compose up` ohne
      Zusatzschritte
- [ ] Oberfläche in 1366 × 768 durchgeklickt, kein horizontales Scrollen (Explorer, Editor, Seitenleiste)
- [ ] Fortschritt nach `docker compose down` und `up` (ohne `-v`) unverändert

## Demo

Ablauf, Stack vorher mit `docker compose up -d` starten. Die Lösungen der Missionen stehen in
`cases/case-001/case.json` (Feld `solution`).

1. http://localhost:5173 öffnen, Konto anlegen – landet in der **Fallübersicht**: Fall #001 mit Sternen,
   SQL-Schwerpunkt und Status „Verfügbar"
2. Fall öffnen – zuerst das **Briefing**, „Ermittlung starten" führt in den Arbeitsbereich
3. **Explorer** links: alle Tabellen des Falls mit Zeilenanzahl. Klick auf `devices` zeigt den Aufbau mit
   Datentypen, PK und NN; Klick auf `assigned_to → persons.id` springt zu `persons`, der SQL-Text bleibt
   stehen
4. Über dem Editor „Mission 1 von 5" mit „Gesucht: …", rechts die Seitenleiste mit Ermittlungsziel,
   Missionsliste und Evidence Board („0 von … Beweisen gesichert", offene Beweise nur mit „?")
5. Eine falsche Abfrage ausführen – nur „Die Mission ist noch offen.", keine erwarteten Werte
6. Die Lösung von Mission 1 anders formulieren als die Referenzlösung, z. B. mit `SELECT *` – trotzdem gelöst:
   Erfolgsmeldung, Beweisstück „Als Beweis sichern", das Board aktualisiert sich ohne Neuladen
7. `SELECT * FROM case_002.persons` – abgewiesen: „Du kannst nur die Tabellen des aktuellen Falls abfragen."
8. Abmelden, wieder anmelden, Fall öffnen – direkt im Editor bei Mission 2 (kein Briefing mehr)
9. Missionen 2 bis 5 lösen, Beweise sichern
10. „Fall abschließen": erst eine falsche Antwort, dann die richtige mit zu wenigen Beweisen – beide Male
    dieselbe neutrale Meldung, die nichts verrät
11. Richtige Antwort mit den geforderten Beweisen auf dem Board ankreuzen – „Fall gelöst!" mit Bilanz, in der
    Fallübersicht steht der Fall auf „Gelöst"
12. `CaseSolvabilityTest` in der IDE zeigen: je Fall und Mission ein eigener Test, ein neuer Fall-Ordner wird
    ohne Codeänderung mitgetestet
13. Neue Endpunkte in der API-Dokumentation unter http://localhost:8080/swagger-ui.html

**Was lief nicht wie geplant:** *(nach dem Review ergänzen)*

## Architektur

- **AE-10 · Ein Schema pro Fall, geladen beim Start** (`docs/architektur/entscheidungen.md`): Das Backend lädt
  bei jedem Start alle Fälle aus `cases/` neu, jeden in ein eigenes Schema (`case-001` → `case_001`).
  Spielerabfragen laufen mit `SET LOCAL search_path` auf das Schema des Falls. Den Eigentümer der
  Spieldatenbank nutzt nur der `CaseLoader` beim Start; Spieler-SQL läuft weiter nur über `game_readonly`.
- Fall 001 liegt jetzt unter `cases/case-001/`, die Init-Skripte für Schema und Seed in `db/game/` sind
  entfallen. `docker-compose.yml` hängt `cases/` schreibgeschützt ins Backend ein – ein neuer Fall braucht
  nur einen Neustart des Backends, keinen neuen Build.
- Das Fallformat (`docs/architektur/fall-format.md`) hat die Felder `testQueries`, `maxEvidence` und `output`
  dazubekommen.
- Alle API-Tests erben von `ApiTestBase` und teilen sich einen Spring-Kontext. Mit einem Kontext je Testklasse
  reichte `max_connections` von PostgreSQL nicht mehr.

## Nicht abgeschlossen

Keine Story aus dem Commitment wandert in Sprint 3.

Empfohlen, aber nicht im Sprint und nicht begonnen:

- **SCRUM-80 Query Logging** (5 SP) – muss vor der KI-Anbindung fertig sein, weil der KI-Detektiv über MCP
  die bisherigen Abfragen des Spielers liest. Erste Story in Sprint 3.

Bekannt und bewusst verschoben, Stories liegen im Backlog:

- Funktionen, die SQL aus einer Zeichenkette ausführen (z. B. `query_to_xml`), erreichen noch Tabellen anderer
  Fälle. Die Trennung der Fälle schützt vor Spoilern, sie ist keine Sicherheitsgrenze (AE-10) → SCRUM-51
- Regeln, wann ein Fall freigeschaltet wird; der Zustand „Gesperrt" wird schon dargestellt → SCRUM-41
- Eigene Beziehungsansicht der Tabellen → SCRUM-47
- Beweise miteinander verknüpfen → SCRUM-57
- Ausführliche Bilanz nach dem Fallabschluss → SCRUM-63

Beim Durchsehen aufgefallen, noch ohne Story:

- Meldungen aus dem Backend, die der Spieler sieht, verwenden Ersatzschreibung statt Umlaute, z. B.
  „Dieses Beweisstueck hast du schon gesichert.", „Deine Beweisfuehrung ueberzeugt noch nicht." oder
  „Fall geloest!" (`ConclusionService`, `EvidenceService`, teils schon `SqlValidator` aus Sprint 1). Die
  Fall-Texte selbst sind korrekt.
