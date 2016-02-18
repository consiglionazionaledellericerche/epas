# ---!Ups
ALTER TABLE users ADD COLUMN disabled boolean;
ALTER TABLE users ADD COLUMN expire_date date;
ALTER TABLE users_history ADD COLUMN disabled boolean;
ALTER TABLE users_history ADD COLUMN expire_date date;
UPDATE users set disabled = false;
update users set expire_date = null;


# ---!Downs

ALTER TABLE users_history DROP COLUMN disabled;
ALTER TABLE users_history DROP COLUMN expire_date;
ALTER TABLE users DROP COLUMN disabled;
ALTER TABLE users DROP COLUMN expire_date;
