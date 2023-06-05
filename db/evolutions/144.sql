# --- !Ups

CREATE TABLE time_slots(
	id BIGSERIAL PRIMARY KEY,
	description TEXT,
	office_id BIGINT REFERENCES office(id),
	begin_slot VARCHAR(64) NOT NULL,
	end_slot VARCHAR(64) NOT NULL,
	disabled BOOLEAN DEFAULT FALSE,
	created_at TIMESTAMP WITHOUT TIME ZONE,
	updated_at TIMESTAMP WITHOUT TIME ZONE,
	version INT DEFAULT 0);
	
CREATE INDEX time_slot_office_id_idx ON time_slots(office_id);

CREATE TABLE time_slots_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
    description TEXT,
	office_id BIGINT,
	begin_slot VARCHAR(64),
	end_slot VARCHAR(64),
	disabled BOOLEAN,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE contract_mandatory_time_slots(
	id BIGSERIAL PRIMARY KEY,
	begin_date DATE,
	end_date DATE,
	contract_id BIGINT REFERENCES contracts(id),
	time_slot_id BIGINT REFERENCES time_slots(id),
	created_at TIMESTAMP WITHOUT TIME ZONE,
	updated_at TIMESTAMP WITHOUT TIME ZONE,
	version INT DEFAULT 0
); 

CREATE INDEX contract_mandatory_time_slots_contract_id_idx ON contract_mandatory_time_slots(contract_id);
CREATE INDEX contract_mandatory_time_slots_time_slot_id_idx ON contract_mandatory_time_slots(time_slot_id);

CREATE TABLE contract_mandatory_time_slots_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
	begin_date DATE,
	end_date DATE,
	contract_id BIGINT,
	time_slot_id BIGINT
);

# --- !Downs

DROP TABLE contract_mandatory_time_slots_history;
DROP TABLE contract_mandatory_time_slots;
DROP TABLE time_slots_history;
DROP TABLE time_slots;
