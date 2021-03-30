# --- !Ups

CREATE TABLE telework_validations(
	id BIGSERIAL PRIMARY KEY,
	person_id BIGINT REFERENCES persons(id),
	year INTEGER,
	month INTEGER,
	approved BOOLEAN,
	approvation_date DATE,
	version INT DEFAULT 0
);

CREATE TABLE telework_validations_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
	person_id BIGINT,
	year INTEGER,
	month INTEGER,
	approved BOOLEAN,
	approvation_date DATE,
	PRIMARY KEY (id, _revision, _revision_type)
);

ALTER TABLE general_setting ADD COLUMN enable_illness_flow BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting_history ADD COLUMN enable_illness_flow BOOLEAN DEFAULT FALSE;

# --- !Downs

DROP TABLE telework_validations_history;
DROP TABLE telework_validations;

ALTER TABLE general_setting DROP COLUMN enable_illness_flow;
ALTER TABLE general_setting_history DROP COLUMN enable_illness_flow;