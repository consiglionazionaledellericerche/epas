# --- !Ups

ALTER TABLE general_setting ADD COLUMN enable_sso_for_attestati BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting_history ADD COLUMN enable_sso_for_attestati BOOLEAN DEFAULT FALSE;

# --- !Downs

ALTER TABLE general_setting DROP COLUMN enable_sso_for_attestati;
ALTER TABLE general_setting_history DROP COLUMN enable_sso_for_attestati;