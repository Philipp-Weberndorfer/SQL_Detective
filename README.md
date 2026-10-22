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
| Frontend | http://localhost:5173 – Konto anlegen, dann landet man im SQL-Editor |
| Backend API | http://localhost:8080 |
| API-Dokumentation | http://localhost:8080/swagger-ui.html |
| Anwendungsdatenbank | `localhost:5432` |
| Spieldatenbank | `localhost:5433` |

Alle Werte (Ports, Passwörter, Zeitlimit, Zeilenlimit) stehen in `.env`, Vorlage ist `.env.example`.
Der erste Start dauert einige Minuten, weil Maven- und npm-Abhängigkeiten geladen werden.

**Frontend ohne Docker entwickeln.** Datenbanken und Backend per `docker compose up app-db game-db backend`
starten, dann `cd frontend && npm install && npm run dev`. Vite leitet `/api` an `localhost:8080` weiter.

**Datenbank zurücksetzen.** Die Init-Skripte unter `db/` laufen nur, wenn das Volume leer ist. Nach einer
Schemaänderung passiert beim normalen Neustart also nichts – dann so zurücksetzen:

```bash
docker compose down -v && docker compose up
```

## Tests

```bash
mvn test                      # aus der Projektwurzel, testet alle Java-Module
cd frontend && npm test       # Frontend-Tests (Vitest)
```

Die Backend-Tests starten über Testcontainers eigene PostgreSQL-Container mit den Skripten aus `db/`.
**Docker muss dafür laufen.** Ohne lokales JDK/Maven geht es auch komplett im Container:

```bash
docker run --rm -v "$PWD:/src" -w /src -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal maven:3.9-eclipse-temurin-21 mvn -B test
```

## API

Vollständig und ausprobierbar unter http://localhost:8080/swagger-ui.html.

| Methode | Pfad | Anmeldung | Zweck |
|---|---|---|---|
| GET | `/api/health` | nein | Status und Erreichbarkeit beider Datenbanken |
| POST | `/api/auth/register` | nein | Konto anlegen, Body `{"username", "email", "password"}` |
| POST | `/api/auth/login` | nein | Anmelden, erzeugt eine Sitzung (Cookie) |
| POST | `/api/auth/logout` | ja | Sitzung beenden |
| GET | `/api/auth/me` | ja | angemeldeter Benutzer |
| POST | `/api/queries` | ja | Spielerabfrage ausführen, Body `{"sql": "SELECT ..."}` |

Fehler kommen immer als `{"message": "...", "type": "..."}` zurück. `type` ist z. B. `VALIDATION`
(abgewiesen), `SQL` (PostgreSQL-Fehler), `TIMEOUT`, `UNAUTHORIZED` oder `CONFLICT`.

Schreibende Aufrufe brauchen das CSRF-Token: Das Backend setzt das Cookie `XSRF-TOKEN`, der Wert muss im
Header `X-XSRF-TOKEN` mitgeschickt werden. Das Frontend erledigt das automatisch.

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
