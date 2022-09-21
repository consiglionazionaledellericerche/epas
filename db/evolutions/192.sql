# ---!Ups

ALTER TABLE general_setting ADD COLUMN enable_absence_top_level_authorization BOOLEAN DEFAULT TRUE;
ALTER TABLE general_setting_history ADD COLUMN enable_absence_top_level_authorization BOOLEAN;

# ---!Downs

ALTER TABLE general_setting DROP COLUMN enable_absence_top_level_authorization;
ALTER TABLE general_setting_history DROP enable_absence_top_level_authorization;
