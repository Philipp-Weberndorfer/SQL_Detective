-- Ermittlungsdaten fuer Fall #001 "Der verschwundene Laptop" (F19).
--
-- Zeitraum: Montag, 28.09.2026 bis Montag, 05.10.2026. Die entscheidenden
-- Ereignisse liegen zwischen Freitag, 02.10., 16:30 Uhr und Montag, 05.10.,
-- 07:15 Uhr. Die Schulwoche davor sorgt dafuer, dass die Tabellen nicht nur
-- aus den wenigen wichtigen Zeilen bestehen.
--
-- Die IDs sind fest vergeben, damit die Verweise unten lesbar bleiben.
-- Der "Hintergrund" (normaler Schulbetrieb) wird mit generate_series erzeugt -
-- deterministisch und immer ausserhalb der Zeitfenster und Raeume, nach denen
-- die Missionen fragen.
--
-- Loesungen stehen NICHT hier, sondern nur in case.json (fall-format.md, Regel 1).

-- ------------------------------------------------------------------ Personen

INSERT INTO persons (id, first_name, last_name, role, class_name, card_id) VALUES
    ( 1, 'Max',       'Mustermann', 'student', '4AHIT', 'C-1001'),
    ( 2, 'Anna',      'Berger',     'student', '4AHIT', 'C-1002'),
    ( 3, 'Lisa',      'Hofer',      'student', '4BHIT', 'C-1003'),
    ( 4, 'Tobias',    'Gruber',     'student', '4AHIT', 'C-1004'),
    ( 5, 'Jonas',     'Reiter',     'student', '4BHIT', 'C-1005'),
    ( 6, 'Herwig',    'Lang',       'teacher', NULL,    'C-2001'),
    ( 7, 'Sabine',    'Moser',      'teacher', NULL,    'C-2002'),
    ( 8, 'Peter',     'Wimmer',     'staff',   NULL,    'C-3001'),
    ( 9, 'Elena',     'Fischer',    'student', '4AHIT', 'C-1006'),
    (10, 'David',     'Steiner',    'student', '4BHIT', 'C-1007'),
    (11, 'Laura',     'Huber',      'student', '4AHIT', 'C-1008'),
    (12, 'Florian',   'Wagner',     'student', '4AHIT', 'C-1009'),
    (13, 'Sophie',    'Bauer',      'student', '4AHIT', 'C-1010'),
    (14, 'Lukas',     'Pichler',    'student', '4AHIT', 'C-1011'),
    (15, 'Hannah',    'Leitner',    'student', '4AHIT', 'C-1012'),
    (16, 'Michael',   'Eder',       'student', '4AHIT', 'C-1013'),
    (17, 'Julia',     'Schwarz',    'student', '4AHIT', 'C-1014'),
    (18, 'Simon',     'Aigner',     'student', '4AHIT', 'C-1015'),
    (19, 'Lena',      'Brunner',    'student', '4BHIT', 'C-1016'),
    (20, 'Paul',      'Mayr',       'student', '4BHIT', 'C-1017'),
    (21, 'Marie',     'Lechner',    'student', '4BHIT', 'C-1018'),
    (22, 'Felix',     'Haas',       'student', '4BHIT', 'C-1019'),
    (23, 'Clara',     'Wallner',    'student', '4BHIT', 'C-1020'),
    (24, 'Elias',     'Kogler',     'student', '4BHIT', 'C-1021'),
    (25, 'Mia',       'Strasser',   'student', '4BHIT', 'C-1022'),
    (26, 'Noah',      'Wolf',       'student', '4BHIT', 'C-1023'),
    (27, 'Andrea',    'Koller',     'teacher', NULL,    'C-2003'),
    (28, 'Thomas',    'Weiß',       'teacher', NULL,    'C-2004'),
    (29, 'Martina',   'Schmid',     'staff',   NULL,    'C-3002'),
    (30, 'Gerhard',   'Fuchs',      'teacher', NULL,    'C-2005'),
    (31, 'Katharina', 'Reisinger',  'teacher', NULL,    'C-2006'),
    (32, 'Stefan',    'Maier',      'staff',   NULL,    'C-3003');

