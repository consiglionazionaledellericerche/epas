# --- !Ups

ALTER TABLE persons ADD COLUMN fiscal_code TEXT;
ALTER TABLE persons_history ADD COLUMN fiscal_code TEXT;

# --- !Downs

ALTER TABLE persons DROP COLUMN fiscal_code;
ALTER TABLE persons_history DROP COLUMN fiscal_code;
