# ---!Ups

ALTER TABLE person_shifts ADD COLUMN begin_date DATE;
ALTER TABLE person_shifts ADD COLUMN end_date DATE;
# ---!Downs

ALTER TABLE person_shifts DROP COLUMN begin_date;
ALTER TABLE person_shifts DROP COLUMN end_date;