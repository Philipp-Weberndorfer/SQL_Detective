# Sprint 2 – Planung

**Zeitraum:** 22.10. – 19.11.2026 (4 Wochen)
**Ziel:** Fall #001 ist ohne KI vollständig spielbar.

Jira ist die führende Quelle. Diese Datei ist der Entwurf, aus dem die Beschreibungen und
Akzeptanzkriterien in die Jira-Vorgänge übernommen werden.

## Umfang

Offen sind 218 SP mit Priorität Highest (MUST). Bis zum Feature Freeze am 18.03.2027 bleiben Sprint 2 bis 6
mit rund 16 effektiven Wochen, also 13,6 SP pro Woche. Für die vier Wochen von Sprint 2 wären das 54,5 SP.
Geplant sind **86 SP**. Danach bleiben 132 SP MUST für 12 Wochen, rund 33 SP pro Sprint – etwa die
Velocity aus Sprint 1. Das lässt Luft für Weihnachten und den internen Freeze am 07.01.

| Vorgang | Titel | SP | Vorschlag |
|---|---|---|---|
| SCRUM-44 | Fall als Datenpaket definieren | 13 | Wieser |
| SCRUM-59 | Mission automatisch prüfen | 13 | Weberndorfer |
| SCRUM-98 | Fall 001 Der verschwundene Laptop | 13 | Anzengruber |
| SCRUM-60 | Fall abschließen mit Beweisführung | 8 | Weberndorfer |
| SCRUM-33 | Spielfortschritt dauerhaft speichern | 5 | Weberndorfer |
| SCRUM-96 | Lösbarkeitstests je Fall | 5 | Wieser |
| SCRUM-55 | Hinweis als Beweis sichern | 5 | Wieser |
| SCRUM-56 | Evidence Board | 5 | Wieser |
| SCRUM-39 | Fallübersicht anzeigen | 3 | Anzengruber |
| SCRUM-42 | Case Briefing lesen | 3 | Anzengruber |
| SCRUM-43 | Ermittlungsziel und Teilziele sehen | 3 | Anzengruber |
| SCRUM-45 | Tabellenübersicht im Explorer | 3 | Anzengruber |
| SCRUM-46 | Tabellenschema anzeigen | 3 | Anzengruber |
| SCRUM-40 | Fallstatus darstellen | 2 | Weberndorfer |
| SCRUM-58 | Aktuelle Mission anzeigen | 2 | Anzengruber |

**Commitment:** 86 SP · Weberndorfer 28 · Wieser 28 · Anzengruber 30

Die Zuordnung hält Zusammengehöriges zusammen: Explorer (SCRUM-45, 46) bei einer Person, Beweise
(SCRUM-55, 56) bei einer Person, Fortschritt und Status (SCRUM-33, 40) bei einer Person.

**Abhängigkeiten:** SCRUM-44 blockiert alles andere, also zuerst. SCRUM-59 braucht SCRUM-44.
SCRUM-55 braucht SCRUM-59, SCRUM-56 baut auf SCRUM-55 auf, SCRUM-60 braucht SCRUM-56.
SCRUM-46 braucht SCRUM-45. SCRUM-40 braucht SCRUM-33 und SCRUM-39.

**Fällt zuerst weg, wenn es eng wird:**
1. SCRUM-39 und SCRUM-40 (5 SP) – Fall #001 wird dann über einen direkten Link geöffnet
2. SCRUM-45 und SCRUM-46 (6 SP)

**Änderung an SCRUM-47:** SCRUM-46 übernimmt die Fremdschlüssel-Markierung, weil sie aus derselben Abfrage
und Ansicht kommt und Mission 1 genau diesen JOIN braucht. SCRUM-47 behält nur eine eigene
Beziehungsansicht und wird in Jira neu geschätzt.

**Empfehlung, nicht im Sprint:** SCRUM-80 Query Logging (5 SP) muss vor Sprint 3 fertig sein, weil der
KI-Detektiv über MCP die bisherigen Abfragen des Spielers liest (Konzept Kapitel 43, Schritt 4). Nachziehen,
sobald in Sprint 2 Kapazität frei wird, sonst als erste Story in Sprint 3.

---

## Beschreibungen und Akzeptanzkriterien

### SCRUM-44 · Fall als Datenpaket definieren · 13 SP

