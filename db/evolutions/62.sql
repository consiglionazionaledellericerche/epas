# ---!Ups

ALTER TABLE users ADD CONSTRAINT users_unique_key UNIQUE (username);

# ---!Downs

ALTER TABLE users DROP CONSTRAINT users_unique_key;