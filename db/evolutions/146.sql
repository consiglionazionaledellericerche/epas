# --- !Ups

ALTER TABLE general_setting ADD COLUMN saturday_holiday_shift BOOLEAN;
ALTER TABLE general_setting_history ADD COLUMN saturday_holiday_shift BOOLEAN;

# --- !Downs

ALTER TABLE general_setting DROP COLUMN saturday_holiday_shift;
ALTER TABLE general_setting_history DROP COLUMN saturday_holiday_shift;
