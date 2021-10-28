# --- !Ups

INSERT INTO roles (name, version) VALUES ('badgeManager', 0);

-- Creo una nuova reviosione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO roles_history (id, _revision, _revision_type, name) 
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, name  FROM roles WHERE name = 'badgeManager';

# --- !Downs

-- Creo una nuova revisione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO roles_history (id, _revision, _revision_type, name) 
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 2, name  FROM roles WHERE name = 'badgeManager';

DELETE FROM roles WHERE name = 'badgeManager';
