-- --- Rev:228, Ups - 5d72292

-- Creazione tabella intermedia per la relazione molti-a-molti tra Person e Office
CREATE TABLE person_offices (
                                id BIGSERIAL PRIMARY KEY,
                                person_id BIGINT NOT NULL REFERENCES persons(id),
                                office_id BIGINT NOT NULL REFERENCES office(id),
                                begin_date DATE NOT NULL,
                                end_date DATE,
                                updated_at TIMESTAMP WITHOUT TIME ZONE,
                                version INTEGER DEFAULT 0
);

-- Indici per migliorare le performance delle query più comuni
CREATE INDEX idx_person_offices_person_id
    ON person_offices(person_id);

CREATE INDEX idx_person_offices_office_id
    ON person_offices(office_id);

CREATE INDEX idx_person_offices_begin_date
    ON person_offices(begin_date);

-- Garantisce una sola afferenza attiva per persona
CREATE UNIQUE INDEX uq_person_offices_active
    ON person_offices(person_id)
    WHERE end_date IS NULL;

-- Migrazione dei dati esistenti:
-- ogni persona viene associata all'ufficio attualmente presente in persons.office_id
INSERT INTO person_offices (
    person_id,
    office_id,
    begin_date,
    updated_at
)
SELECT
    p.id,
    p.office_id,
    COALESCE(p.begin_date, DATE '2000-01-01'),
    NOW()
FROM persons p
WHERE p.office_id IS NOT NULL;

-- Tabella per l'auditing (Envers)
CREATE TABLE person_offices_history (
                                        id BIGINT NOT NULL,
                                        rev INTEGER NOT NULL,
                                        revtype SMALLINT,
                                        person_id BIGINT,
                                        office_id BIGINT,
                                        begin_date DATE,
                                        end_date DATE,
                                        updated_at TIMESTAMP WITHOUT TIME ZONE,
                                        version INTEGER,
                                        PRIMARY KEY (id, rev)
);

-- Eliminazione delle view che dipendono da persons.office_id
DROP VIEW IF EXISTS covid_19_view;
DROP VIEW IF EXISTS personale_attivo_sedi_view;

-- Rimozione della colonna ormai sostituita dalla tabella di relazione
ALTER TABLE persons DROP COLUMN office_id;

-- Ricreazione view covid_19_view
CREATE VIEW covid_19_view AS
SELECT
    i.code AS sigla_istituto,
    i.name AS nome_istituto,
    o.name AS nome_sede,
    o.code AS codice_sede,
    o.code_id AS sede_id,
    pd.date,
    count(*) AS numero_codici_covid_19
FROM institutes i
         JOIN office o
              ON o.institute_id = i.id
                  AND (o.end_date <= now() OR o.end_date IS NULL)
         JOIN person_offices po
              ON po.office_id = o.id
                  AND po.end_date IS NULL
         JOIN persons p
              ON p.id = po.person_id
         JOIN person_days pd
              ON pd.person_id = p.id
         JOIN absences a
              ON a.person_day_id = pd.id
         JOIN absence_types at
              ON a.absence_type_id = at.id
WHERE at.code = 'COVID19'
GROUP BY
    pd.date,
    o.id,
    i.code,
    i.name
ORDER BY
    o.code_id;

-- Ricreazione view personale_attivo_sedi_view
CREATE VIEW personale_attivo_sedi_view AS
SELECT
    i.code AS sigla_istituto,
    i.name AS nome_istituto,
    o.name AS nome_sede,
    o.code AS codice_sede,
    o.code_id AS sede_id,
    count(*) AS personale_attivo
FROM institutes i
         JOIN office o
              ON o.institute_id = i.id
                  AND (o.end_date <= now() OR o.end_date IS NULL)
         JOIN person_offices po
              ON po.office_id = o.id
                  AND po.end_date IS NULL
         JOIN persons p
              ON p.id = po.person_id
         JOIN contracts c
              ON c.person_id = p.id
WHERE c.begin_date < now()::date
  AND (c.end_contract IS NULL OR c.end_contract >= now()::date)
  AND (c.end_date IS NULL OR c.end_date >= now()::date)
  AND c.on_certificate IS TRUE
GROUP BY
    o.id,
    i.code,
    i.name
ORDER BY
    o.code_id;