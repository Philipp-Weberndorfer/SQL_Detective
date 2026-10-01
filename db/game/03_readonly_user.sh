#!/bin/bash
# Legt den Nur-Lese-Benutzer fuer die Spieldatenbank an (F33, F112).
#
# Ueber DIESEN Benutzer laufen alle Abfragen, die Spieler eingeben - und nur
# ueber ihn. Er ist die zweite Sicherheitsschicht: Selbst wenn die Validierung
# in der Anwendung versagt, scheitert "DROP TABLE persons;" hier an fehlenden
# Rechten (Defense in Depth).
#
# Laeuft als 03_, also NACH Schema (01_) und Seed (02_) - die Tabellen muessen
# existieren, bevor Rechte darauf vergeben werden koennen.

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER ${GAME_DB_READONLY_USER} WITH PASSWORD '${GAME_DB_READONLY_PASSWORD}';

    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${GAME_DB_READONLY_USER};
    GRANT USAGE   ON SCHEMA public           TO ${GAME_DB_READONLY_USER};
    GRANT SELECT  ON ALL TABLES IN SCHEMA public TO ${GAME_DB_READONLY_USER};

    -- gilt auch fuer Tabellen, die spaeter dazukommen
    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT SELECT ON TABLES TO ${GAME_DB_READONLY_USER};

    -- keine Rechte auf Sequenzen, keine Moeglichkeit, Objekte anzulegen
    REVOKE CREATE ON SCHEMA public FROM ${GAME_DB_READONLY_USER};
EOSQL

echo "Nur-Lese-Benutzer ${GAME_DB_READONLY_USER} angelegt."
