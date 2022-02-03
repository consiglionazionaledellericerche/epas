# --- !Ups

ALTER TABLE stampings ADD COLUMN marked_by_telework BOOLEAN DEFAULT FALSE;
ALTER TABLE stampings_history ADD COLUMN marked_by_telework BOOLEAN DEFAULT FALSE;

# --- !Downs

ALTER TABLE stampings_history DROP COLUMN marked_by_telework;
ALTER TABLE stampings DROP COLUMN marked_by_telework;