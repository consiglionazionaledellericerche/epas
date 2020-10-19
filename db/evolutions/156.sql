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

# --- !Downs

DROP TABLE monthly_competence_type_history;
DROP TABLE monthly_competence_type;
