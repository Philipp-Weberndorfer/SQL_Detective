# Definition of Done und Definition of Ready

## Definition of Done

Eine Story ist erst dann **fertig**, wenn alle Punkte erfüllt sind. Keine Ausnahmen, auch nicht kurz vor
einer Präsentation.

- [ ] Alle Akzeptanzkriterien der Story sind erfüllt
- [ ] Code ist über einen Pull Request von **einer zweiten Person** reviewt
- [ ] Automatisierte Tests sind vorhanden und laufen grün
- [ ] Die Anwendung läuft mit `docker compose up` ohne manuelle Zusatzschritte
- [ ] Keine Geheimnisse im Repository (Passwörter, API-Schlüssel nur in der lokalen `.env`)
- [ ] Dokumentation aktualisiert, wenn sich Start, Konfiguration oder Architektur geändert haben
- [ ] Auf `main` gemerged, Branch gelöscht
- [ ] Jira-Vorgang auf **Done** gesetzt

## Definition of Ready

Eine Story darf erst in einen Sprint gezogen werden, wenn sie:

- [ ] im Format „Als … möchte ich … damit …" formuliert ist
- [ ] prüfbare Akzeptanzkriterien hat
- [ ] geschätzt ist (Story Points)
- [ ] keine offenen Abhängigkeiten hat, die im Sprint nicht auflösbar sind
- [ ] auf eine oder mehrere F-Nummern aus `docs/konzept/SQL_Detective.pdf` verweist

## Schätzung

Story Points in Fibonacci: **1, 2, 3, 5, 8, 13**. Alles über 13 wird aufgeteilt.

Geschätzt wird gemeinsam im Planning, nicht einzeln vorab. Die Velocity ergibt sich aus den tatsächlich
abgeschlossenen Punkten je Sprint und wird erst nach Sprint 1 belastbar.

## Branches und Commits

```
feature/SCRUM-23-kurzbeschreibung
```

Commit-Nachrichten beginnen mit der Jira-Nummer:

```
SCRUM-23: Docker-Grundgeruest mit beiden Datenbanken
```

Dadurch verknüpft Jira Commits und Pull Requests automatisch mit dem Vorgang – das ist die
Nachvollziehbarkeit, die bei der Begutachtung angesehen wird.
