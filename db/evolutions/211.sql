# --- !Ups

ALTER TABLE general_setting ADD COLUMN enable_sso_for_attestati BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting_history ADD COLUMN enable_sso_for_attestati BOOLEAN DEFAULT FALSE;

ALTER TABLE general_setting ADD COLUMN timeout_attestati INT DEFAULT 60;
ALTER TABLE general_setting_history ADD COLUMN timeout_attestati INT DEFAULT 60;

# --- !Downs

ALTER TABLE general_setting DROP COLUMN enable_sso_for_attestati;
ALTER TABLE general_setting_history DROP COLUMN enable_sso_for_attestati;

ALTER TABLE general_setting DROP COLUMN timeout_attestati;
ALTER TABLE general_setting_history DROP COLUMN timeout_attestati;

