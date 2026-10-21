# --- !Ups

-- Nel mese di un trasferimento ogni sede invia ad Attestati i dati del proprio periodo:
-- certificazioni e competenze sono associate alla sede che le invia.
ALTER TABLE certifications ADD COLUMN office_id BIGINT REFERENCES office(id);
ALTER TABLE certifications_history ADD COLUMN office_id BIGINT;
CREATE INDEX certifications_office_id_idx ON certifications(office_id);

ALTER TABLE competences ADD COLUMN office_id BIGINT REFERENCES office(id);
ALTER TABLE competences_history ADD COLUMN office_id BIGINT;
CREATE INDEX competences_office_id_idx ON competences(office_id);
-- Nel mese del trasferimento la stessa competenza può esistere per entrambe le sedi
ALTER TABLE competences DROP CONSTRAINT IF EXISTS unique_integrity_key;

-- Dati esistenti: sede di afferenza all'ultimo giorno del mese, in mancanza la più recente
UPDATE certifications c SET office_id = (
  SELECT po.office_id FROM person_offices po
  WHERE po.person_id = c.person_id
    AND po.begin_date <= (make_date(c.year, c.month, 1) + INTERVAL '1 month - 1 day')::date
  ORDER BY po.begin_date DESC LIMIT 1);

UPDATE competences c SET office_id = (
  SELECT po.office_id FROM person_offices po
  WHERE po.person_id = c.person_id
    AND po.begin_date <= (make_date(c.year, c.month, 1) + INTERVAL '1 month - 1 day')::date
  ORDER BY po.begin_date DESC LIMIT 1);

ALTER TABLE competences ADD CONSTRAINT unique_integrity_key
  UNIQUE (person_id, competence_code_id, year, month, office_id);

# --- !Downs

ALTER TABLE competences DROP CONSTRAINT unique_integrity_key;
-- Le competenze del mese divise tra più sedi vengono ricondotte ad una sola riga sommandone
-- i valori
UPDATE competences k SET valueapproved = t.total FROM (
  SELECT MIN(id) AS id, SUM(valueapproved) AS total FROM competences
  GROUP BY person_id, competence_code_id, year, month HAVING COUNT(*) > 1) t
  WHERE k.id = t.id;
DELETE FROM competences c USING competences d
  WHERE c.person_id = d.person_id AND c.competence_code_id = d.competence_code_id
    AND c.year = d.year AND c.month = d.month AND c.id > d.id;
ALTER TABLE competences ADD CONSTRAINT unique_integrity_key
  UNIQUE (person_id, competence_code_id, year, month);

DROP INDEX competences_office_id_idx;
ALTER TABLE competences_history DROP COLUMN office_id;
ALTER TABLE competences DROP COLUMN office_id;

DROP INDEX certifications_office_id_idx;
ALTER TABLE certifications_history DROP COLUMN office_id;
ALTER TABLE certifications DROP COLUMN office_id;
