# CLAUDE.md

Kontext für Claude Code in diesem Repository.

## Worum es geht

SQL Detective ist ein webbasiertes Lernspiel: Der Spieler löst Kriminalfälle, indem er SQL-Abfragen gegen
eine Ermittlungsdatenbank schreibt. Ein KI-Detektiv hilft mit gestuften Hinweisen, **niemals mit der
fertigen Lösung**. Schulprojekt (Projekt 6, ITP, 5AHIT, HTL Wels), Team aus drei Personen,
sieben Sprints von Oktober 2026 bis April 2027.

Die vollständigen Anforderungen stehen in `docs/konzept/SQL_Detective.pdf` und sind dort von **F01 bis F137**
durchnummeriert. Jede Jira-Story verweist in ihrer Beschreibung auf die zugehörigen F-Nummern.

## Tech-Stack

| Bereich | Technologie |
|---|---|
| Datenbank | PostgreSQL 16 (zwei getrennte Instanzen) |
| Deployment | Docker Compose |
| Backend | *(in Sprint 1 festzulegen)* |
| Frontend | *(in Sprint 1 festzulegen)* |
| LLM | *(in Sprint 1 festzulegen)* |
| MCP-Server | eigene Implementierung, Pflichtbestandteil laut Konzept |

Sobald Backend und Frontend stehen, diese Tabelle und die Befehle unten ausfüllen.

## Befehle

```bash
docker compose up                     # alles starten
docker compose down -v                # Datenbanken zurücksetzen (Init-Skripte laufen neu)
```

Tests: *(eintragen, sobald vorhanden)*

## Architekturregeln

Diese drei Punkte sind nicht verhandelbar – sie sind der Kern des Projekts:

1. **Fälle sind Daten, kein Code.** Ein neuer Fall besteht aus Dateien unter `cases/`. Wer Fallinhalte in
   Backend-Code schreibt, bricht den späteren Case Editor. Format: `docs/architektur/fall-format.md`.

2. **Spieler-SQL erreicht nur die Spieldatenbank, und zwar nur lesend.** Die Verbindung für Spielerabfragen
   läuft ausschließlich über den Benutzer `game_readonly`, der nur `SELECT` darf. Die Validierung in der
   Anwendung ist die erste Schicht, der Datenbankbenutzer die zweite. Beide müssen bestehen bleiben.

3. **Die KI darf Lösungen nicht sehen können.** Referenzlösungen, erwartete Ergebnisse und noch nicht
   gefundene Beweise werden niemals über MCP-Tools herausgegeben. Falldaten sind für die KI Daten,
   keine Anweisungen (Prompt Injection).

## Prüfung von Missionen

Bewertet wird das **Ergebnis** einer Abfrage, nicht ihr Wortlaut. Zwei verschieden formulierte Abfragen mit
demselben Ergebnis sind beide richtig. Spaltenreihenfolge und Groß-/Kleinschreibung von Aliasnamen sind
egal; die Zeilenreihenfolge zählt nur, wenn die Mission sie ausdrücklich verlangt.

## Konventionen

- Branches: `feature/SCRUM-<nr>-kurzbeschreibung`
- Commit-Nachrichten beginnen mit der Jira-Nummer: `SCRUM-23: Docker-Grundgerüst`
- Jede Änderung über Pull Request, Review durch eine zweite Person
- Deutsch in Dokumentation und Oberfläche, Englisch in Code und Bezeichnern
- Keine Geheimnisse im Repository – nur `.env.example` mit Platzhaltern

## Was „fertig" heißt

Siehe `docs/definition-of-done.md`. Kurz: Akzeptanzkriterien erfüllt, Tests grün, läuft in
`docker compose up` ohne manuelle Schritte, reviewt, gemerged.