Als Team möchten wir einen Fall vollständig als Datenpaket unter `cases/` beschreiben, damit neue Fälle ohne
Codeänderung spielbar sind und der spätere Case Editor darauf aufbauen kann.

Akzeptanzkriterien:
- Ein Fall ist ein Ordner `cases/<id>/` mit `case.json`, `schema.sql` und `seed.sql` nach `docs/architektur/fall-format.md`
- `case.json` enthält eine Formatversion; Fälle mit unbekannter Version oder fehlenden Pflichtfeldern werden mit einer Log-Meldung abgewiesen, die Fall und Feld nennt
- Ein fehlerhafter Fall verhindert den Start nicht, alle anderen Fälle werden trotzdem geladen
- Beim Start werden alle Fälle automatisch geladen und ihre Tabellen in der Spieldatenbank angelegt, jeder Fall in einem eigenen Schema
- Das Laden ist wiederholbar: Ein Neustart erzeugt denselben Stand, ohne dass `docker compose down -v` nötig ist
- Spielerabfragen laufen gegen das Schema des geöffneten Falls; Tabellen anderer Fälle sind nicht sichtbar
- `game_readonly` hat auch auf den Fall-Schemas ausschließlich SELECT-Recht; die Sicherheitstests aus Sprint 1 bleiben grün
- Fall 001 ist aus `db/game/` nach `cases/case-001/` umgezogen
- Kein API-Endpunkt liefert `expectedResult`, `solution`, `conclusion.correct`, `requiredEvidence` oder den Text noch nicht gefundener Beweise aus; ein automatisierter Test prüft das für alle Fall-Endpunkte
- Ein zusätzlicher Fall-Ordner ist nach einem Neustart ohne Neukompilieren spielbar (Test mit einem Mini-Testfall)

Konzept: F15–F19, F136

### SCRUM-59 · Mission automatisch prüfen · 13 SP

Als Spieler möchte ich, dass meine Abfrage automatisch gegen die aktuelle Mission geprüft wird, damit ich
sofort weiß, ob ich die gesuchte Information gefunden habe.

Akzeptanzkriterien:
- Jede im Fall ausgeführte Abfrage wird automatisch gegen die offene Mission geprüft, ein eigener Prüfen-Knopf ist nicht nötig
- Geprüft wird das Ergebnis der Abfrage, nicht der Wortlaut; zwei unterschiedlich formulierte Abfragen mit gleichem Ergebnis gelten beide als richtig
- Spaltenreihenfolge und Groß-/Kleinschreibung der Spaltennamen sind egal; zusätzliche Spalten (z. B. bei `SELECT *`) sind erlaubt, solange alle erwarteten Spalten enthalten sind
- Zeilen werden als Multimenge verglichen: fehlende, zusätzliche oder doppelte Zeilen gelten als falsch; die Reihenfolge zählt nur bei `orderMatters: true`
- Bei Erfolg wird die Mission als gelöst gespeichert und die nächste freigeschaltet
- Bei falschem Ergebnis gibt es keine Fehlermeldung, sondern nur den Hinweis, dass die Mission noch offen ist – ohne erwartete Werte oder Referenzlösung zu verraten
- Jeder Versuch wird gezählt (`mission_progress.attempts`)
- Tests: mindestens zwei unterschiedliche korrekte Lösungswege, dazu falsche Spalten, fehlende Zeile, doppelte Zeile und falsche Reihenfolge bei `orderMatters`

Konzept: F44, F45

### SCRUM-98 · Fall 001 Der verschwundene Laptop · 13 SP

Als Spieler möchte ich den Fall „Der verschwundene Laptop" vollständig durchspielen, damit ich SELECT, WHERE
und erste JOINs an einer zusammenhängenden Geschichte übe.

Akzeptanzkriterien:
- Fünf Missionen nach Konzept Kapitel 41: Gerätezuordnung (SELECT, WHERE), letzte Anmeldung (ORDER BY, LIMIT), Personen nach 16:30 Uhr im Gebäude (JOIN), Zugang zum Labor (mehrere Bedingungen), WLAN-Verbindungen (mehrere JOINs)
- Jede Mission hat ein erwartetes Ergebnis, eine Referenzlösung, vier Hinweisstufen nach F58–F61 und ein Beweisstück bei Erfolg
- Finale mit Schlussfolgerungsfrage, mindestens drei plausiblen Antwortmöglichkeiten und den Beweisen, die die richtige Antwort belegen
- Realistische Datenmenge: mindestens 30 Personen, 100 Zutrittseinträge und 50 WLAN-Verbindungen über das ganze Wochenende
- Mindestens eine falsche Fährte (eine Person wirkt verdächtig, hat aber ein Alibi) und mindestens eine Zeugenaussage, die den Daten widerspricht
- Jede Mission ist allein aus den Daten beantwortbar, die Schlussfolgerung ist eindeutig
- Alle Texte in korrektem Deutsch mit Umlauten, keine Ersatzschreibung wie „Geraet"
- Liegt im Format aus SCRUM-44 vor und besteht die Lösbarkeitstests aus SCRUM-96

