# --- !Ups

CREATE TABLE monthly_competence_type(
	id BIGSERIAL PRIMARY KEY,
	name TEXT,
	workdays_code BIGINT REFERENCES competence_codes(id) NOT NULL,
	holidays_code BIGINT REFERENCES competence_codes(id) NOT NULL,
	version INT DEFAULT 0);
	

CREATE TABLE monthly_competence_type_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
  	name TEXT,
	workdays_code BIGINT,
	holidays_code BIGINT,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE INDEX monthly_competence_type_history_id ON monthly_competence_type_history(id);

ALTER TABLE person_reperibility_types ADD COLUMN monthly_competence_type_id BIGINT;
ALTER TABLE person_reperibility_types_history ADD COLUMN monthly_competence_type_id BIGINT;
ALTER TABLE person_reperibility_types ADD FOREIGN KEY (monthly_competence_type_id) REFERENCES monthly_competence_type(id);

INSERT INTO 
    monthly_competence_type (name, workdays_code, holidays_code, version) 
SELECT
    'Reperibilità', cc1.id, cc2.id, 0
FROM
    competence_codes cc1, competence_codes cc2
WHERE
    cc1.code = '207' and cc2.code = '208';

-- Creo una nuova revisione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO monthly_competence_type_history (id, _revision, _revision_type, name, workdays_code, holidays_code)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, name, workdays_code, holidays_code
FROM monthly_competence_type;
    
    
UPDATE person_reperibility_types 
SET monthly_competence_type_id = ( SELECT m.id from monthly_competence_type m);

INSERT INTO person_reperibility_types_history (id, _revision, _revision_type, description, supervisor_id, disabled, monthly_competence_type_id) 
     SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, description, supervisor_id, disabled, monthly_competence_type_id 
     FROM person_reperibility_types;


# --- !Downs

DROP TABLE monthly_competence_type_history;
DROP TABLE monthly_competence_type;
