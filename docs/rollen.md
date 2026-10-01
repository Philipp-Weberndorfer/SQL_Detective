# Rollen und Arbeitsteilung

## Scrum-Rollen

Jira kennt diese Rollen nicht als Feld – sie werden hier festgehalten.

| Rolle | Person                            | Aufgabe |
|---|-----------------------------------|---|
| Product Owner | Philipp Weberndorfer              | pflegt das Backlog, entscheidet Prioritäten, ist Ansprechpartner der Betreuungsperson |
| Scrum Master | Laurenz Anzengruber               | Termine, Board sauber halten, Hindernisse beseitigen; hat Jira-Administratorrechte im Projekt |
| Entwicklung | Anzengruber, Weberndorfer, Wieser | alle drei |

Product Owner und Scrum Master sollten nicht dieselbe Person sein.

## Fachliche Schwerpunkte

Die Aufteilung dient der Effizienz, nicht der Abschottung. **Reviews laufen über die Grenzen hinweg** –
bei der Präsentation muss jeder zu jedem Teil Auskunft geben können.

| Person | Schwerpunkt |
|---|---|
| Anzengruber | Frontend: SQL-Editor, Database Explorer, Dashboard, Evidence Board |
| Weberndorfer | Backend: API, Spiellogik, Missionsprüfung, sichere SQL-Ausführung |
| Wieser | Infrastruktur und KI: Docker, CI, Tests, ab Sprint 3 KI-Detektiv und MCP-Server |

In Sprint 1 gibt es noch keine KI-Arbeit, deshalb übernimmt Wieser dort Projektorganisation,
Docker-Grundgerüst und die Abfragelimits.

## Entscheidungswege

- **Technische Entscheidungen** trifft das Team gemeinsam und hält sie in
  `docs/architektur/entscheidungen.md` fest – mit Begründung, nicht nur mit Ergebnis.
- **Umfangsentscheidungen** (was fällt raus, wenn die Zeit nicht reicht) trifft der Product Owner nach
  Rücksprache mit der Betreuungsperson.
- **Schätzungen** macht das Team gemeinsam im Planning. Eine Schätzung, hinter der nicht alle stehen,
  macht die Velocity wertlos.
