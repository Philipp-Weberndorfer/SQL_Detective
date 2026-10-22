# Sprint 1 – Review

**Zeitraum:** 01.10. – 22.10.2026
**Ziel:** Ein angemeldeter Benutzer kann eine SELECT-Abfrage ausführen und das Ergebnis als Tabelle sehen –
schreibende Befehle werden zuverlässig abgewiesen.

## Ergebnis

| Vorgang | Titel | SP | Beauftragt | Status |
|---|---|---|---|---|
| SCRUM-23 | Projektorganisation aufsetzen | 3 | Wieser | Done ¹ |
| SCRUM-24 | Entwicklungsumgebung und Docker-Grundgerüst | 5 | Wieser | Done |
| SCRUM-25 | Getrennte Datenbanken mit Read-only-Benutzer | 3 | Weberndorfer | Done |
| SCRUM-26 | Registrierung und Login | 5 | Anzengruber | Done |
| SCRUM-27 | SQL-Abfrage über die API ausführen | 5 | Weberndorfer | Done |
| SCRUM-28 | Schreibende Statements zuverlässig abweisen | 3 | Weberndorfer | Done |
| SCRUM-29 | SQL-Editor mit Ergebnistabelle | 5 | Anzengruber | Done |
| SCRUM-30 | Query-Timeout und Zeilenlimit *(Stretch)* | 2 | Wieser | Done |

**Commitment:** 29 SP
**Abgeschlossen:** 31 SP (29 SP Commitment + 2 SP Stretch)
**Velocity Sprint 1:** 31 SP

¹ Zwei Akzeptanzkriterien weichen bewusst ab und werden in Jira an die Entscheidung angepasst: Das
Jira-Projekt heißt **SCRUM** statt SQLD, und die Pull-Request-Pflicht hat das Team gestrichen (Commit
`b6bea21`). Die Bestätigung der Definition of Done durch alle drei wird in diesem Review festgehalten.

## Sprint-Ziel erreicht?

**Ja.** Ein angemeldeter Benutzer führt im Browser SELECT-Abfragen aus und sieht das Ergebnis als Tabelle.
Schreibende Befehle scheitern an drei unabhängigen Schichten: Validierung in der Anwendung,
Datenbankbenutzer `game_readonly` mit reinem Leserecht, READ ONLY-Transaktion mit Rollback.

Am 22.10. an einem frisch geklonten `main` geprüft:

- `cp .env.example .env` und `docker compose up` starten alle vier Container ohne Zusatzschritte
- Backend: 89 Tests grün, davon 49 gegen echte PostgreSQL-Container (Testcontainers)
- Frontend: 12 Tests grün, Produktions-Build fehlerfrei
- Oberfläche in 1366 × 768 durchgeklickt: kein horizontales Scrollen, auch nicht bei breiten Tabellen
- Konten bleiben nach einem Neustart erhalten (Volumes)

## Demo

Ablauf, Stack vorher mit `docker compose up -d` starten (der erste Build dauert einige Minuten):

1. http://localhost:5173/editor ohne Anmeldung öffnen – leitet auf den Login um
2. Konto anlegen – landet direkt im SQL-Editor
3. `SELECT * FROM persons;` per Button ausführen – Tabelle mit Spaltenüberschriften
4. Breite Abfrage per **Strg + Enter**:
   `SELECT * FROM access_logs a JOIN persons p ON p.id = a.person_id JOIN rooms r ON r.id = a.room_id`
   – die Tabelle scrollt in ihrem Rahmen, nicht die Seite
5. `DROP TABLE persons;` und `SELECT 1; DROP TABLE persons;` – beide verständlich abgewiesen
6. `SELECT nope FROM persons` – PostgreSQL-Meldung lesbar, kein Stacktrace
7. `SELECT pg_sleep(10)` – Ladezustand sichtbar, Abbruch nach 5 Sekunden mit Meldung
8. `SELECT g FROM generate_series(1, 2000) g` – 500 Zeilen und Hinweis auf die Begrenzung
9. API-Dokumentation unter http://localhost:8080/swagger-ui.html
10. `ReadonlyUserTest` zeigen: Schreibbefehle scheitern direkt am Datenbankbenutzer, ohne Validierung
11. Abmelden – der Editor ist wieder gesperrt

**Was lief nicht wie geplant:** *(nach dem Review ergänzen)*

## Nicht abgeschlossen

Keine Story wandert in Sprint 2. Alle acht Vorgänge sind fertig, inklusive Stretch-Ziel SCRUM-30.

Bewusst nicht Teil von Sprint 1, Stories liegen bereits im Backlog:

- Systemfunktionen und Systemtabellen (z. B. `pg_advisory_lock`, `pg_catalog`) sind für Spielerabfragen noch
  erreichbar → Story „Zugriff auf Systemtabellen einschränken" (F115)
- Keine Begrenzung der Abfragen pro Benutzer → Story „Rate Limiting für Abfragen" (F116)
- Ausgeführte Abfragen werden noch nicht protokolliert → Story „Query Logging" (F120–F122)
- Sitzungen liegen im Speicher des Backends: Nach einem Neustart des Backends muss man sich neu anmelden
  (AE-08, für das Projekt ausreichend)
