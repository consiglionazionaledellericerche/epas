# ---!Ups

ALTER TABLE competences ADD CONSTRAINT unique_integrity_key UNIQUE (person_id, competence_code_id, year, month);

# ---!Downs

ALTER TABLE competences DROP CONSTRAINT unique_integrity_key;