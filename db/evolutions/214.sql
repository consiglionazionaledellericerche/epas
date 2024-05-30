# --- !Ups
ALTER TABLE shift_time_table RENAME COLUMN paid_minutes TO paid_minutes_morning;
ALTER TABLE shift_time_table ADD COLUMN paid_minutes_afternoon INTEGER;
UPDATE shift_time_table SET paid_minutes_afternoon = paid_minutes_morning;
UPDATE shift_time_table SET paid_minutes_afternoon = 360 WHERE start_morning = '07:00:00' and end_morning = '14:00:00';

# --- !Downs
-- Non è necessaria una down