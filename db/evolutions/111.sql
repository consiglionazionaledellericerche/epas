# ---!Ups

ALTER TABLE stampings ADD COLUMN place TEXT;
ALTER TABLE stampings ADD COLUMN reason TEXT;

ALTER TABLE stampings_history ADD COLUMN place TEXT;
ALTER TABLE stampings_history ADD COLUMN reason TEXT;

# ---!Downs

ALTER TABLE stampings_history DROP COLUMN place;
ALTER TABLE stampings_history DROP COLUMN reason;

ALTER TABLE stampings DROP COLUMN place;
ALTER TABLE stampings DROP COLUMN reason;