# --- !Ups

ALTER TABLE groups ADD COLUMN external_id TEXT;
ALTER TABLE groups_history ADD COLUMN external_id TEXT;

ALTER TABLE groups ADD COLUMN end_date DATE;
ALTER TABLE groups_history ADD COLUMN end_date DATE;

CREATE INDEX groups_office_id_idx ON groups(office_id);
CREATE UNIQUE INDEX groups_external_id_unique_idx ON groups(external_id, office_id);

CREATE TABLE affiliation (
  id BIGSERIAL PRIMARY KEY NOT NULL,
  group_id BIGINT NOT NULL REFERENCES groups(id),
  person_id BIGINT NOT NULL REFERENCES persons(id),
  begin_date DATE,
  end_date DATE,
  percentage NUMERIC(5,2) DEFAULT 100,
  version INT DEFAULT 0
);

CREATE INDEX affiliation_group_id_idx ON affiliation(group_id);
CREATE INDEX affiliation_person_id_idx ON affiliation(person_id);
CREATE INDEX affiliation_begin_date_idx ON affiliation(begin_date);
CREATE INDEX affiliation_end_date_idx ON affiliation(end_date);

CREATE TABLE affiliation_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
    group_id BIGINT,
    person_id BIGINT,
	begin_date DATE,
	end_date DATE,
	percentage NUMERIC(5,2),
	PRIMARY KEY (id, _revision, _revision_type)
);

INSERT INTO affiliation (group_id, person_id, begin_date, percentage)
  (SELECT groups_id AS group_id, people_id AS person_id, created_at, 100 AS begin_date 
	FROM groups_people gp JOIN groups g ON g.id = gp.groups_id);

ALTER TABLE affiliation ALTER COLUMN begin_date SET NOT NULL;

-- Creo una nuova revisione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO affiliation_history (id, _revision, _revision_type, group_id, person_id, begin_date, end_date, percentage)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, group_id, person_id, begin_date, end_date, percentage
    FROM affiliation;

DROP TABLE groups_people;
DROP TABLE groups_people_history;

# --- !Downs

ALTER TABLE groups DROP COLUMN external_id;
ALTER TABLE groups_history DROP COLUMN external_id;

ALTER TABLE groups DROP COLUMN end_date;
ALTER TABLE groups_history DROP COLUMN end_date;

CREATE TABLE groups_people (
  groups_id BIGINT NOT NULL REFERENCES groups(id),
  people_id BIGINT NOT NULL REFERENCES persons(id)
);

CREATE TABLE groups_people_history (
  groups_id BIGINT,
  people_id BIGINT,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT NOT NULL,
  PRIMARY KEY (groups_id, people_id, _revision, _revision_type)  
);

INSERT INTO groups_people (groups_id, people_id)
  SELECT group_id AS groups_id, person_id AS people_id FROM affiliation;

-- Creo una nuova revisione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO groups_people_history (_revision, _revision_type, groups_id, people_id)
  SELECT (SELECT MAX(rev) AS rev FROM revinfo), 0, groups_id, people_id
    FROM groups_people;
 
DROP TABLE affiliation_history;
DROP TABLE affiliation;
