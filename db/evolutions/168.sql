# --- !Ups

CREATE TABLE information_requests(
	id BIGSERIAL PRIMARY KEY,
	person_id BIGINT REFERENCES persons(id),
	start_at TIMESTAMP WITHOUT TIME ZONE,
	end_to TIMESTAMP WITHOUT TIME ZONE,
	information_type TEXT,
	office_head_approved TIMESTAMP WITHOUT TIME ZONE,
	office_head_approval_required BOOLEAN,
	administrative_approved TIMESTAMP WITHOUT TIME ZONE,
	administrative_approval_required BOOLEAN,
	flow_started BOOLEAN,
	flow_ended BOOLEAN,
	version INT DEFAULT 0
);

CREATE TABLE information_requests_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL DEFAULT 0,
	person_id BIGINT,
	start_at TIMESTAMP WITHOUT TIME ZONE,
	end_to TIMESTAMP WITHOUT TIME ZONE,
	information_type TEXT,
	office_head_approved TIMESTAMP WITHOUT TIME ZONE,
	office_head_approval_required BOOLEAN,
	administrative_approved TIMESTAMP WITHOUT TIME ZONE,
	administrative_approval_required BOOLEAN,
	flow_started BOOLEAN,
	flow_ended BOOLEAN,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE information_request_events(
	id BIGSERIAL PRIMARY KEY,
	information_request_id BIGINT REFERENCES information_requests(id),
	owner_id BIGINT REFERENCES users(id),
	description TEXT,
	event_type TEXT,
	created_at TIMESTAMP WITHOUT TIME ZONE,
	version INT DEFAULT 0
);

CREATE TABLE information_request_events_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL DEFAULT 0,
	information_request_id BIGINT,
	owner_id BIGINT REFERENCES users(id),
	description TEXT,
	event_type TEXT,
	created_at TIMESTAMP WITHOUT TIME ZONE,
	PRIMARY KEY (id, _revision, _revision_type)
);


CREATE TABLE telework_requests(
	information_request_id BIGINT NOT NULL REFERENCES information_requests (id),	
	context TEXT,
	year INTEGER NOT NULL,
	month INTEGER NOT NULL);	

CREATE TABLE telework_requests_history(
	information_request_id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL DEFAULT 0,
	context TEXT,
  	year INTEGER,
	month INTEGER,
	PRIMARY KEY (information_request_id, _revision, _revision_type)
);

CREATE TABLE illness_requests(
	information_request_id BIGINT NOT NULL REFERENCES information_requests (id),
	begin_date DATE NOT NULL,
	end_date DATE NOT NULL,
	name TEXT NOT NULL);	

CREATE TABLE illness_requests_history(
	information_request_id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL DEFAULT 0,
	begin_date DATE,
	end_date DATE,
	name TEXT,
	PRIMARY KEY (information_request_id, _revision, _revision_type)
);

CREATE TABLE service_requests(
	information_request_id BIGINT NOT NULL REFERENCES information_requests (id),
	day DATE NOT NULL,
	begin_at TIME WITHOUT TIME ZONE NOT NULL,
	finish_to TIME WITHOUT TIME ZONE NOT NULL,
	reason TEXT );
	

CREATE TABLE service_requests_history(
	information_request_id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL DEFAULT 0,
	day DATE,
	begin_at TIME WITHOUT TIME ZONE,
	finish_to TIME WITHOUT TIME ZONE,
	reason TEXT,
	PRIMARY KEY (information_request_id, _revision, _revision_type)
);


# --- !Downs

