# ---!Ups

ALTER TABLE person_reperibility_types ADD COLUMN supervisor bigint REFERENCES persons(id);
ALTER TABLE person_reperibility_types_history ADD COLUMN supervisor bigint;

# ---!Downs

ALTER TABLE person_reperibility_types DROP COLUMN supervisor;
ALTER TABLE person_reperibility_types_history DROP COLUMN supervisor;