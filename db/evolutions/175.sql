# --- !Ups

CREATE TABLE check_green_pass (
	id BIGSERIAL PRIMARY KEY,
	person_id BIGINT REFERENCES persons(id),
	check_date DATE,
	checked BOOLEAN,
	version INT DEFAULT 0);
	
CREATE TABLE check_green_pass_history (
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL DEFAULT 0,
	person_id BIGINT,
	check_date DATE,
	checked BOOLEAN,
	PRIMARY KEY (id, _revision, _revision_type));

# --- !Downs

DROP TABLE check_green_pass_history;
DROP TABLE check_green_pass;