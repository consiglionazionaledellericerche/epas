# --- !Ups

ALTER TABLE general_setting ADD COLUMN cookie_policy_enabled BOOLEAN default FALSE;
ALTER TABLE general_setting ADD COLUMN cookie_policy_content TEXT;
ALTER TABLE general_setting_history ADD COLUMN cookie_policy_enabled BOOLEAN;
ALTER TABLE general_setting_history ADD COLUMN cookie_policy_content TEXT;

# --- !Downs

ALTER TABLE general_setting DROP COLUMN cookie_policy_enabled;
ALTER TABLE general_setting_history DROP COLUMN cookie_policy_enabled;
ALTER TABLE general_setting DROP COLUMN cookie_policy_content;
ALTER TABLE general_setting_history DROP COLUMN cookie_policy_content;