# ---!Ups

ALTER TABLE person_days DROP COLUMN IF EXISTS time_justified;

# ---!Downs

ALTER TABLE person_days ADD COLUMN time_justified integer;