# --- !Ups

CREATE TABLE telework_stampings(
    id BIGSERIAL PRIMARY KEY,
    date timestamp without time zone,
    note character varying(255),
    stamp_type TEXT,
    person_day_id bigint NOT NULL,
    FOREIGN KEY (person_day_id) REFERENCES person_days (id),
    version INT DEFAULT 0
);

CREATE INDEX telework_stampings_id ON telework_stampings(id);

CREATE TABLE telework_stampings_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
    date timestamp without time zone,
    note character varying(255),
    stamp_type TEXT,
    person_day_id bigint,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE INDEX telework_stampings_history_id ON telework_stampings_history(id);
CREATE INDEX telework_stampings_history_revision ON telework_stampings_history(_revision);

# --- !Downs

DROP INDEX telework_stampings_history_id;
DROP INDEX telework_stampings_history_revision;
DROP INDEX telework_stampings_id;

DROP TABLE telework_stampings_history;
DROP TABLE telework_stampings;