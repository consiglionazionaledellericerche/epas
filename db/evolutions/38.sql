# --- !Ups

ALTER TABLE revinfo ADD COLUMN owner_id BIGINT REFERENCES users;
ALTER TABLE revinfo ADD COLUMN ipaddress TEXT;

# ---!Downs

ALTER TABLE revinfo DROP COLUMN ipaddress;
ALTER TABLE revinfo DROP COLUMN owner_id;
