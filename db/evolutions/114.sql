# ---!Ups

ALTER TABLE absences ADD COLUMN expire_recover_date DATE;
ALTER TABLE absences ADD COLUMN time_to_recover INT;

ALTER TABLE absences_history ADD COLUMN expire_recover_date DATE;
ALTER TABLE absences_history ADD COLUMN time_to_recover INT;
UPDATE absences SET time_to_recover = 0;

CREATE TABLE time_variations (
	id BIGSERIAL PRIMARY KEY,
	date_variation DATE,
	time_variation INT,
	absence_id BIGINT NOT NULL REFERENCES absences(id),
	version INT DEFAULT 0
);

CREATE TABLE time_variations_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
	date_variation DATE,
	time_variation INT,
	absence_id BIGINT NOT NULL REFERENCES absences(id),
	PRIMARY KEY (id, _revision, _revision_type)
);

# ---!Downs

ALTER TABLE absences_history DROP COLUMN expire_recover_date;
ALTER TABLE absences_history DROP COLUMN time_to_recover;

ALTER TABLE absences DROP COLUMN expire_recover_date;
ALTER TABLE absences DROP COLUMN time_to_recover;

DROP TABLE time_variations_history;
DROP TABLE time_variations;