-- -------------------------------------------------------------------- Raeume

INSERT INTO rooms (id, name, floor, room_type) VALUES
    (1, 'Labor 1',      2, 'lab'),
    (2, 'Labor 2',      2, 'lab'),
    (3, 'Labor 3',      2, 'lab'),         -- das Netzwerklabor
    (4, 'Klasse 4AHIT', 1, 'classroom'),
    (5, 'Klasse 4BHIT', 1, 'classroom'),
    (6, 'Lehrerzimmer', 1, 'office'),
    (7, 'Sekretariat',  0, 'office'),
    (8, 'Haupteingang', 0, 'entrance'),
    (9, 'Bibliothek',   1, 'library');

-- ------------------------------------------------------------------- Geraete

INSERT INTO devices (id, inventory_no, device_type, model, assigned_to, home_room_id, mac_address) VALUES
    ( 1, 'NB-040', 'notebook', 'Lenovo ThinkPad T14',   11,   3, 'AA:BB:CC:00:00:40'),
    ( 2, 'NB-041', 'notebook', 'Lenovo ThinkPad T14',   12,   3, 'AA:BB:CC:00:00:41'),
    ( 3, 'NB-042', 'notebook', 'Lenovo ThinkPad T14',    1,   3, 'AA:BB:CC:00:00:42'),
    ( 4, 'NB-043', 'notebook', 'Lenovo ThinkPad T14',    2,   3, 'AA:BB:CC:00:00:43'),
    ( 5, 'NB-044', 'notebook', 'Lenovo ThinkPad T14',    6,   3, 'AA:BB:CC:00:00:44'),
    ( 6, 'NB-045', 'notebook', 'Lenovo ThinkPad T14',    9,   3, 'AA:BB:CC:00:00:45'),
    ( 7, 'NB-046', 'notebook', 'Lenovo ThinkPad T14',   14,   3, 'AA:BB:CC:00:00:46'),
    ( 8, 'NB-047', 'notebook', 'Lenovo ThinkPad T14',   NULL, 3, 'AA:BB:CC:00:00:47'),
    ( 9, 'NB-050', 'notebook', 'Dell Latitude 5440',     3,   2, 'AA:BB:CC:00:00:50'),
    (10, 'NB-051', 'notebook', 'Dell Latitude 5440',     5,   2, 'AA:BB:CC:00:00:51'),
    (11, 'NB-052', 'notebook', 'Dell Latitude 5440',    10,   2, 'AA:BB:CC:00:00:52'),
    (12, 'NB-053', 'notebook', 'Dell Latitude 5440',    19,   2, 'AA:BB:CC:00:00:53'),
    (13, 'PC-101', 'desktop',  'Dell OptiPlex 7010',    NULL, 1, 'AA:BB:CC:00:01:01'),
    (14, 'PC-102', 'desktop',  'Dell OptiPlex 7010',    NULL, 1, 'AA:BB:CC:00:01:02'),
    (15, 'PC-103', 'desktop',  'Dell OptiPlex 7010',    NULL, 1, 'AA:BB:CC:00:01:03'),
    (16, 'PC-104', 'desktop',  'Dell OptiPlex 7010',    NULL, 1, 'AA:BB:CC:00:01:04'),
    (17, 'PH-011', 'phone',    'Samsung Galaxy A54',     4,   NULL, 'AA:BB:CC:00:02:11'),
    (18, 'PH-012', 'phone',    'Apple iPhone 13',        3,   NULL, 'AA:BB:CC:00:02:12'),
    (19, 'PH-013', 'phone',    'Google Pixel 7a',        2,   NULL, 'AA:BB:CC:00:02:13'),
    (20, 'PH-014', 'phone',    'Samsung Galaxy S23',     1,   NULL, 'AA:BB:CC:00:02:14'),
    (21, 'PH-015', 'phone',    'Apple iPhone 14',        9,   NULL, 'AA:BB:CC:00:02:15'),
    (22, 'PH-016', 'phone',    'Xiaomi Redmi Note 12',   5,   NULL, 'AA:BB:CC:00:02:16'),
    (23, 'PH-030', 'phone',    'Samsung Galaxy A34',     6,   NULL, 'AA:BB:CC:00:02:30'),
    (24, 'PH-031', 'phone',    'Nokia G22',              8,   NULL, 'AA:BB:CC:00:02:31'),
    (25, 'PH-032', 'phone',    'Apple iPhone 12',        7,   NULL, 'AA:BB:CC:00:02:32'),
    (26, 'PH-033', 'phone',    'Samsung Galaxy A14',    29,   NULL, 'AA:BB:CC:00:02:33');

