# ---!Ups

CREATE SEQUENCE seq_badge_systems
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE badge_systems (
    
    id BIGINT NOT NULL DEFAULT nextval('seq_badge_systems'::regclass),
    
    name TEXT NOT NULL,
    description TEXT, 
    enabled BOOLEAN NOT NULL,

    office_owner_id BIGINT NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (office_owner_id) REFERENCES office (id),
    CONSTRAINT name_unique UNIQUE(name)
);

CREATE TABLE badge_systems_history (

    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,
	
    name TEXT,
    description TEXT, 
    enabled BOOLEAN,

    office_owner_id BIGINT,
    
    PRIMARY KEY (id, _revision)
);

CREATE TABLE badge_readers_badge_systems (
	badgereaders_id BIGINT NOT NULL,
	badgesystems_id BIGINT NOT NULL,
	FOREIGN KEY (badgereaders_id) REFERENCES badge_readers (id),
	FOREIGN KEY (badgesystems_id) REFERENCES badge_systems (id)
);

CREATE TABLE badge_readers_badge_systems_history (

    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,

    badgereaders_id BIGINT,
    badgesystems_id BIGINT,
    
    PRIMARY KEY (id, _revision)
);

ALTER TABLE badges ADD COLUMN badge_system_id BIGINT;
ALTER TABLE badges ADD FOREIGN KEY (badge_system_id) REFERENCES badge_systems (id);
ALTER TABLE badges_history ADD COLUMN badge_system_id BIGINT;

CREATE TABLE badge_systems_office (
	badgesystems_id BIGINT NOT NULL,
	offices_id BIGINT NOT NULL,
	FOREIGN KEY (badgesystems_id) REFERENCES badge_systems (id),
	FOREIGN KEY (offices_id) REFERENCES office (id)
);

CREATE TABLE badge_systems_office_history (

    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,

    badgesystems_id BIGINT,
    offices_id BIGINT,
    
    PRIMARY KEY (id, _revision)
);

# ---!Downs

DROP TABLE badge_systems_office;
DROP TABLE badge_systems_office_history;


ALTER TABLE badges_history DROP COLUMN badge_system_id;
ALTER TABLE badges DROP CONSTRAINT badges_badge_system_id_fkey;
ALTER TABLE badges DROP COLUMN badge_system_id;

DROP TABLE badge_readers_badge_systems;
DROP TABLE badge_readers_badge_systems_history;

DROP TABLE badge_systems;
DROP TABLE badge_systems_history;
DROP SEQUENCE IF EXISTS seq_badge_systems;







