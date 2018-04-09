# ---!Ups

ALTER TABLE person_reperibility DROP CONSTRAINT person_reperibility_person_id_key;

# ---!Downs

ALTER TABLE person_reperibility ADD CONSTRAINT person_reperibility_person_id_key UNIQUE (person_id);