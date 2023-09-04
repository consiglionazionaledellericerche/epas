# --- !Ups

ALTER TABLE persons ADD COLUMN residence TEXT;
ALTER TABLE persons_history ADD COLUMN residence TEXT;


# --- !Downs

ALTER TABLE persons DROP COLUMN residence;
ALTER TABLE persons_history DROP COLUMN residence;