# ---!Ups

CREATE TABLE competences_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
	person_id BIGINT NOT NULL,
	competence_code_id BIGINT NOT NULL,
	year INT,
	month INT,
	valuerequested NUMERIC (5,2),
	exceeded_mins INT,
	valueapproved INT,
	reason TEXT,
  	PRIMARY KEY (id, _revision, _revision_type)
	);

INSERT INTO competences_history (id, _revision, _revision_type, person_id, competence_code_id, year, month, valuerequested,
exceeded_mins, valueapproved, reason)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, person_id, competence_code_id, year, month, valuerequested, exceeded_mins,
valueapproved, reason FROM competences;

CREATE TABLE contracts_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
  	begin_date DATE,
  	end_date DATE,
  	end_contract DATE,
  	oncertificate BOOLEAN,
  	person_id BIGINT,
  	source_date_residual DATE,
  	source_permission_used INTEGER,
  	source_recovery_day_used INTEGER,
  	source_remaining_minutes_current_year INTEGER,
  	source_remaining_minutes_last_year INTEGER,
  	source_vacation_current_year_used INTEGER,
  	source_vacation_last_year_used INTEGER,
  	source_remaining_meal_ticket INTEGER,
  	source_by_admin BOOLEAN,
  	source_date_meal_ticket DATE,
  	perseo_id BIGINT,
  	is_temporary BOOLEAN,
  	source_date_vacation DATE,
  	PRIMARY KEY (id, _revision, _revision_type)
	);

INSERT INTO contracts_history (id, _revision, _revision_type, begin_date, end_date, end_contract, oncertificate, person_id,
source_date_residual, source_permission_used, source_recovery_day_used, source_remaining_minutes_current_year,
source_remaining_minutes_last_year, source_vacation_current_year_used, source_vacation_last_year_used, source_remaining_meal_ticket,
source_by_admin, source_date_meal_ticket, perseo_id, is_temporary, source_date_vacation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, begin_date, end_date, end_contract, oncertificate, person_id,
source_date_residual, source_permission_used, source_recovery_day_used, source_remaining_minutes_current_year,
source_remaining_minutes_last_year, source_vacation_current_year_used, source_vacation_last_year_used, source_remaining_meal_ticket,
source_by_admin, source_date_meal_ticket, perseo_id, is_temporary, source_date_vacation FROM contracts;

DELETE FROM vacation_periods_history;

INSERT INTO vacation_periods_history (id, _revision, _revision_type, begin_date, end_date, contract_id, vacation_code)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, begin_date, end_date, contract_id, vacation_code
FROM vacation_periods;


# ---!Downs

DROP TABLE competences_history;
DROP TABLE contracts_history;


