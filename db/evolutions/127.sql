# --- !Ups

-- Creo una nouva revisione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO roles (name, version) VALUES ('mealTicketManager', 0);

INSERT INTO roles_history (id, _revision, _revision_type, name) 
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, name  FROM roles WHERE name = 'mealTicketManager';

INSERT INTO users_roles_offices (office_id, role_id, user_id, version)
SELECT uro.office_id, (SELECT id FROM roles WHERE name = 'mealTicketManager'), uro.user_id, 0 
FROM users_roles_offices uro LEFT JOIN roles r ON r.id = uro.role_id WHERE r.name = 'personnelAdmin';

INSERT INTO users_roles_offices_history (id, _revision, _revision_type, office_id, role_id, user_id)
SELECT uro.id, (SELECT MAX(rev) AS rev FROM revinfo), 0, uro.office_id, uro.role_id, uro.user_id FROM users_roles_offices uro
LEFT JOIN roles r ON r.id = uro.role_id WHERE r.name = 'mealTicketManager'; 


-- Non è necessaria una down