# ---!Ups

ALTER TABLE absences ADD COLUMN external_identifier BIGINT;
ALTER TABLE absences_history ADD COLUMN external_identifier BIGINT;
ALTER TABLE person_days	ADD COLUMN working_time_in_mission int;
ALTER TABLE person_days_history	ADD COLUMN working_time_in_mission int;

INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);


INSERT INTO users (id, expire_recovery_token, password, recovery_token, username, 
disabled, expire_date, office_owner_id ) 
select nextval('seq_users'), null, md5('Doh5aule'), null, 'app.missioni',false, null, null;

INSERT INTO users_history (id, _revision, _revision_type)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0 FROM users WHERE username = 'app.missioni';

INSERT INTO user_roles select id,'MISSIONS_MANAGER' from users WHERE username = 'app.missioni';

ALTER TABLE person_reperibility_days ALTER id SET DEFAULT nextval('seq_person_reperibility_days'::regclass);

# ---!Downs

ALTER TABLE absences_history DROP COLUMN external_identifier;
ALTER TABLE absences DROP COLUMN external_identifier;
ALTER TABLE person_days_history DROP COLUMN working_time_in_mission;
ALTER TABLE person_days DROP COLUMN working_time_in_mission;
DELETE FROM user_roles where roles = 'MISSIONS_MANAGER';
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);
INSERT INTO users_history (id, _revision, _revision_type)
SELECT id, (SELECT MAX (rev) AS rev FROM revinfo), 2 FROM users_history WHERE username = 'app.missioni';
DELETE FROM users where username = 'app.missioni';