-- -------------------------------------------------------------- Stundenplan

INSERT INTO lessons (room_id, teacher_id, subject, class_name, starts_at, ends_at) VALUES
    (3, 30, 'Netzwerktechnik',         '4BHIT', '2026-10-02 08:00', '2026-10-02 09:40'),
    (4, 27, 'Deutsch',                 '4AHIT', '2026-10-02 08:00', '2026-10-02 09:40'),
    (4, 28, 'Englisch',                '4AHIT', '2026-10-02 10:00', '2026-10-02 11:40'),
    (1, 31, 'Programmieren',           '4BHIT', '2026-10-02 10:00', '2026-10-02 11:40'),
    (5,  7, 'Mathematik',              '4BHIT', '2026-10-02 13:00', '2026-10-02 14:40'),
    (4, 27, 'Geschichte',              '4AHIT', '2026-10-02 13:00', '2026-10-02 14:20'),
    (3,  6, 'Systemtechnik',           '4AHIT', '2026-10-02 14:30', '2026-10-02 16:10'),
    (2,  7, 'Mathematik (Förderkurs)', '4BHIT', '2026-10-02 16:00', '2026-10-02 17:30');

-- ----------------------------------------------------------- Zutrittssystem

-- Hintergrund: Montag bis Donnerstag kommen und gehen alle Schuelerinnen und
-- Schueler durch den Haupteingang.
INSERT INTO access_logs (person_id, room_id, access_time, direction)
SELECT p.id, 8, d + interval '7 hours 20 minutes' + p.id * interval '1 minute', 'IN'
FROM persons p
CROSS JOIN generate_series(timestamp '2026-09-28', timestamp '2026-10-01', interval '1 day') AS d
WHERE p.role = 'student';

INSERT INTO access_logs (person_id, room_id, access_time, direction)
SELECT p.id, 8, d + interval '15 hours' + (p.id % 30) * interval '1 minute', 'OUT'
FROM persons p
CROSS JOIN generate_series(timestamp '2026-09-28', timestamp '2026-10-01', interval '1 day') AS d
WHERE p.role = 'student';

-- Freitag frueh: alle kommen.
INSERT INTO access_logs (person_id, room_id, access_time, direction)
SELECT p.id, 8, timestamp '2026-10-02 07:20' + p.id * interval '1 minute', 'IN'
FROM persons p
WHERE p.role = 'student';

-- Freitag: die 4BHIT geht nach Mathematik (ausser Lisa, die hat Foerderkurs).
INSERT INTO access_logs (person_id, room_id, access_time, direction)
SELECT p.id, 8, timestamp '2026-10-02 14:45' + (p.id % 10) * interval '1 minute', 'OUT'
FROM persons p
WHERE p.class_name = '4BHIT' AND p.id <> 3;

-- Freitag: die 4AHIT hat bis 16:10 Systemtechnik in Labor 3 und geht danach
-- (Max, Tobias und Elena stehen weiter unten einzeln).
INSERT INTO access_logs (person_id, room_id, access_time, direction)
SELECT p.id, 3, timestamp '2026-10-02 14:25' + (p.id % 5) * interval '1 minute', 'IN'
FROM persons p WHERE p.class_name = '4AHIT' AND p.id NOT IN (1, 4, 9);

