# Architekturentscheidungen

Jede größere Entscheidung bekommt hier einen Eintrag – mit **Begründung**, nicht nur mit Ergebnis.
Bei der Präsentation wird genau danach gefragt, und in einem halben Jahr weiß sonst niemand mehr, warum
etwas so gebaut wurde.

Format je Eintrag: Entscheidung · Alternativen · Begründung · Konsequenz · Datum

---

## AE-01 · PostgreSQL als Datenbanksystem

**Entscheidung:** PostgreSQL 16 für beide Datenbanken.
**Alternativen:** MySQL/MariaDB, SQLite.
**Begründung:** Im Konzept als F15 vorgegeben. Fachlich passt es ohnehin am besten, weil der Lernpfad bis
zu CTEs und Window Functions führt – die beherrscht PostgreSQL vollständig. Außerdem lassen sich
Benutzerrechte fein genug einstellen, um den Nur-Lese-Benutzer sauber umzusetzen.
**Konsequenz:** Die Fehlermeldungen, die Spieler sehen, sind PostgreSQL-Meldungen. Das Fallformat darf keine
Syntax enthalten, die nur PostgreSQL versteht, falls später ein Wechsel nötig würde.
**Datum:** 01.10.2026

---

## AE-02 · Zwei getrennte Datenbanken

**Entscheidung:** Anwendungsdatenbank und Spieldatenbank laufen als **getrennte PostgreSQL-Container**.
Spielerabfragen gehen ausschließlich über den Benutzer `game_readonly` gegen die Spieldatenbank.
**Alternativen:** Eine Datenbank mit getrennten Schemata und Rechten.
**Begründung:** Spieler geben beliebiges SQL ein. Mit einer einzigen Datenbank hinge die Trennung allein an
korrekt gesetzten Rechten – ein Fehler dort legt Benutzerdaten offen. Zwei Instanzen bedeuten, dass selbst
bei vollständig falsch konfigurierten Rechten keine Verbindung zu den Benutzerdaten besteht
(F108, F109, Defense in Depth).
**Konsequenz:** Das Backend hält zwei Verbindungen. Daten aus beiden Welten (z. B. Fortschritt und
Falldaten) müssen in der Anwendung zusammengeführt werden, nicht per JOIN.
**Datum:** 01.10.2026

---

## AE-03 · Fälle sind Daten, kein Code

**Entscheidung:** Ein Fall liegt vollständig als Dateien unter `cases/` – Briefing, Schema, Seed-Daten,
Missionen, Referenzlösungen, Hinweise. Siehe `fall-format.md`.
**Alternativen:** Fälle fest im Backend-Code hinterlegen.
**Begründung:** Sechs Fälle sind Pflicht (F19). Hart codiert müsste jeder einzeln entwickelt werden, und der
Case Editor für Lehrpersonen (F88–F94) wäre nachträglich kaum noch umsetzbar. Als Daten lässt sich ein neuer
Fall ohne Codeänderung hinzufügen.
**Konsequenz:** Das Fallformat muss früh stabil sein. Änderungen daran betreffen alle bereits gebauten Fälle.
**Datum:** 01.10.2026

---

## AE-04 · Backend-Framework

**Entscheidung:** Spring Boot (Java) mit Spring Security für Registrierung und Login.
**Alternativen:** Quarkus, Javalin.
**Begründung:** Spring Boot ist der verbreitetste Java-Standard, mit Abstand am besten dokumentiert und im
Unterricht bekannt. Spring Security liefert Passwort-Hashing (BCrypt) und Sitzungsverwaltung fertig, statt
dass wir sicherheitskritischen Code selbst schreiben (F01, F02). Zwei getrennte DataSources für
Anwendungs- und Spieldatenbank (AE-02) lassen sich sauber konfigurieren, ebenso Query-Timeout und
Zeilenlimit über JDBC (F34, F35). Quarkus wäre schlanker, hat aber weniger Lernmaterial; mit Javalin
müssten Security und Datenbankzugriff von Hand gebaut werden.
**Konsequenz:** Zwei DataSources müssen explizit konfiguriert werden – die Spieldatenbank ausschließlich mit
dem Benutzer `game_readonly`. Spring Boot startet langsamer und braucht mehr Speicher als die Alternativen,
was für das Projekt keine Rolle spielt.
**Datum:** 01.10.2026

---

## AE-05 · Frontend-Framework

**Entscheidung:** React mit TypeScript, gebaut mit Vite.
**Alternativen:** Vue, Angular.
**Begründung:** React hat das größte Ökosystem. Für die zentralen Bausteine – SQL-Editor mit
Syntaxhervorhebung (Monaco oder CodeMirror), Ergebnistabelle, Evidence Board – gibt es ausgereifte
Komponenten. Vite liefert schnellen Entwicklungsserver und einfachen Build. Vue wäre einsteigerfreundlicher,
hat aber weniger passende Komponenten; Angular bringt für ein Team von drei Personen zu viel Struktur und
Einarbeitung mit.
**Konsequenz:** Routing, Formularvalidierung und Zustandsverwaltung sind in React nicht eingebaut und müssen
bewusst gewählt werden (z. B. React Router). Der Entwicklungsserver läuft auf Port 5173.
**Datum:** 01.10.2026

---

## AE-06 · LLM-Anbieter und Modell

**Entscheidung:** *(offen – in Sprint 1 zu entscheiden)*
**Zu klären:** Welches Modell, wer zahlt die Kosten, wie wird der Verbrauch je Spieler begrenzt.
**Hinweis:** Weil Schema und Fallkontext in jeder Anfrage gleich sind, spart Prompt Caching hier besonders
viel. Ein Nutzungslimit pro Spieler sollte von Anfang an eingebaut werden – ein Testlauf mit einer ganzen
Klasse erzeugt sonst unerwartet hohe Kosten.
**Datum:**

---

## AE-07 · Anbindung des MCP-Servers

**Entscheidung:** *(offen – in Sprint 1 zu entscheiden)*
**Zu klären:** Wird der eigene MCP-Server über den MCP-Connector der LLM-API angebunden, oder werden die
Tools direkt als Tool-Definitionen übergeben? Das beeinflusst den Aufbau des Servers und sollte vor dem
Start von Sprint 3 feststehen.
**Datum:**
