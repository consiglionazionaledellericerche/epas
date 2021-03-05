# --- !Ups

CREATE TABLE telework_request(
	id BIGSERIAL PRIMARY KEY,
	person_id BIGINT NOT NULL,
	information_type TEXT,
	office_head_approved TIMESTAMP WITHOUT TIME ZONE,
	context TEXT,
	year INTEGER,
	month INTEGER,
	version INT DEFAULT 0);
	

CREATE TABLE telework_request_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
  	name TEXT,
	workdays_code BIGINT,
	holidays_code BIGINT,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE illness_request(
	id BIGSERIAL PRIMARY KEY,
	name TEXT,
	workdays_code BIGINT REFERENCES competence_codes(id) NOT NULL,
	holidays_code BIGINT REFERENCES competence_codes(id) NOT NULL,
	version INT DEFAULT 0);
	

CREATE TABLE illness_request_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
  	name TEXT,
	workdays_code BIGINT,
	holidays_code BIGINT,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE service_request(
	id BIGSERIAL PRIMARY KEY,
	name TEXT,
	workdays_code BIGINT REFERENCES competence_codes(id) NOT NULL,
	holidays_code BIGINT REFERENCES competence_codes(id) NOT NULL,
	version INT DEFAULT 0);
	

CREATE TABLE service_request_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
  	name TEXT,
	workdays_code BIGINT,
	holidays_code BIGINT,
	PRIMARY KEY (id, _revision, _revision_type)
);