INSERT INTO access_logs (person_id, room_id, access_time, direction)
SELECT p.id, 3, timestamp '2026-10-02 16:10' + (p.id % 4) * interval '1 minute', 'OUT'
FROM persons p WHERE p.class_name = '4AHIT' AND p.id NOT IN (1, 4, 9);

INSERT INTO access_logs (person_id, room_id, access_time, direction)
SELECT p.id, 8, timestamp '2026-10-02 16:15' + (p.id % 5) * interval '1 minute', 'OUT'
FROM persons p WHERE p.class_name = '4AHIT' AND p.id NOT IN (1, 4, 9);

-- Die Beteiligten am Freitag
INSERT INTO access_logs (person_id, room_id, access_time, direction) VALUES
    -- Max Mustermann: Systemtechnik, geht kurz vor dem Ende
    (1, 3, '2026-10-02 14:27', 'IN'),
    (1, 3, '2026-10-02 16:08', 'OUT'),
    (1, 8, '2026-10-02 16:12', 'OUT'),
    -- Tobias Gruber: geht nach dem Unterricht - und kommt am Abend zurueck
    (4, 3, '2026-10-02 14:28', 'IN'),
    (4, 3, '2026-10-02 16:11', 'OUT'),
    (4, 8, '2026-10-02 16:14', 'OUT'),
    (4, 8, '2026-10-02 18:36', 'IN'),
    (4, 3, '2026-10-02 18:42', 'IN'),
    (4, 3, '2026-10-02 19:02', 'OUT'),
    (4, 8, '2026-10-02 19:05', 'OUT'),
    -- Elena Fischer: nach dem Unterricht in der Bibliothek
    (9, 3, '2026-10-02 14:26', 'IN'),
    (9, 3, '2026-10-02 16:10', 'OUT'),
    (9, 9, '2026-10-02 16:18', 'IN'),
    (9, 9, '2026-10-02 17:20', 'OUT'),
    (9, 8, '2026-10-02 17:24', 'OUT'),
    -- Herwig Lang: raeumt nach dem Unterricht auf, holt um 17:10 sein Notebook
    (6, 8, '2026-10-02 07:40', 'IN'),
    (6, 3, '2026-10-02 14:22', 'IN'),
    (6, 3, '2026-10-02 16:35', 'OUT'),
    (6, 6, '2026-10-02 16:38', 'IN'),
    (6, 6, '2026-10-02 17:05', 'OUT'),
    (6, 3, '2026-10-02 17:10', 'IN'),
    (6, 3, '2026-10-02 17:14', 'OUT'),
    (6, 6, '2026-10-02 17:16', 'IN'),
    (6, 6, '2026-10-02 17:42', 'OUT'),
    (6, 8, '2026-10-02 17:46', 'OUT'),
    -- Sabine Moser und Lisa Hofer: Foerderkurs in Labor 2
    (7, 8, '2026-10-02 07:35', 'IN'),
    (7, 2, '2026-10-02 15:55', 'IN'),
    (7, 2, '2026-10-02 17:32', 'OUT'),
    (7, 8, '2026-10-02 17:36', 'OUT'),
    (3, 2, '2026-10-02 15:58', 'IN'),
    (3, 2, '2026-10-02 17:31', 'OUT'),
    (3, 8, '2026-10-02 17:34', 'OUT'),
    -- Peter Wimmer (Schulwart): Dienst am Tag, Schliessrunde am Abend
    (8, 8, '2026-10-02 06:30', 'IN'),
    (8, 8, '2026-10-02 15:00', 'OUT'),
    (8, 8, '2026-10-02 19:40', 'IN'),
    (8, 1, '2026-10-02 19:50', 'IN'),
    (8, 1, '2026-10-02 19:55', 'OUT'),
    (8, 3, '2026-10-02 20:00', 'IN'),
    (8, 3, '2026-10-02 20:10', 'OUT'),
    (8, 8, '2026-10-02 20:20', 'OUT'),
    -- Martina Schmid (Sekretariat) und Stefan Maier (IT)
    (29, 8, '2026-10-02 07:15', 'IN'),
    (29, 7, '2026-10-02 07:20', 'IN'),
    (29, 7, '2026-10-02 15:00', 'OUT'),
    (29, 8, '2026-10-02 15:05', 'OUT'),
    (32, 8, '2026-10-02 11:50', 'IN'),
    (32, 1, '2026-10-02 12:00', 'IN'),
    (32, 1, '2026-10-02 12:40', 'OUT'),
    (32, 8, '2026-10-02 13:00', 'OUT');

