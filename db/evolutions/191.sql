# ---!Ups

ALTER TABLE general_setting ADD COLUMN holiday_shift_in_night_too BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting_history ADD COLUMN holiday_shift_in_night_too BOOLEAN DEFAULT FALSE;

# ---!Downs

ALTER TABLE general_setting DROP COLUMN holiday_shift_in_night_too;
ALTER TABLE general_setting_history DROP COLUMN holiday_shift_in_night_too;