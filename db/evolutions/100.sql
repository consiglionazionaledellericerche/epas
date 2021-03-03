# ---!Ups

CREATE TABLE zones (
	id BIGSERIAL PRIMARY KEY,
	name TEXT NOT NULL,
	description TEXT,
	badge_reader_id BIGINT NOT NULL REFERENCES badge_readers(id),
	CONSTRAINT name_zone_unique UNIQUE(name)
	);
	
CREATE TABLE zones_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
	name TEXT NOT NULL,
	description TEXT,
	badge_reader_id BIGINT NOT NULL REFERENCES badge_readers(id),
	PRIMARY KEY (id, _revision, _revision_type)
	);
	
CREATE TABLE zone_to_zones (
	id BIGSERIAL PRIMARY KEY,
	zone_base_id BIGINT NOT NULL REFERENCES zones (id),
	zone_linked_id BIGINT NOT NULL REFERENCES zones (id),
	delay int
);

CREATE TABLE zone_to_zones_history (
	id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
    zone_base_id BIGINT,
    zone_linked_id BIGINT,
    delay int,
    
    PRIMARY KEY (id, _revision, _revision_type)
);

ALTER TABLE stampings ADD COLUMN stamping_zone TEXT;
ALTER TABLE stampings_history ADD COLUMN stamping_zone TEXT;

ALTER TABLE person_days ADD COLUMN justified_time_between_zones INTEGER;
ALTER TABLE person_days_history ADD COLUMN justified_time_between_zones INTEGER;

# ---!Downs

ALTER TABLE zones DROP CONSTRAINT name_zone_unique;
DROP TABLE zone_to_zones;
DROP TABLE zones;
DROP TABLE zones_history;
DROP TABLE zone_to_zones_history;
ALTER TABLE stampings_history DROP COLUMN stamping_zone;
ALTER TABLE stmpings DROP COLUMN stamping_zone;
ALTER TABLE person_days_history DROP COLUMN justified_time_between_zones;
ALTER TABLE person_days DROP COLUMN justified_time_between_zones;