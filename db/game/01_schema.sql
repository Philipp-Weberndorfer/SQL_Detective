-- Spieldatenbank: die Ermittlungsdaten, gegen die Spieler ihre Abfragen schreiben.
--
-- Hier liegen KEINE Loesungen und KEINE Benutzerdaten. Was ein Spieler hier
-- sieht, darf er sehen - die Herausforderung besteht darin, die richtigen
-- Zusammenhaenge zu finden, nicht darin, etwas Verstecktes aufzudecken.
--
-- Schema fuer Fall #001 "Der verschwundene Laptop" (F16, F17, F18).

-- ------------------------------------------------------------------ Personen

CREATE TABLE persons (
    id          SERIAL PRIMARY KEY,
    first_name  VARCHAR(50)  NOT NULL,
    last_name   VARCHAR(50)  NOT NULL,
    role        VARCHAR(20)  NOT NULL,      -- student | teacher | staff
    class_name  VARCHAR(20),
    card_id     VARCHAR(20)  UNIQUE         -- Zutrittskarte
);

-- -------------------------------------------------------------------- Raeume

CREATE TABLE rooms (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    floor       INTEGER     NOT NULL,
    room_type   VARCHAR(30) NOT NULL        -- lab | classroom | office
);

-- ------------------------------------------------------------------- Geraete

CREATE TABLE devices (
    id              SERIAL PRIMARY KEY,
    inventory_no    VARCHAR(20)  NOT NULL UNIQUE,   -- z. B. NB-042
    device_type     VARCHAR(30)  NOT NULL,          -- notebook | desktop | phone
    model           VARCHAR(60),
    assigned_to     INTEGER REFERENCES persons(id),
    home_room_id    INTEGER REFERENCES rooms(id),
    mac_address     VARCHAR(17)  UNIQUE
);

-- ----------------------------------------------------------- Zutrittssystem

CREATE TABLE access_logs (
    id          SERIAL PRIMARY KEY,
    person_id   INTEGER   NOT NULL REFERENCES persons(id),
    room_id     INTEGER   NOT NULL REFERENCES rooms(id),
    access_time TIMESTAMP NOT NULL,
    direction   VARCHAR(5) NOT NULL         -- IN | OUT
);

CREATE INDEX idx_access_logs_time ON access_logs(access_time);

-- ---------------------------------------------------------------- Anmeldungen

CREATE TABLE device_logins (
    id          SERIAL PRIMARY KEY,
    device_id   INTEGER   NOT NULL REFERENCES devices(id),
    person_id   INTEGER   NOT NULL REFERENCES persons(id),
    login_time  TIMESTAMP NOT NULL,
    logout_time TIMESTAMP
);

-- --------------------------------------------------------------------- WLAN

CREATE TABLE wifi_connections (
    id              SERIAL PRIMARY KEY,
    device_id       INTEGER   NOT NULL REFERENCES devices(id),
    access_point    VARCHAR(30) NOT NULL,
    connected_at    TIMESTAMP NOT NULL,
    disconnected_at TIMESTAMP
);

-- -------------------------------------------------------------- Stundenplan

CREATE TABLE lessons (
    id          SERIAL PRIMARY KEY,
    room_id     INTEGER   NOT NULL REFERENCES rooms(id),
    teacher_id  INTEGER   NOT NULL REFERENCES persons(id),
    subject     VARCHAR(50) NOT NULL,
    class_name  VARCHAR(20) NOT NULL,
    starts_at   TIMESTAMP NOT NULL,
    ends_at     TIMESTAMP NOT NULL
);

-- ------------------------------------------------------------- Zeugenaussagen

-- ACHTUNG: Freitext aus dieser Tabelle kann beim KI-Detektiv landen.
-- Er darf dort niemals als Anweisung wirken (F117, Prompt Injection).
CREATE TABLE witness_statements (
    id          SERIAL PRIMARY KEY,
    person_id   INTEGER   NOT NULL REFERENCES persons(id),
    statement   TEXT      NOT NULL,
    recorded_at TIMESTAMP NOT NULL
);
