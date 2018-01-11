# ---!Ups

ALTER TABLE person_shift ADD COLUMN begin_date DATE;
ALTER TABLE person_shift ADD COLUMN end_date DATE;

# ---!Downs

ALTER TABLE person_shift DROP COLUMN begin_date;
ALTER TABLE person_shift DROP COLUMN end_date;