Konzept: Kapitel 41, F12–F19

### SCRUM-60 · Fall abschließen mit Beweisführung · 8 SP

Als Spieler möchte ich am Ende eine Schlussfolgerung wählen und mit meinen Beweisen belegen, damit der Fall
durch Ermittlungsarbeit gelöst wird und nicht durch Raten.

Akzeptanzkriterien:
- Der Abschluss wird erst freigeschaltet, wenn alle Missionen des Falls gelöst sind
- Der Spieler wählt eine der Antworten aus `case.json` und wählt auf dem Evidence Board (SCRUM-56) die gesicherten Beweise aus, die sie belegen
- Gelöst ist der Fall nur, wenn die Antwort stimmt und alle geforderten Beweise vorgelegt wurden; der richtige Name allein reicht nicht
- Geprüft wird ausschließlich im Backend; richtige Antwort und geforderte Beweise verlassen den Server nie
- Bei falscher oder unvollständig belegter Antwort kommt eine Rückmeldung, die die richtige Antwort nicht verrät; ein neuer Versuch ist möglich
- Bei Erfolg wechselt der Fall auf SOLVED und es erscheint „Fall gelöst" mit Anzahl gelöster Missionen und gefundener Beweise (die ausführliche Bilanz folgt mit SCRUM-63)
- Tests: richtige Antwort mit allen Beweisen, richtige Antwort mit fehlendem Beweis, falsche Antwort, Abschluss vor der letzten Mission

Konzept: Kapitel 41 (Finale), Kapitel 43 Schritt 6

### SCRUM-33 · Spielfortschritt dauerhaft speichern · 5 SP

Als Spieler möchte ich, dass mein Fortschritt dauerhaft gespeichert wird, damit ich nach einer Pause genau
dort weitermache, wo ich aufgehört habe.

Akzeptanzkriterien:
- Gespeichert werden je Fall der Status (LOCKED, AVAILABLE, IN_PROGRESS, SOLVED) und die aktuelle Mission, je Mission gelöst oder offen samt Anzahl Versuche, dazu die gesicherten Beweise
- Gespeichert wird sofort beim Lösen einer Mission oder Sichern eines Beweises, ohne Speichern-Knopf
- Beim ersten Öffnen eines Falls wechselt sein Status von AVAILABLE auf IN_PROGRESS
- Nach Logout und erneutem Login ist der Fortschritt unverändert
- Nach einem Neustart aller Container (`docker compose down` und `up`, ohne `-v`) ist der Fortschritt unverändert
- Jeder Spieler sieht und ändert nur seinen eigenen Fortschritt; ein Test mit zwei Benutzern weist das nach

Konzept: F05, F135

### SCRUM-96 · Lösbarkeitstests je Fall · 5 SP

Als Team wollen wir für jeden Fall automatisch prüfen, dass er lösbar ist, damit ein fehlerhafter Fall nie
bei Spielern ankommt.

Akzeptanzkriterien:
- Für jede Mission jedes Falls läuft die Referenzlösung gegen echtes PostgreSQL (Testcontainers) und liefert genau das erwartete Ergebnis
- Je Mission sind mindestens zwei unterschiedliche korrekte Abfragen und eine typische falsche Abfrage im Fallordner hinterlegt; die Prüfung aus SCRUM-59 erkennt sie richtig
- Diese Testabfragen werden wie Lösungen behandelt und nie ausgeliefert
- Durchspieltest je Fall: alle Missionen nacheinander lösen, alle geforderten Beweise sind dabei erreichbar, die richtige Schlussfolgerung wird akzeptiert
- Ein neuer Fall wird automatisch mitgetestet, ohne dass Testcode geändert werden muss
- Ein kaputter Fall lässt `mvn test` fehlschlagen; die Meldung nennt Fall und Mission

