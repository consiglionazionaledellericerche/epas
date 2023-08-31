# --- !Ups

ALTER TABLE general_setting ADD COLUMN epas_service_enabled BOOLEAN DEFAULT false;
ALTER TABLE general_setting_history ADD COLUMN epas_service_enabled BOOLEAN;

ALTER TABLE general_setting ADD COLUMN epas_service_url TEXT DEFAULT 'http://epas-service:8080/';
ALTER TABLE general_setting_history ADD COLUMN epas_service_url TEXT;

ALTER TABLE general_setting ADD COLUMN epas_helpdesk_service_enabled BOOLEAN DEFAULT false;
ALTER TABLE general_setting_history ADD COLUMN epas_helpdesk_service_enabled BOOLEAN;

ALTER TABLE general_setting ADD COLUMN epas_helpdesk_service_url TEXT DEFAULT 'http://epas-helpdesk-service:8080/';
ALTER TABLE general_setting_history ADD COLUMN epas_helpdesk_service_url TEXT;

CREATE TABLE jwt_tokens (
	id BIGSERIAL PRIMARY KEY,
	id_token TEXT,
	access_token TEXT,
	refresh_token TEXT,
	token_type TEXT,
	expires_in INT,
	scope TEXT,
	taken_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
	expires_at TIMESTAMP WITHOUT TIME ZONE,
	created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
	updated_at TIMESTAMP WITHOUT TIME ZONE,
	version INT DEFAULT 0
);

CREATE INDEX id_token_jwt_tokens_idx ON jwt_tokens(id_token);
CREATE INDEX created_at_jwt_tokens_idx ON jwt_tokens(created_at);

# --- !Downs

ALTER TABLE general_setting DROP COLUMN epas_service_enabled;
ALTER TABLE general_setting_history DROP COLUMN epas_service_enabled;

ALTER TABLE general_setting DROP COLUMN epas_service_url;
ALTER TABLE general_setting_history DROP COLUMN epas_service_url;

ALTER TABLE general_setting DROP COLUMN epas_helpdesk_service_enabled;
ALTER TABLE general_setting_history DROP COLUMN epas_helpdesk_service_enabled;

ALTER TABLE general_setting DROP COLUMN epas_helpdesk_service_url;
ALTER TABLE general_setting_history DROP COLUMN epas_helpdesk_service_url;

DROP TABLE jwt_tokens;