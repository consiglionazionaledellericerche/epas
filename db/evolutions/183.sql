# --- !Ups

CREATE TABLE personal_working_times(
	id BIGSERIAL PRIMARY KEY,
	description TEXT,
	contract_id BIGINT REFERENCES contracts(id),
	begin_date DATE,
	end_date DATE,
	working_time TEXT,
	version INT DEFAULT 0);
	
CREATE INDEX personal_working_times_contract_id_idx ON personal_working_times(contract_id);

CREATE TABLE personal_working_times_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
    description TEXT,
	begin_date DATE,
	end_date DATE,
	contract_id BIGINT,
	working_time TEXT
);

# --- !Downs

DROP TABLE personal_working_times_history;
DROP TABLE personal_working_times;
