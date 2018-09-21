# ---!Ups

CREATE TABLE absence_requests (
	id BIGSERIAL PRIMARY KEY,
	type TEXT,
	person_id BIGINT NOT NULL REFERENCES persons(id),
	start_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
	end_to TIMESTAMP WITHOUT TIME ZONE,
	note TEXT,
	attachment TEXT,
	manager_approved DATE,
	administrative_approved DATE,
	office_head_approved DATE,
	manager_approval_required BOOLEAN DEFAULT TRUE,
	administrative_approval_required BOOLEAN DEFAULT TRUE,
	office_head_approval_required BOOLEAN DEFAULT TRUE,
	flow_started BOOLEAN DEFAULT FALSE,
	flow_ended BOOLEAN DEFAULT FALSE,
	created_at TIMESTAMP WITHOUT TIME ZONE,
	updated_at TIMESTAMP WITHOUT TIME ZONE,
	version INT DEFAULT 0
);
	
CREATE TABLE absence_requests_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
  	type TEXT,
	person_id BIGINT,
	start_at TIMESTAMP WITHOUT TIME ZONE,
	end_to TIMESTAMP WITHOUT TIME ZONE,
	note TEXT,
	attachment TEXT,
	manager_approved DATE,
	administrative_approved DATE,
	office_head_approved DATE,
	manager_approval_required BOOLEAN,
	administrative_approval_required BOOLEAN,
	office_head_approval_required BOOLEAN,
	flow_started BOOLEAN,
	flow_ended BOOLEAN,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE absence_request_events (
	id BIGSERIAL PRIMARY KEY,
	absence_request_id BIGINT NOT NULL REFERENCES absence_requests(id),
	owner_id BIGINT NOT NULL REFERENCES users(id),
	description TEXT,	
	event_type TEXT,
	created_at TIMESTAMP WITHOUT TIME ZONE, 
	version INT DEFAULT 0
);

INSERT INTO roles (name, version) VALUES ('groupManager', 0); 

INSERT INTO roles_history (id, _revision, _revision_type, name) 
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, name  FROM roles WHERE name = 'groupManager';

INSERT INTO users_roles_offices (office_id, role_id, user_id, version)
SELECT office_id, (SELECT id FROM roles WHERE name = 'groupManager'), user_id, 0 
FROM persons WHERE is_person_in_charge = true;

INSERT INTO users_roles_offices_history (id, _revision, _revision_type, office_id, role_id, user_id)
SELECT uro.id, (SELECT MAX(rev) AS rev FROM revinfo), 0, uro.office_id, uro.role_id, uro.user_id FROM users_roles_offices uro
LEFT JOIN roles r ON r.id = uro.role_id WHERE r.name = 'groupManager'; 

ALTER TABLE persons_history DROP COLUMN is_person_in_charge;
ALTER TABLE persons DROP COLUMN is_person_in_charge;


# ---!Downs

DROP TABLE absence_request_events;
DROP TABLE absence_requests_history;
DROP TABLE absence_requests;

