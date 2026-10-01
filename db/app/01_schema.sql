-- Anwendungsdatenbank: Benutzer, Fortschritt, Spielstand.
-- Spieler-SQL erreicht diese Datenbank NIEMALS (F108, F109).
--
-- Laeuft automatisch beim ersten Start des Containers.
-- Nach Aenderungen: docker compose down -v && docker compose up

-- ---------------------------------------------------------------- Benutzer

CREATE TABLE users (
    id              SERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,     -- niemals Klartext (F01)
    display_name    VARCHAR(100),
    role            VARCHAR(20)  NOT NULL DEFAULT 'player',  -- player | teacher | admin
    xp              INTEGER      NOT NULL DEFAULT 0,
    level           INTEGER      NOT NULL DEFAULT 1,
    show_in_leaderboard BOOLEAN  NOT NULL DEFAULT FALSE,     -- F86
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------- Fortschritt im Fall

CREATE TABLE case_progress (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    case_id         VARCHAR(50) NOT NULL,      -- verweist auf cases/<case_id>/
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',  -- LOCKED | AVAILABLE | IN_PROGRESS | SOLVED
    current_mission VARCHAR(50),
    xp_earned       INTEGER     NOT NULL DEFAULT 0,
    started_at      TIMESTAMP,
    solved_at       TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, case_id)
);

CREATE TABLE mission_progress (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    case_id         VARCHAR(50) NOT NULL,
    mission_id      VARCHAR(50) NOT NULL,
    solved          BOOLEAN     NOT NULL DEFAULT FALSE,
    attempts        INTEGER     NOT NULL DEFAULT 0,
    max_hint_level  INTEGER     NOT NULL DEFAULT 0,   -- 0 = ohne Hinweis geloest (F81)
    solved_at       TIMESTAMP,
    UNIQUE (user_id, case_id, mission_id)
);

-- --------------------------------------------------- Notizbuch und Beweise

CREATE TABLE notes (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    case_id         VARCHAR(50) NOT NULL,
    text            TEXT        NOT NULL,
    linked_query    TEXT,                       -- zugehoerige Abfrage (F38)
    linked_result   JSONB,                      -- Ergebnis als Momentaufnahme
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE discovered_evidence (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    case_id         VARCHAR(50) NOT NULL,
    evidence_id     VARCHAR(50) NOT NULL,
    discovered_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, case_id, evidence_id)
);

-- ----------------------------------------------- Abfrageprotokoll, Analytics

CREATE TABLE query_log (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    case_id         VARCHAR(50),
    mission_id      VARCHAR(50),
    sql_text        TEXT        NOT NULL,
    success         BOOLEAN     NOT NULL,       -- F121
    error_type      VARCHAR(50),                -- F122: syntax | join | groupby | ...
    error_message   TEXT,
    duration_ms     INTEGER,
    row_count       INTEGER,
    executed_at     TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_query_log_user ON query_log(user_id, executed_at DESC);

CREATE TABLE hint_log (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    case_id         VARCHAR(50) NOT NULL,
    mission_id      VARCHAR(50) NOT NULL,
    hint_level      INTEGER     NOT NULL,       -- 1 bis 4, 5 = Komplettloesung
    requested_at    TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Prozentwerte je SQL-Thema werden aus nachvollziehbaren Lernereignissen
-- berechnet, nicht von der KI geschaetzt (F76).
CREATE TABLE skill_progress (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill           VARCHAR(30) NOT NULL,       -- SELECT | WHERE | JOIN | GROUP BY | SUBQUERY | CTE | WINDOW
    successes       INTEGER     NOT NULL DEFAULT 0,
    attempts        INTEGER     NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, skill)
);

-- ------------------------------------------------- Achievements, Klassen

CREATE TABLE achievements (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_key VARCHAR(50) NOT NULL,       -- first_case | join_the_force | ...
    earned_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, achievement_key)
);

CREATE TABLE classes (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    teacher_id      INTEGER      NOT NULL REFERENCES users(id),
    join_code       VARCHAR(20)  NOT NULL UNIQUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE class_members (
    class_id        INTEGER NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    user_id         INTEGER NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    joined_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (class_id, user_id)
);
