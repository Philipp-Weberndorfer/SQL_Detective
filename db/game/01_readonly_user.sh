#!/bin/bash
# Legt den Nur-Lese-Benutzer fuer die Spieldatenbank an (F33, F112).
#
# Ueber DIESEN Benutzer laufen alle Abfragen, die Spieler eingeben - und nur
# ueber ihn. Er ist die zweite Sicherheitsschicht: Selbst wenn die Validierung
# in der Anwendung versagt, scheitert "DROP TABLE persons;" hier an fehlenden
# Rechten (Defense in Depth).
#
# Die Falltabellen legt das Backend beim Start an, jeden Fall in einem eigenen
# Schema (AE-10). Dort vergibt es game_readonly ausschliesslich USAGE und SELECT.

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER ${GAME_DB_READONLY_USER} WITH PASSWORD '${GAME_DB_READONLY_PASSWORD}';

    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${GAME_DB_READONLY_USER};

    -- keine Moeglichkeit, Objekte anzulegen
    REVOKE CREATE ON SCHEMA public FROM ${GAME_DB_READONLY_USER};

    -- PostgreSQL erlaubt PUBLIC (also jedem Benutzer) standardmaessig, temporaere
    -- Tabellen anzulegen und das public-Schema zu nutzen. Beides entziehen -
    -- game_readonly behaelt nur die ausdruecklich vergebenen Rechte.
    REVOKE TEMPORARY ON DATABASE ${POSTGRES_DB} FROM PUBLIC;
    REVOKE ALL       ON SCHEMA public           FROM PUBLIC;
EOSQL

echo "Nur-Lese-Benutzer ${GAME_DB_READONLY_USER} angelegt."
