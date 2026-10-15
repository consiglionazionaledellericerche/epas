# --- !Ups

-- Per una persona e un'attività di turno i periodi di associazione non devono sovrapporsi.
-- Le date di inizio e di fine sono comprese nel periodo, una data di fine nulla indica un
-- periodo aperto.

-- Si eliminano i periodi contenuti in un altro periodo della stessa persona sulla stessa
-- attività (a parità di periodo si mantiene quello con id minore)
DELETE FROM person_shift_shift_type b
USING person_shift_shift_type a
WHERE a.personshifts_id = b.personshifts_id AND a.shifttypes_id = b.shifttypes_id
  AND a.id <> b.id
  AND a.begin_date <= b.begin_date
  AND (a.end_date IS NULL OR (b.end_date IS NOT NULL AND b.end_date <= a.end_date))
  AND (a.begin_date <> b.begin_date OR a.end_date IS DISTINCT FROM b.end_date OR a.id < b.id);

-- I periodi rimasti che si sovrappongono al successivo terminano il giorno prima del suo inizio
UPDATE person_shift_shift_type p SET end_date = n.next_begin_date - 1
FROM (
  SELECT id, LEAD(begin_date) OVER (PARTITION BY personshifts_id, shifttypes_id
    ORDER BY begin_date) AS next_begin_date
  FROM person_shift_shift_type
) n
WHERE p.id = n.id AND n.next_begin_date IS NOT NULL
  AND (p.end_date IS NULL OR p.end_date >= n.next_begin_date);

ALTER TABLE person_shift_shift_type ALTER COLUMN begin_date SET NOT NULL;

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE person_shift_shift_type ADD CONSTRAINT person_shift_shift_type_no_overlap
EXCLUDE USING gist (personshifts_id WITH =, shifttypes_id WITH =,
  daterange(begin_date, end_date, '[]') WITH &&);

# --- !Downs

-- I periodi eliminati o accorciati non vengono ripristinati
ALTER TABLE person_shift_shift_type DROP CONSTRAINT person_shift_shift_type_no_overlap;
ALTER TABLE person_shift_shift_type ALTER COLUMN begin_date DROP NOT NULL;
