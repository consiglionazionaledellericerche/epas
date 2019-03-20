# --- !Ups

-- Creo una nouva revisione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

ALTER TABLE groups ADD COLUMN office_id BIGINT;
ALTER TABLE groups ADD FOREIGN KEY (office_id) REFERENCES office(id);
ALTER TABLE groups_history ADD COLUMN office_id BIGINT;

UPDATE groups SET office_id = (SELECT p.office_id FROM persons p WHERE p.id = manager);

INSERT INTO groups_history (id, _revision, _revision_type, name, description, send_flows_email, manager, office_id) 
     SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, name, description, send_flows_email, manager, office_id FROM groups;

-- Non è necessaria una down