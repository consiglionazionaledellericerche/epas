# --- !Ups

ALTER TABLE general_setting ALTER COLUMN saturday_holiday_shift SET DEFAULT FALSE;
UPDATE general_setting SET saturday_holiday_shift = FALSE WHERE saturday_holiday_shift IS NULL;


# --- !Downs
