CREATE TABLE suspects (
    id    SERIAL PRIMARY KEY,
    name  VARCHAR(50) NOT NULL,
    city  VARCHAR(50)
);

CREATE TABLE sightings (
    id          SERIAL PRIMARY KEY,
    suspect_id  INTEGER     NOT NULL REFERENCES suspects(id),
    seen_at     TIMESTAMP   NOT NULL,
    place       VARCHAR(50) NOT NULL
);
