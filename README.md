# SQL Detective

> „Learn SQL by Crime" – ein webbasiertes Lernspiel, in dem man SQL lernt, indem man Kriminalfälle löst.

Der Spieler ist Ermittler und bekommt keine Aufgabenstellung wie „Schreibe einen INNER JOIN", sondern eine
Geschichte und eine Ermittlungsdatenbank. Er muss selbst herausfinden, welche Information er braucht, in
welchen Tabellen sie steht und wie er sie verknüpft. Unterstützt wird er dabei von einem KI-Detektiv, der
über einen selbst entwickelten MCP-Server den Spielstand kennt – und bewusst Hinweise gibt statt Lösungen.

**Projekt 6 · ITP · 5AHIT · HTL Wels · Schuljahr 2026/27**

| | |
|---|---|
| Team | Anzengruber, Weberndorfer, Wieser |
| Jira-Board | https://louchovibes.atlassian.net/jira/software/projects/SCRUM/summary |
| Pflichtenheft | [`docs/konzept/SQL_Detective.pdf`](docs/konzept/SQL_Detective.pdf) (F01–F137) |
| Sprintplan | [`docs/sprintplan.md`](docs/sprintplan.md) |

---

## Schnellstart

Voraussetzung: Docker Desktop.

```bash
git clone https://github.com/Philipp-Weberndorfer/SQL_Detective.git
cd SQL_Detective
cp .env.example .env
docker compose up
```

| Dienst | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Anwendungsdatenbank | `localhost:5432` |
| Spieldatenbank | `localhost:5433` |

**Datenbank zurücksetzen.** Die Init-Skripte unter `db/` laufen nur, wenn das Volume leer ist. Nach einer
Schemaänderung passiert beim normalen Neustart also nichts – dann so zurücksetzen:

```bash
docker compose down -v && docker compose up
```

## Tests

```bash
cd backend
mvn test
```

## API

| Methode | Pfad | Zweck |
|---|---|---|
| GET | `/api/health` | Status und Erreichbarkeit beider Datenbanken |
| POST | `/api/queries` | Spielerabfrage ausführen, Body `{"sql": "SELECT ..."}` |

Beispiel (PowerShell):

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/queries -Method Post `
  -ContentType "application/json" `
  -Body '{"sql": "SELECT * FROM access_logs WHERE room_id = 3"}'
```

## Ordnerstruktur

```
.
├─ docs/           Konzept, Sprintplan, Architekturentscheidungen, Sprint-Protokolle
├─ db/             Init-Skripte: app/ = Anwendungsdatenbank, game/ = Spieldatenbank
├─ cases/          Die Kriminalfälle als Daten (Briefing, Schema, Seed, Missionen, Hints)
├─ backend/        REST API + Spiellogik + sichere SQL-Ausführung
├─ frontend/       Weboberfläche
└─ mcp-server/     MCP-Server, über den der KI-Detektiv den Spielstand liest
```

Warum `cases/` ganz oben und nicht in `backend/`: Fälle sind **Daten, kein Code**. Ein neuer Fall darf keine
Codeänderung erfordern – das ist die Grundlage für den späteren Case Editor. Siehe
[`docs/architektur/fall-format.md`](docs/architektur/fall-format.md).

## Sicherheit

Spieler geben beliebiges SQL ein. Der Schutz liegt bewusst auf mehreren Schichten:

1. **Validierung in der Anwendung** – nur lesende Statements, keine Mehrfach-Statements, keine Kommentar-Tricks
2. **Read-only-Datenbankbenutzer** – `game_readonly` besitzt ausschließlich `SELECT`
3. **Getrennte Datenbanken** – Spieler-SQL erreicht die Anwendungsdatenbank nie
4. **Limits** – Zeitlimit je Abfrage, maximal 500 Ergebniszeilen, Rate Limiting

`DROP TABLE persons;` muss zuverlässig scheitern und ist als automatisierter Test hinterlegt.

## Mitarbeiten

- Branch pro Vorgang: `feature/SCRUM-23-kurzbeschreibung`
- Die Jira-Nummer gehört in die Commit-Nachricht (`SCRUM-23: Docker-Grundgerüst`)
- Was „fertig" bedeutet, steht in [`docs/definition-of-done.md`](docs/definition-of-done.md)

Geheimnisse (Datenbankpasswörter, LLM-API-Schlüssel) gehören ausschließlich in die lokale `.env` –
niemals ins Repository.
