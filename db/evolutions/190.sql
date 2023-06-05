# --- !Ups

ALTER TABLE general_setting ADD COLUMN enable_daily_presence_for_manager BOOLEAN DEFAULT TRUE;
ALTER TABLE general_setting_history ADD COLUMN enable_daily_presence_for_manager BOOLEAN;

# --- !Downs

ALTER TABLE general_setting_history DROP COLUMN enable_daily_presence_for_manager;
ALTER TABLE general_setting DROP COLUMN enable_daily_presence_for_manager;