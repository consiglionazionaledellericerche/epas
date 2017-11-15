# ---!Ups

ALTER TABLE absences ADD COLUMN external_identifier BIGINT;
ALTER TABLE absences_history ADD COLUMN external_identifier BIGINT;
ALTER TABLE person_days	ADD COLUMN working_time_in_mission int;
ALTER TABLE person_days_history	ADD COLUMN working_time_in_mission int;
INSERT INTO users (id, expire_recovery_token, password, recovery_token, username, 
disabled, expire_date, office_owner_id ) 
select nextval('seq_users'), null, md5('missioni'), null, 'missioni',false, null, null;
INSERT INTO user_roles select id,'MISSIONS_MANAGER' from users WHERE username = 'missioni';

# ---!Downs

ALTER TABLE absences_history DROP COLUMN external_identifier;
ALTER TABLE absences DROP COLUMN external_identifier;
ALTER TABLE person_days_history DROP COLUMN working_time_in_mission;
ALTER TABLE person_days DROP COLUMN working_time_in_mission;
DELETE FROM user_roles where roles = 'MISSIONS_MANAGER';
DELETE FROM users where username = 'missioni';

