# ---!Ups

-- Storico per la contract_stamp_profiles

CREATE TABLE contract_stamp_profiles_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
	fixed_working_time BOOLEAN,
	begin_date DATE,
	contract_id BIGINT,	
	PRIMARY KEY (id, _revision, _revision_type)
);
	
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO contract_stamp_profiles_history (id, _revision, _revision_type, fixed_working_time, begin_date, contract_id)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, fixed_working_time, begin_date, contract_id FROM contract_stamp_profiles;

# ---!Downs

DROP TABLE contract_stamp_profiles_history;
