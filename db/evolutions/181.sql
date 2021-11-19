# ---!Ups

ALTER TABLE absence_types ADD COLUMN to_update BOOLEAN default TRUE;
ALTER TABLE absence_types_history ADD COLUMN to_update BOOLEAN;

ALTER TABLE general_setting ADD COLUMN enable_autoconfig_smartworking BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting_history ADD COLUMN enable_autoconfig_smartworking BOOLEAN DEFAULT FALSE;

# ---!Downs

ALTER TABLE absence_types_history DROP COLUMN to_update;
ALTER TABLE absence_types DROP COLUMN to_update;

ALTER TABLE general_setting DROP COLUMN enable_autoconfig_smartworking;
ALTER TABLE general_setting_history DROP COLUMN enable_autoconfig_smartworking;