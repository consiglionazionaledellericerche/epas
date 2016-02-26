# ---!Ups
ALTER TABLE users ADD COLUMN disabled boolean;
ALTER TABLE users ADD COLUMN expire_date date;
ALTER TABLE users_history ADD COLUMN disabled boolean;
ALTER TABLE users_history ADD COLUMN expire_date date;
UPDATE users SET disabled = false;
UPDATE users SET expire_date = null;

-- rimuovo la relazione con gli uffici presente sui badge readers per spostarla sugli user e renderla nullabile
ALTER TABLE badge_readers DROP CONSTRAINT badge_reader_owner_id_fkey;

ALTER TABLE users ADD COLUMN office_owner_id BIGINT;
ALTER TABLE users_history ADD COLUMN office_owner_id BIGINT;
ALTER TABLE users ADD FOREIGN KEY (office_owner_id) REFERENCES office (id);

-- inserisco i valori presenti in office_owner_id di badge_readers nel nuovo office_owner_id presente su users

UPDATE users
SET office_owner_id = br.office_owner_id
FROM badge_readers br 
WHERE users.id = br.user_id;

-- rimuovo le colonne che ospitavano la chiave esterna della relazione con gli uffici
ALTER TABLE badge_readers_history DROP COLUMN office_owner_id;
ALTER TABLE badge_readers DROP COLUMN office_owner_id;


# ---!Downs

ALTER TABLE users_history DROP COLUMN disabled;
ALTER TABLE users_history DROP COLUMN expire_date;
ALTER TABLE users DROP COLUMN disabled;
ALTER TABLE users DROP COLUMN expire_date;

ALTER TABLE users DROP CONSTRAINT users_office_owner_id_fkey;

ALTER TABLE badge_readers ADD COLUMN office_owner_id BIGINT;
ALTER TABLE badge_readers ADD FOREIGN KEY (office_owner_id) REFERENCES office (id);


UPDATE badge_readers
SET office_owner_id = u.office_owner_id
FROM users u
WHERE badge_readers.user_id = u.id;


ALTER TABLE users_history DROP COLUMN office_owner_id;
ALTER TABLE users DROP COLUMN office_owner_id;


