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

# ---!Downs

DROP CONSTRAINT name_unique;
DROP TABLE zone_to_zones;
DROP TABLE zones;
DROP TABLE zones_history;
DROP TABLE zone_to_zones_history;
