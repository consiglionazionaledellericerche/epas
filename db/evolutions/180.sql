# --- !Ups

ALTER TABLE general_setting ADD COLUMN enable_autoconfig_covid19 BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting_history ADD COLUMN enable_autoconfig_covid19 BOOLEAN DEFAULT FALSE;

# --- !Downs

ALTER TABLE general_setting DROP COLUMN enable_autoconfig_covid19;
ALTER TABLE general_setting_history DROP COLUMN enable_autoconfig_covid19;