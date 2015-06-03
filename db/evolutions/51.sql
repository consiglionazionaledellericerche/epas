# ---!Ups

ALTER TABLE person_days
  ADD COLUMN is_holiday boolean;
ALTER TABLE person_days_history
  ADD COLUMN is_holiday boolean;

UPDATE person_days SET is_holiday = false;
UPDATE person_days_history SET is_holiday = false;

# ---!Downs

ALTER TABLE person_days DROP COLUMN is_holiday;
ALTER TABLE person_days_history DROP COLUMN is_holiday;