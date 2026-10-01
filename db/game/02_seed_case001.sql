-- Startdatensatz fuer Fall #001 "Der verschwundene Laptop".
--
-- ACHTUNG: Das ist ein Geruest fuer Sprint 1, damit der SQL-Editor etwas zum
-- Abfragen hat. Das Konzept verlangt in F19 ausdruecklich glaubwuerdige Daten
-- und nicht nur eine Handvoll Testdatensaetze - die vollstaendigen Falldaten
-- entstehen in Sprint 2 zusammen mit den Missionen.
--
-- Zeitlinie: Freitag, 02.10.2026 - Montag, 05.10.2026

-- ------------------------------------------------------------------ Personen

INSERT INTO persons (first_name, last_name, role, class_name, card_id) VALUES
    ('Max',    'Mustermann', 'student', '4AHIT', 'C-1001'),
    ('Anna',   'Berger',     'student', '4AHIT', 'C-1002'),
    ('Lisa',   'Hofer',      'student', '4BHIT', 'C-1003'),
    ('Tobias', 'Gruber',     'student', '4AHIT', 'C-1004'),
    ('Jonas',  'Reiter',     'student', '4BHIT', 'C-1005'),
    ('Herwig', 'Lang',       'teacher', NULL,    'C-2001'),
    ('Sabine', 'Moser',      'teacher', NULL,    'C-2002'),
    ('Peter',  'Wimmer',     'staff',   NULL,    'C-3001'),
    ('Elena',  'Fischer',    'student', '4AHIT', 'C-1006'),
    ('David',  'Steiner',    'student', '4BHIT', 'C-1007');

-- -------------------------------------------------------------------- Raeume

INSERT INTO rooms (name, floor, room_type) VALUES
    ('Labor 1',      2, 'lab'),
    ('Labor 2',      2, 'lab'),
    ('Labor 3',      2, 'lab'),        -- das Netzwerklabor
    ('Klasse 4AHIT', 1, 'classroom'),
    ('Sekretariat',  0, 'office');

-- ------------------------------------------------------------------- Geraete

INSERT INTO devices (inventory_no, device_type, model, assigned_to, home_room_id, mac_address) VALUES
    ('NB-042', 'notebook', 'Lenovo ThinkPad T14',  1,    3, 'AA:BB:CC:00:00:42'),
    ('NB-043', 'notebook', 'Lenovo ThinkPad T14',  2,    3, 'AA:BB:CC:00:00:43'),
    ('NB-044', 'notebook', 'Lenovo ThinkPad T14',  6,    3, 'AA:BB:CC:00:00:44'),
    ('PC-101', 'desktop',  'Dell OptiPlex 7010',   NULL, 1, 'AA:BB:CC:00:01:01'),
    ('PH-011', 'phone',    'Samsung Galaxy A54',   4,    NULL, 'AA:BB:CC:00:02:11'),
    ('PH-012', 'phone',    'Apple iPhone 13',      3,    NULL, 'AA:BB:CC:00:02:12');

-- ----------------------------------------------------------- Zutrittssystem

INSERT INTO access_logs (person_id, room_id, access_time, direction) VALUES
    (2, 4, '2026-10-02 13:00:00', 'IN'),
    (2, 4, '2026-10-02 15:30:00', 'OUT'),
    (1, 3, '2026-10-02 14:00:00', 'IN'),
    (1, 3, '2026-10-02 16:20:00', 'OUT'),
    (6, 3, '2026-10-02 15:00:00', 'IN'),
    (6, 3, '2026-10-02 16:35:00', 'OUT'),
    (3, 2, '2026-10-02 16:00:00', 'IN'),
    (3, 2, '2026-10-02 17:30:00', 'OUT'),
    (4, 3, '2026-10-02 18:42:00', 'IN'),
    (4, 3, '2026-10-02 19:05:00', 'OUT'),
    (8, 3, '2026-10-02 20:00:00', 'IN'),
    (8, 3, '2026-10-02 20:10:00', 'OUT'),
    (6, 3, '2026-10-05 07:15:00', 'IN');

-- ---------------------------------------------------------------- Anmeldungen

INSERT INTO device_logins (device_id, person_id, login_time, logout_time) VALUES
    (2, 2, '2026-10-02 13:10:00', '2026-10-02 15:20:00'),
    (1, 1, '2026-10-02 14:05:00', '2026-10-02 16:15:00'),
    (1, 4, '2026-10-02 18:45:00', '2026-10-02 18:58:00'),
    (3, 6, '2026-10-02 15:05:00', '2026-10-02 16:30:00');

-- --------------------------------------------------------------------- WLAN

INSERT INTO wifi_connections (device_id, access_point, connected_at, disconnected_at) VALUES
    (2, 'AP-Lab3', '2026-10-02 13:05:00', '2026-10-02 15:25:00'),
    (1, 'AP-Lab3', '2026-10-02 14:02:00', '2026-10-02 16:18:00'),
    (3, 'AP-Lab3', '2026-10-02 15:02:00', '2026-10-02 16:32:00'),
    (1, 'AP-Lab3', '2026-10-02 18:44:00', '2026-10-02 18:59:00'),
    (5, 'AP-Lab3', '2026-10-02 18:40:00', '2026-10-02 19:06:00');

-- -------------------------------------------------------------- Stundenplan

INSERT INTO lessons (room_id, teacher_id, subject, class_name, starts_at, ends_at) VALUES
    (4, 7, 'Deutsch',        '4AHIT', '2026-10-02 13:00:00', '2026-10-02 14:40:00'),
    (3, 6, 'Systemtechnik',  '4AHIT', '2026-10-02 15:00:00', '2026-10-02 16:30:00'),
    (2, 7, 'Mathematik',     '4BHIT', '2026-10-02 16:00:00', '2026-10-02 17:30:00');

-- ------------------------------------------------------------- Zeugenaussagen

INSERT INTO witness_statements (person_id, statement, recorded_at) VALUES
    (4, 'Ich bin am Freitag nach dem Unterricht sofort nach Hause gefahren.',
        '2026-10-05 09:00:00'),
    (2, 'Ich habe Max gegen 16:15 noch im Labor gesehen, der Laptop lag am Tisch.',
        '2026-10-05 09:20:00'),
    (8, 'Ich habe um 20:00 Uhr abgesperrt. Da war niemand mehr im Labor.',
        '2026-10-05 09:40:00');
