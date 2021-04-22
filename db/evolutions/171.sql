# --- !Ups

ALTER TABLE general_setting ADD COLUMN regulations_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting_history ADD COLUMN regulations_enabled BOOLEAN;

# --- !Downs

ALTER TABLE general_setting DROP COLUMN regulations_enabled;
ALTER TABLE general_setting_history DROP COLUMN regulations_enabled;