# ---!Ups

ALTER TABLE person_days ADD COLUMN decurted INTEGER;
ALTER TABLE person_days_history ADD COLUMN decurted INTEGER;

# ---!Downs

ALTER TABLE person_days DROP COLUMN decurted;
ALTER TABLE person_days_history DROP COLUMN decurted;

