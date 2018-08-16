# ---!Ups

UPDATE absence_types SET code = '661MO' where code = '661M';
UPDATE absence_types_history SET code = '661MO' where code = '661M';


-- -------------------------------
-- JUSTIFIED BEHAVIOUR
-- -------------------------------

CREATE SEQUENCE seq_justified_behaviours
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE justified_behaviours (
    
    id BIGINT NOT NULL DEFAULT nextval('seq_justified_behaviours'::regclass),
    
    created_at timestamp without time zone,
    updated_at timestamp without time zone,

    name TEXT NOT NULL,
    
    version INTEGER,	
    
    PRIMARY KEY (id),
    CONSTRAINT justified_behaviours_name_unique UNIQUE(name)
);

CREATE TABLE justified_behaviours_history (

    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,
	
    name TEXT,
        
    PRIMARY KEY (id, _revision)
);

-- -------------------------------
-- ABSENCE TYPES JUSTIFIED BEHAVIOUR
-- -------------------------------
CREATE SEQUENCE seq_absence_types_justified_behaviours
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE absence_types_justified_behaviours (
    
    id BIGINT NOT NULL DEFAULT nextval('seq_absence_types_justified_behaviours'::regclass),
    
    created_at timestamp without time zone,
    updated_at timestamp without time zone,

    absence_type_id BIGINT NOT NULL,
    justified_behaviour_id BIGINT NOT NULL,
    data INTEGER,
    
    version INTEGER,	
    
    PRIMARY KEY (id)
);

CREATE TABLE absence_types_justified_behaviours_history (

    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,
	
    absence_type_id BIGINT,
    justified_behaviour_id BIGINT,
    data INTEGER,
        
    PRIMARY KEY (id, _revision)
);

-- -------------------------------
-- RELAZIONI
-- -------------------------------

ALTER TABLE absence_types_justified_behaviours ADD CONSTRAINT "absence_types_justified_behaviours_justified_behaviour_id_fkey" 
  FOREIGN KEY (justified_behaviour_id) REFERENCES justified_behaviours(id);

ALTER TABLE absence_types_justified_behaviours ADD CONSTRAINT "absence_types_justified_behaviours_absence_type_id_fkey" 
  FOREIGN KEY (absence_type_id) REFERENCES absence_types(id);


# ---!Downs

DROP TABLE absence_types_justified_behaviours;
DROP TABLE absence_types_justified_behaviours_history;
DROP SEQUENCE seq_absence_types_justified_behaviours;

DROP TABLE justified_behaviours;
DROP TABLE justified_behaviours_history;
DROP SEQUENCE seq_justified_behaviours;

UPDATE absence_types SET code = '661M' where code = '661MO';
UPDATE absence_types_history SET code = '661M' where code = '661MO';

