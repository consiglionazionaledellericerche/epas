# ---!Ups

ALTER TABLE absences ADD COLUMN external_identifier BIGINT;
ALTER TABLE absences_history ADD COLUMN external_identifier BIGINT;
ALTER TABLE person_days	ADD COLUMN working_time_in_mission int;
ALTER TABLE person_days_history	ADD COLUMN working_time_in_mission int;
ALTER TABLE person_reperibility_days ALTER id SET DEFAULT nextval('seq_person_reperibility_days'::regclass);

# ---!Downs

ALTER TABLE absences_history DROP COLUMN external_identifier;
ALTER TABLE absences DROP COLUMN external_identifier;
ALTER TABLE person_days_history DROP COLUMN working_time_in_mission;
ALTER TABLE person_days DROP COLUMN working_time_in_mission;

DELETE FROM users where username = 'app.missioni';
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);
INSERT INTO users_history (id, _revision, _revision_type)
SELECT id, (SELECT MAX (rev) AS rev FROM revinfo), 2 FROM users_history WHERE username = 'app.missioni';

DELETE FROM user_roles WHERE roles = 'MISSIONS_MANAGER';
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);
INSER INTO user_roles_history (_revision, _revision_type, user_id, roles)
SELECT (SELECT MAX (rev) AS rev FROM revinfo), 2, user_id, roles  FROM user_roles_history WHERE roles = 'MISSIONS_MANAGER';