Konzept: F128, F129

### SCRUM-55 · Hinweis als Beweis sichern · 5 SP

Als Spieler möchte ich eine gefundene Information als Beweisstück sichern, damit ich sie am Ende für meine
Schlussfolgerung verwenden kann.

Akzeptanzkriterien:
- Nach einer gelösten Mission wird das zugehörige Beweisstück angezeigt und lässt sich mit einem Klick sichern
- Gesicherte Beweise werden dauerhaft gespeichert und erscheinen sofort auf dem Evidence Board (SCRUM-56); doppeltes Sichern ist nicht möglich
- Den Text noch nicht gefundener Beweise schickt das Backend nie an das Frontend
- Ein Beweis lässt sich nur für eine eigene, gelöste Mission sichern; ein direkter API-Aufruf für eine ungelöste Mission wird abgewiesen
- Tests: sichern, doppelt sichern, Mission noch nicht gelöst, Beweis eines anderen Spielers

Konzept: F39, F40

### SCRUM-56 · Evidence Board · 5 SP

Als Spieler möchte ich alle gefundenen und noch offenen Beweise auf einer Tafel sehen, damit ich weiß, was
ich schon belegen kann und was mir noch fehlt.

Akzeptanzkriterien:
- Das Evidence Board ist während des Falls neben dem Editor erreichbar; der eingegebene SQL-Text bleibt erhalten
- Gesicherte Beweise stehen abgehakt mit Titel und Text auf dem Board, noch offene nur als Platzhalter mit Titel und Fragezeichen (wie Konzept F41)
- Der Stand ist als Zahl sichtbar, z. B. „3 von 6 Beweisen gefunden"
- Ein neu gesicherter Beweis erscheint ohne Neuladen der Seite
- Beim Fallabschluss (SCRUM-60) wählt der Spieler die Beweise direkt auf dem Board aus
- Auf einem Laptopbildschirm (1366 × 768) scrollt die Seite nicht horizontal
- API-Test, dass offene Beweise ohne Text ausgeliefert werden, und Frontend-Test für gefundene und offene Beweise

Konzept: F41 – das Verknüpfen von Beweisen (F42) bleibt bei SCRUM-57

### SCRUM-39 · Fallübersicht anzeigen · 3 SP

Als Spieler möchte ich alle Fälle mit Titel, Schwierigkeit und Schwerpunkt sehen, damit ich weiß, welche
Fälle es gibt, und einen davon öffnen kann.

Akzeptanzkriterien:
- Nach dem Login ist die Fallübersicht über die Navigation erreichbar
- Jeder Fall erscheint mit Nummer, Titel, Schwierigkeit in Sternen und SQL-Schwerpunkt (wie die Tabelle in Konzept F09), sortiert nach Fallnummer
- Die Liste kommt aus den geladenen Fällen (SCRUM-44); ein neuer Fall-Ordner erscheint nach einem Neustart ohne Codeänderung
- Ein Klick auf einen verfügbaren Fall öffnet ihn: beim ersten Mal mit dem Briefing, danach direkt im Editor (SCRUM-42)
- API- und Frontend-Test, mit dem Mini-Testfall aus SCRUM-44 als zweitem Fall

Konzept: F09

### SCRUM-42 · Case Briefing lesen · 3 SP

Als Spieler möchte ich zu Beginn eines Falls die Geschichte und die Ausgangslage lesen, damit ich weiß,
worum es geht und wonach ich suche.

Akzeptanzkriterien:
- Beim ersten Öffnen eines Falls aus der Fallübersicht erscheint zuerst das Briefing mit Fallnummer, Titel, Schwierigkeit in Sternen und Ausgangslage (wie Konzept Kapitel 43, Schritt 1)
- Alle Texte kommen aus `case.json`, nichts ist im Frontend fest eingetragen
- „Ermittlung starten" führt zum Editor; beim nächsten Öffnen des Falls geht es direkt zum Editor
- Das Briefing lässt sich während des Falls jederzeit wieder öffnen
- Frontend-Test für erstes und wiederholtes Öffnen

Konzept: F12

### SCRUM-43 · Ermittlungsziel und Teilziele sehen · 3 SP

Als Spieler möchte ich mein Endziel und die einzelnen Ermittlungsfragen sehen, damit ich weiß, wie weit ich
im Fall bin.

