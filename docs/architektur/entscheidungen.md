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

**Entscheidung:** *(offen – in Sprint 1 zu entscheiden)*
**Alternativen:**
**Begründung:**
**Konsequenz:**
**Datum:**

---

## AE-05 · Frontend-Framework

**Entscheidung:** *(offen – in Sprint 1 zu entscheiden)*
**Alternativen:**
**Begründung:**
**Konsequenz:**
**Datum:**

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
