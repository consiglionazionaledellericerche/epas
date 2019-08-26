# --- !Ups

CREATE TABLE general_setting (
	id BIGSERIAL PRIMARY KEY,
	sync_badges_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  	sync_offices_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  	sync_persons_enabled BOOLEAN NOT NULL DEFAULT FALSE,
	created_at TIMESTAMP WITHOUT TIME ZONE,
	updated_at TIMESTAMP WITHOUT TIME ZONE,
	version INT DEFAULT 0
);

CREATE TABLE general_setting_history (
	id BIGINT NOT NULL,
	sync_badges_enabled BOOLEAN,
  	sync_offices_enabled BOOLEAN,
  	sync_persons_enabled BOOLEAN,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
	PRIMARY KEY (id, _revision, _revision_type)
);

# --- !Downs

DROP TABLE general_setting_history;
DROP TABLE general_setting;