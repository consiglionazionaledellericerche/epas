# --- !Ups

CREATE TABLE telework_request(
	id BIGSERIAL PRIMARY KEY,
	person_id BIGINT NOT NULL,
	information_type TEXT,
	office_head_approved TIMESTAMP WITHOUT TIME ZONE,
	office_head_approval_required BOOLEAN,
	context TEXT,
	year INTEGER,
	month INTEGER,
	version INT DEFAULT 0);
	

CREATE TABLE telework_request_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
  	person_id BIGINT,
	information_type TEXT,
	context TEXT,
  	year INTEGER,
	month INTEGER,
	office_head_approved TIMESTAMP WITHOUT TIME ZONE,
	office_head_approval_required BOOLEAN,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE illness_request(
	id BIGSERIAL PRIMARY KEY,
	person_id BIGINT NOT NULL,
	information_type TEXT,
	office_head_approved TIMESTAMP WITHOUT TIME ZONE,
	office_head_approval_required BOOLEAN,
	begin_date DATE,
	end_date DATE,
	name TEXT,
	version INT DEFAULT 0);
	

CREATE TABLE illness_request_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
	person_id BIGINT,
	information_type TEXT,
	office_head_approved TIMESTAMP WITHOUT TIME ZONE,
	office_head_approval_required BOOLEAN,
	begin_date DATE,
	end_date DATE,
	name TEXT,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE service_request(
	id BIGSERIAL PRIMARY KEY,
	person_id BIGINT NOT NULL,
	information_type TEXT,
	office_head_approved TIMESTAMP WITHOUT TIME ZONE,
	office_head_approval_required BOOLEAN,
	day DATE,
	start_at TIMESTAMP WITHOUT TIME ZONE,
	end_to TIMESTAMP WITHOUT TIME ZONE,
	reason TEXT,
	version INT DEFAULT 0);
	

CREATE TABLE service_request_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
	person_id BIGINT,
	information_type TEXT,
	office_head_approved TIMESTAMP WITHOUT TIME ZONE,
	office_head_approval_required BOOLEAN,
	day DATE,
	start_at TIMESTAMP WITHOUT TIME ZONE,
	end_to TIMESTAMP WITHOUT TIME ZONE,
	reason TEXT,
	PRIMARY KEY (id, _revision, _revision_type)
);

