# --- !Ups

ALTER TABLE general_setting ADD COLUMN enable_unique_daily_shift BOOLEAN DEFAULT TRUE;
ALTER TABLE general_setting_history ADD COLUMN enable_unique_daily_shift BOOLEAN DEFAULT TRUE;

# --- !Downs

ALTER TABLE general_setting DROP COLUMN enable_unique_daily_shift;
ALTER TABLE general_setting_history DROP COLUMN enable_unique_daily_shift;