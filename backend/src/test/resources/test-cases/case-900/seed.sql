INSERT INTO suspects (name, city) VALUES
    ('Anna',  'Wels'),
    ('Bernd', 'Linz'),
    ('Clara', 'Wels');

INSERT INTO sightings (suspect_id, seen_at, place) VALUES
    (1, '2026-10-02 17:00:00', 'Kantine'),
    (3, '2026-10-02 19:30:00', 'Ausgang'),
    (3, '2026-10-02 18:00:00', 'Labor'),
    (2, '2026-10-02 18:15:00', 'Parkplatz');
