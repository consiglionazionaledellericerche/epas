# --- !Ups

ALTER TABLE general_setting ADD COLUMN epas_service_enabled BOOLEAN DEFAULT false;
ALTER TABLE general_setting_history ADD COLUMN epas_service_enabled BOOLEAN;

ALTER TABLE general_setting ADD COLUMN epas_service_url TEXT DEFAULT false;
ALTER TABLE general_setting_history ADD COLUMN epas_service_url TEXT;

# --- !Downs

ALTER TABLE general_setting DROP COLUMN epas_service_enabled;
ALTER TABLE general_setting_history DROP COLUMN epas_service_enabled;

ALTER TABLE general_setting DROP COLUMN epas_service_url;
ALTER TABLE general_setting_history DROP COLUMN epas_service_url;