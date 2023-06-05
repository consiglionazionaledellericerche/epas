# --- !Ups

INSERT INTO roles (name) VALUES ('absenceManager');

-- Creo una nouva reviosione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO roles_history (id, _revision, _revision_type, name) 
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, name  FROM roles WHERE name = 'absenceManager';

# --- !Downs

DELETE FROM roles WHERE name = 'absenceManager';
