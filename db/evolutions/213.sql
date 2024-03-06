# --- !Ups

ALTER TABLE absence_types ADD COLUMN external_id TEXT;
ALTER TABLE absence_types_history ADD COLUMN external_id TEXT;

# --- !Downs

ALTER TABLE absence_types DROP COLUMN external_id;
ALTER TABLE absence_types_history DROP COLUMN external_id;