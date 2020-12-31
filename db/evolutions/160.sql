# --- !Ups

ALTER TABLE affiliation ADD COLUMN external_id TEXT;
ALTER TABLE affiliation_history ADD COLUMN external_id TEXT;

ALTER TABLE contracts ADD COLUMN external_id TEXT;
ALTER TABLE contracts_history ADD COLUMN external_id TEXT;

ALTER TABLE working_time_types ADD COLUMN external_id TEXT;
ALTER TABLE working_time_types_history ADD COLUMN external_id TEXT;

# --- !Downs

ALTER TABLE affiliation DROP COLUMN external_id;
ALTER TABLE affiliation_history DROP COLUMN external_id;

ALTER TABLE contracts DROP COLUMN external_id;
ALTER TABLE contracts_history DROP COLUMN external_id;

ALTER TABLE working_time_types DROP COLUMN external_id;
ALTER TABLE working_time_types_history DROP COLUMN external_id;
