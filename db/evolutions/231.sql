# --- !Ups

-- Afferenza periodicizzata di una persona ad una sede
CREATE TABLE person_offices (
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES persons(id),
    office_id BIGINT NOT NULL REFERENCES office(id),
    begin_date DATE NOT NULL,
    end_date DATE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    version INTEGER DEFAULT 0,
    CONSTRAINT person_offices_dates_check CHECK (end_date IS NULL OR end_date >= begin_date)
);

CREATE INDEX person_offices_person_id_idx ON person_offices(person_id);
CREATE INDEX person_offices_office_id_idx ON person_offices(office_id);
CREATE INDEX person_offices_dates_idx ON person_offices(office_id, begin_date, end_date);

-- Una sola afferenza aperta per persona
CREATE UNIQUE INDEX person_offices_open_uidx ON person_offices(person_id) WHERE end_date IS NULL;

CREATE TABLE person_offices_history (
    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,
    person_id BIGINT,
    office_id BIGINT,
    begin_date DATE,
    end_date DATE,
    CONSTRAINT person_offices_history_pkey PRIMARY KEY (id, _revision)
);

-- Migrazione: l'afferenza attuale decorre dall'inizio del primo contratto della persona,
-- in mancanza di contratti dalla data di inizio della persona.
INSERT INTO person_offices (person_id, office_id, begin_date, updated_at)
SELECT p.id, p.office_id,
       COALESCE((SELECT MIN(c.begin_date) FROM contracts c WHERE c.person_id = p.id),
                p.begin_date::date, DATE '1970-01-01'),
       NOW()
FROM persons p
WHERE p.office_id IS NOT NULL;

-- La colonna persons.office_id è deprecata: non più valorizzata dall'applicazione,
-- viene mantenuta per un periodo di transizione e verrà rimossa in una release successiva.
ALTER TABLE persons ALTER COLUMN office_id DROP NOT NULL;
COMMENT ON COLUMN persons.office_id IS 'DEPRECATA: sostituita da person_offices';

# --- !Downs

-- Riallinea persons.office_id con l'ultima afferenza nota prima di eliminare la tabella
UPDATE persons p SET office_id = po.office_id
FROM (SELECT DISTINCT ON (person_id) person_id, office_id
      FROM person_offices ORDER BY person_id, begin_date DESC) po
WHERE po.person_id = p.id;

COMMENT ON COLUMN persons.office_id IS NULL;
DROP TABLE person_offices_history;
DROP TABLE person_offices;
