# --- !Ups

ALTER TABLE users ADD COLUMN subject_id TEXT; 
ALTER TABLE users_history ADD COLUMN subject_id TEXT;

UPDATE users AS u SET subject_id = p.eppn FROM persons p 
	WHERE p.user_id = u.id and eppn is NOT NULL;

ALTER TABLE users ADD COLUMN password_sha512 TEXT;
ALTER TABLE users_history ADD COLUMN password_sha512 TEXT;

# --- !Downs

ALTER TABLE users DROP COLUMN subject_id; 
ALTER TABLE users_history DROP COLUMN subject_id;

ALTER TABLE users DROP COLUMN password_sha512; 
ALTER TABLE users_history DROP COLUMN password_sha512;