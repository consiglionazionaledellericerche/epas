# ---!Ups
# -- Evoluzione per la modellazione dei badgeReaders.

# -- Eliminare vecchia implementazione badge_readers
ALTER TABLE stampings DROP CONSTRAINT fk785e8f148868391d;
DROP TABLE badge_readers;
DROP TABLE badge_readers_history;
DROP SEQUENCE IF EXISTS seq_badge_readers;

CREATE SEQUENCE seq_badge_readers
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE badge_readers (
    
    id BIGINT NOT NULL DEFAULT nextval('seq_badge_readers'::regclass),
    
    code TEXT NOT NULL,
    description TEXT, 
    location TEXT,
    enabled BOOLEAN NOT NULL,

    user_id BIGINT NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT code_unique UNIQUE(code)
);

CREATE TABLE badge_readers_history (

    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,
	
    code TEXT,
    description TEXT, 
    location TEXT,
    enabled BOOLEAN,

    user_id BIGINT,
    
    PRIMARY KEY (id, _revision)
);


CREATE SEQUENCE seq_badges
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE badges (
    
    id BIGINT NOT NULL DEFAULT nextval('seq_badges'::regclass),
    
    code TEXT,
    person_id BIGINT NOT NULL,
    badge_reader_id BIGINT NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (person_id) REFERENCES persons (id),
    FOREIGN KEY (badge_reader_id) REFERENCES badge_readers (id),
    CONSTRAINT badge_unique UNIQUE(code, badge_reader_id)
);

CREATE TABLE badges_history (

    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,
	
    code TEXT,
    person_id BIGINT, 
    badge_reader_id BIGINT,
    
    PRIMARY KEY (id, _revision)
);

# -- Gli utenti con ruolo 'badgeReader' diventano badge_readers.

INSERT INTO badge_readers (user_id, code, enabled) 
SELECT DISTINCT u.id AS user_id, u.username AS code, true AS enabled 
FROM roles r 
LEFT OUTER JOIN users_roles_offices uro ON r.id = uro.role_id 
LEFT OUTER JOIN users u ON u.id = uro.user_id 
WHERE r.name = 'badgeReader';

# -- Alle persone con badgenumber definito viene associato un badge 
# -- per ogni badge_reader associato all'office cui la persona appartiene.

INSERT INTO badges (code, person_id, badge_reader_id) 
SELECT p.badgenumber AS code, p.id AS person_id, br.id AS badge_reader_id 
FROM persons p 
LEFT OUTER JOIN office op ON op.id = p.office_id 
LEFT OUTER JOIN users_roles_offices uro ON uro.office_id = op.id 
LEFT OUTER JOIN roles r ON r.id = uro.role_id
LEFT OUTER JOIN badge_readers br ON uro.user_id = br.user_id 
WHERE p.badgenumber IS NOT null AND r.name = 'badgeReader';

# ---!Downs

DROP TABLE badges;
DROP TABLE badges_history;

DROP TABLE badge_readers;
DROP TABLE badge_readers_history;
DROP SEQUENCE IF EXISTS seq_badge_readers;
DROP SEQUENCE IF EXISTS seq_badges;

CREATE SEQUENCE seq_badge_readers
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE badge_readers (
    
    id BIGINT NOT NULL DEFAULT nextval('seq_badge_readers'::regclass),
    
    code TEXT NOT NULL,
    description TEXT NOT NULL, 
    location TEXT NOT NULL,
    enabled BOOLEAN NOT NULL,

    user_id BIGINT NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT code_unique UNIQUE(code)
);

CREATE TABLE badge_readers_history (

    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,
	
    code TEXT,
    description TEXT, 
    location TEXT,
    enabled BOOLEAN,

    user_id BIGINT,
    
    PRIMARY KEY (id, _revision)
);

ALTER TABLE stampings ADD CONSTRAINT fk785e8f148868391d FOREIGN KEY (badge_reader_id) REFERENCES badge_readers(id);