-- Samstag: Kontrollgang des Schulwarts
INSERT INTO access_logs (person_id, room_id, access_time, direction) VALUES
    (8, 8, '2026-10-03 09:00', 'IN'),
    (8, 7, '2026-10-03 09:05', 'IN'),
    (8, 7, '2026-10-03 09:30', 'OUT'),
    (8, 8, '2026-10-03 09:35', 'OUT');

-- Montag frueh: der Laptop fehlt
INSERT INTO access_logs (person_id, room_id, access_time, direction) VALUES
    (8,  8, '2026-10-05 06:30', 'IN'),
    (6,  8, '2026-10-05 07:05', 'IN'),
    (6,  3, '2026-10-05 07:15', 'IN'),
    (29, 8, '2026-10-05 07:20', 'IN'),
    (1,  8, '2026-10-05 07:40', 'IN'),
    (2,  8, '2026-10-05 07:42', 'IN');

-- ---------------------------------------------------------------- Anmeldungen

INSERT INTO device_logins (device_id, person_id, login_time, logout_time) VALUES
    (3,  1,  '2026-09-28 10:05', '2026-09-28 11:30'),
    (3,  1,  '2026-09-30 14:40', '2026-09-30 16:00'),
    (3,  11, '2026-10-01 08:10', '2026-10-01 09:30'),
    (13, 22, '2026-10-02 10:05', '2026-10-02 11:35'),
    (5,  6,  '2026-10-02 14:32', '2026-10-02 16:10'),
    (6,  9,  '2026-10-02 14:31', '2026-10-02 16:06'),
    (1,  11, '2026-10-02 14:33', '2026-10-02 16:05'),
    (2,  12, '2026-10-02 14:34', '2026-10-02 16:04'),
    (3,  1,  '2026-10-02 14:35', '2026-10-02 16:05'),
    (4,  2,  '2026-10-02 14:36', '2026-10-02 16:03'),
    (7,  14, '2026-10-02 14:37', '2026-10-02 16:02'),
    (9,  3,  '2026-10-02 16:03', '2026-10-02 17:27'),
    (5,  6,  '2026-10-02 17:11', '2026-10-02 17:13'),
    (3,  4,  '2026-10-02 18:45', '2026-10-02 18:58');

-- --------------------------------------------------------------------- WLAN

-- Hintergrund: Handys der Schuelerinnen und Schueler im Gang-WLAN, Montag bis Freitag
INSERT INTO wifi_connections (device_id, access_point, connected_at, disconnected_at)
SELECT dev.id, 'AP-Gang1',
       d + interval '7 hours 25 minutes' + dev.id * interval '1 minute',
       d + interval '12 hours' + dev.id * interval '1 minute'
FROM devices dev
JOIN persons p ON p.id = dev.assigned_to
CROSS JOIN generate_series(timestamp '2026-09-28', timestamp '2026-10-02', interval '1 day') AS d
WHERE dev.device_type = 'phone' AND p.role = 'student';

-- Hintergrund: Notebooks in Labor 2, Montag bis Donnerstag am Vormittag
INSERT INTO wifi_connections (device_id, access_point, connected_at, disconnected_at)
SELECT dev.id, 'AP-Labor2',
       d + interval '10 hours' + dev.id * interval '1 minute',
       d + interval '11 hours 40 minutes'
