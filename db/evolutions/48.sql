# ---!Ups

ALTER TABLE person_days
  ADD COLUMN accepted_holiday_working_time boolean;
ALTER TABLE person_days_history
  ADD COLUMN accepted_holiday_working_time boolean;

UPDATE person_days SET accepted_holiday_working_time = false;
UPDATE person_days_history SET accepted_holiday_working_time = false;

# ---!Downs

ALTER TABLE person_days DROP COLUMN accepted_holiday_working_time;
ALTER TABLE person_days_history DROP COLUMN accepted_holiday_working_time;