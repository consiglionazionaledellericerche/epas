# --- !Ups

ALTER TABLE general_setting ADD COLUMN person_creation_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE general_setting_history ADD COLUMN person_creation_enabled BOOLEAN DEFAULT TRUE;

# --- !Downs

ALTER TABLE general_setting DROP COLUMN person_creation_enabled;
ALTER TABLE general_setting_history DROP COLUMN person_creation_enabled;