FROM devices dev
CROSS JOIN generate_series(timestamp '2026-09-28', timestamp '2026-10-01', interval '1 day') AS d
WHERE dev.home_room_id = 2;

-- Freitag: Notebooks in Labor 3 waehrend Systemtechnik (alle vor 16:15 getrennt)
INSERT INTO wifi_connections (device_id, access_point, connected_at, disconnected_at)
SELECT dev.id, 'AP-Labor3',
       timestamp '2026-10-02 14:30' + dev.id * interval '1 minute',
       timestamp '2026-10-02 16:05' + (dev.id % 5) * interval '1 minute'
FROM devices dev
WHERE dev.home_room_id = 3 AND dev.assigned_to IS NOT NULL;

-- Freitag nach dem Unterricht
INSERT INTO wifi_connections (device_id, access_point, connected_at, disconnected_at) VALUES
    (25, 'AP-Labor2',       '2026-10-02 15:56', '2026-10-02 17:31'),
    (18, 'AP-Labor2',       '2026-10-02 15:59', '2026-10-02 17:30'),
    (9,  'AP-Labor2',       '2026-10-02 16:02', '2026-10-02 17:28'),
    (21, 'AP-Bibliothek',   '2026-10-02 16:19', '2026-10-02 17:19'),
    (23, 'AP-Labor3',       '2026-10-02 17:10', '2026-10-02 17:15'),
    (5,  'AP-Labor3',       '2026-10-02 17:11', '2026-10-02 17:15'),
    (23, 'AP-Lehrerzimmer', '2026-10-02 17:16', '2026-10-02 17:42'),
    (17, 'AP-Eingang',      '2026-10-02 18:35', '2026-10-02 18:39'),
    (17, 'AP-Labor3',       '2026-10-02 18:40', '2026-10-02 19:03'),
    (3,  'AP-Labor3',       '2026-10-02 18:44', '2026-10-02 18:59'),
    (17, 'AP-Eingang',      '2026-10-02 19:03', '2026-10-02 19:06'),
    (3,  'AP-Eingang',      '2026-10-02 19:04', '2026-10-02 19:05'),
    (24, 'AP-Eingang',      '2026-10-02 19:40', '2026-10-02 19:48'),
    (24, 'AP-Labor3',       '2026-10-02 20:01', '2026-10-02 20:09'),
    (24, 'AP-Eingang',      '2026-10-02 20:15', '2026-10-02 20:20'),
    (23, 'AP-Labor3',       '2026-10-05 07:15', '2026-10-05 07:40');

-- ------------------------------------------------------------- Zeugenaussagen

INSERT INTO witness_statements (person_id, statement, recorded_at) VALUES
    (6, 'Nach dem Unterricht habe ich bis etwa 16:30 Uhr im Labor aufgeräumt. Um 17:10 Uhr habe ich nur kurz mein eigenes Notebook geholt – NB-042 lag da noch auf Max'' Platz.',
        '2026-10-05 08:00'),
    (1, 'Ich habe mich um 16:05 Uhr abgemeldet und den Laptop im Labor liegen lassen, weil ich ihn am Montag wieder brauche.',
        '2026-10-05 08:30'),
    (4, 'Ich bin am Freitag nach dem Unterricht sofort nach Hause gefahren. Am Abend war ich sicher nicht mehr in der Schule.',
        '2026-10-05 09:00'),
    (2, 'Ich habe Max gegen 16:05 Uhr noch im Labor gesehen. Der Laptop lag zugeklappt auf seinem Tisch.',
        '2026-10-05 09:20'),
    (8, 'Ich habe um 20:00 Uhr meine Runde gemacht und Labor 3 abgesperrt. Da war niemand mehr im Labor, aufgefallen ist mir nichts.',
        '2026-10-05 09:40'),
    (3, 'Ich war bis halb sechs im Förderkurs in Labor 2. Auf dem Gang habe ich niemanden gesehen.',
        '2026-10-05 10:00');
