# --- !Ups

ALTER TABLE general_setting ALTER COLUMN saturday_holiday_shift SET DEFAULT TRUE;
UPDATE general_setting SET saturday_holiday_shift = TRUE WHERE saturday_holiday_shift IS NULL;


# --- !Downs
