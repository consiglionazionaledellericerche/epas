# ---!Ups

CREATE TABLE groups (
	id BIGSERIAL PRIMARY KEY,
	name TEXT,
	description TEXT,
	send_flows_email BOOLEAN DEFAULT FALSE,
	manager BIGINT NOT NULL REFERENCES persons(id),
	created_at TIMESTAMP WITHOUT TIME ZONE,
	updated_at TIMESTAMP WITHOUT TIME ZONE,
	version INT DEFAULT 0
);

CREATE TABLE groups_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
	name TEXT,
	description TEXT,
	send_flows_email BOOLEAN,
	manager BIGINT,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE groups_persons (
	groups_id BIGINT NOT NULL,
	people_id BIGINT NOT NULL,
	FOREIGN KEY (groups_id) REFERENCES groups (id),
	FOREIGN KEY (people_id) REFERENCES persons (id)
);

CREATE INDEX groups_persons_groups_id_idx ON groups_persons(groups_id);
CREATE INDEX groups_persons_people_id_idx ON groups_persons(people_id);

CREATE TABLE groups_persons_history (	
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
    groups_id BIGINT,
    people_id BIGINT,    
    PRIMARY KEY (_revision, _revision_type, groups_id, people_id)
);



# ---!Downs

DROP INDEX groups_persons_groups_id_idx;
DROP INDEX groups_persons_people_id_idx;

DROP TABLE groups_persons_history;
DROP TABLE groups_persons;
DROP TABLE groups_history;
DROP TABLE groups;