Akzeptanzkriterien:
- Das Ermittlungsziel ist während des ganzen Falls neben dem Editor sichtbar
- Alle Ermittlungsfragen stehen als Liste: gelöste abgehakt, die aktuelle hervorgehoben, spätere als gesperrt markiert
- Der Stand ist als Zahl sichtbar, z. B. „2 von 5 gelöst"
- Nach einer gelösten Mission aktualisiert sich die Liste ohne Neuladen der Seite
- Frontend-Test für alle drei Zustände

Konzept: F13, F14

### SCRUM-45 · Tabellenübersicht im Explorer · 3 SP

Als Spieler möchte ich sehen, welche Tabellen mir im Fall zur Verfügung stehen, damit ich weiß, wo ich mit
der Ermittlung anfangen kann.

Akzeptanzkriterien:
- Neben dem Editor listet ein Explorer alle Tabellen des aktuellen Falls mit ihrer Zeilenanzahl
- Angezeigt werden nur Tabellen dieses Falls: keine Systemtabellen, keine Tabellen anderer Fälle, nichts aus der Anwendungsdatenbank
- Die Liste liefert das Backend aus den geladenen Falldaten, nicht über eine Spielerabfrage
- Die fest eingetragene Tabellenliste in `frontend/src/pages/EditorPage.tsx` entfällt
- API-Test und Frontend-Test

Konzept: F20

### SCRUM-46 · Tabellenschema anzeigen · 3 SP

Als Spieler möchte ich Spalten, Datentypen und Verknüpfungen einer Tabelle untersuchen, damit ich die
richtigen Spaltennamen und JOINs in meine Abfragen schreibe, ohne zu raten.

Akzeptanzkriterien:
- Ein Klick auf eine Tabelle im Explorer zeigt ihre Spalten mit PostgreSQL-Datentyp (z. B. `integer`, `varchar(50)`, `timestamp`)
- Primärschlüssel und Pflichtfelder (NOT NULL) sind gekennzeichnet
- Fremdschlüssel sind mit Zieltabelle und Zielspalte gekennzeichnet, z. B. `assigned_to → persons.id`; ein Klick darauf öffnet die Zieltabelle
- Das Schema ist sichtbar, ohne den Editor zu verlassen; der eingegebene SQL-Text bleibt erhalten
- Auf einem Laptopbildschirm (1366 × 768) scrollt die Seite nicht horizontal
- API-Test, dass nur Tabellen des aktuellen Falls abgefragt werden können, und Frontend-Test

Konzept: F21, Fremdschlüssel aus F22 – eine eigene Beziehungsansicht bleibt bei SCRUM-47

### SCRUM-40 · Fallstatus darstellen · 2 SP

Als Spieler möchte ich erkennen, ob ein Fall gesperrt, verfügbar, in Arbeit oder gelöst ist, damit ich sehe,
wo ich weitermachen kann.

Akzeptanzkriterien:
- Jeder Fall in der Übersicht zeigt seinen Status Gesperrt, Verfügbar, In Arbeit oder Gelöst – als Text und Symbol, nicht nur über die Farbe
- Der Status kommt aus dem gespeicherten Fortschritt des angemeldeten Spielers (SCRUM-33); jeder sieht seinen eigenen
- Nach dem Lösen eines Falls (SCRUM-60) steht er in der Übersicht auf Gelöst
- Die Regeln, wann ein Fall freigeschaltet wird, kommen mit SCRUM-41; bis dahin ist kein Fall gesperrt, der Zustand Gesperrt wird aber schon dargestellt
- Frontend-Test für alle vier Zustände

Konzept: F11

### SCRUM-58 · Aktuelle Mission anzeigen · 2 SP

Als Spieler möchte ich jederzeit sehen, welche Mission gerade offen ist, damit ich weiß, welche Frage meine
nächste Abfrage beantworten soll.

Akzeptanzkriterien:
- Die aktuelle Mission steht direkt über dem Editor, z. B. „Mission 2 von 5" mit der Frage
- Nach dem Lösen erscheint eine Erfolgsmeldung und ohne Neuladen die nächste Mission
- Nach der letzten Mission erscheint statt einer Mission der Weg zum Fallabschluss (SCRUM-60)
- Nach Logout und Login wird wieder die richtige Mission angezeigt
- Frontend-Test für Missionswechsel und letzte Mission

Konzept: F43
