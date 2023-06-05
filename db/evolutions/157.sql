# ---!Ups

create table competence_requests (
	id BIGSERIAL PRIMARY KEY,
	type TEXT,
	person_id BIGINT NOT NULL REFERENCES persons(id),
	team_mate_id BIGINT REFERENCES persons(id),
	value INTEGER,
	year INTEGER,
	month INTEGER,
	begin_date_to_ask DATE,
	end_date_to_ask DATE,
	begin_date_to_give DATE,
	end_date_to_give DATE,
	shift_slot TEXT,
	start_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
	end_to TIMESTAMP WITHOUT TIME ZONE,
	note TEXT,	
	employee_approved TIMESTAMP,
	reperibility_manager_approved TIMESTAMP,
	employee_approval_required BOOLEAN DEFAULT FALSE,
	reperibility_manager_approval_required BOOLEAN DEFAULT TRUE,
	flow_started BOOLEAN DEFAULT FALSE,
	flow_ended BOOLEAN DEFAULT FALSE,
	created_at TIMESTAMP WITHOUT TIME ZONE,
	updated_at TIMESTAMP WITHOUT TIME ZONE,
	version INT DEFAULT 0
);
CREATE INDEX competence_requests_person_id_idx ON competence_requests (person_id);

CREATE TABLE competence_requests_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
  	type TEXT,
	person_id BIGINT,
	team_mate_id BIGINT,
	value INTEGER,
	year INTEGER,
	month INTEGER,
	begin_date_to_ask DATE,
	end_date_to_ask DATE,
	begin_date_to_give DATE,
	end_date_to_give DATE,
	shift_slot TEXT,
	start_at TIMESTAMP WITHOUT TIME ZONE,
	end_to TIMESTAMP WITHOUT TIME ZONE,
	note TEXT,	
	employee_approved TIMESTAMP,
	reperibility_manager_approved TIMESTAMP,
	employee_approval_required BOOLEAN,
	reperibility_manager_approval_required BOOLEAN,
	flow_started BOOLEAN,
	flow_ended BOOLEAN,
	PRIMARY KEY (id, _revision, _revision_type)
);
create table competence_request_events (
	id BIGSERIAL PRIMARY KEY,
	competence_request_id BIGINT NOT NULL REFERENCES competence_requests(id),
	owner_id BIGINT NOT NULL REFERENCES users(id),
	description TEXT,	
	event_type TEXT,
	created_at TIMESTAMP WITHOUT TIME ZONE, 
	version INT DEFAULT 0
);

CREATE INDEX competence_requests_events_competence_request_id_idx ON competence_request_events(competence_request_id);
CREATE INDEX competence_requests_events_owner_id_idx ON competence_request_events(owner_id);

# ---!Downs

DROP INDEX competence_requests_person_id_idx;
DROP INDEX competence_requests_events_competence_request_id_idx;
DROP INDEX competence_requests_events_owner_id_idx;

DROP TABLE competence_request_events;
DROP TABLE competence_requests_history;
DROP TABLE competence_requests;