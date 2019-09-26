# --- !Ups

-- Creo una nouva revisione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO roles (name, version) VALUES ('personDayReader', 0);

INSERT INTO roles_history (id, _revision, _revision_type, name) 
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, name  FROM roles WHERE name = 'personDayReader';

# ---!Downs
--Non è necessaria una down