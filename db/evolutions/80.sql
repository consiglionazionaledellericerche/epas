# --- !Ups

ALTER TABLE institutes ADD COLUMN perseo_id BIGINT;
ALTER TABLE institutes_history ADD COLUMN perseo_id BIGINT;

ALTER TABLE office ADD COLUMN perseo_id BIGINT;
ALTER TABLE office_history ADD COLUMN perseo_id BIGINT;

ALTER TABLE persons ADD COLUMN perseo_id BIGINT;
ALTER TABLE persons_history ADD COLUMN perseo_id BIGINT;

ALTER TABLE contracts ADD COLUMN perseo_id BIGINT;

ALTER TABLE contracts ADD COLUMN is_temporary BOOLEAN default false;

UPDATE contracts SET is_temporary = true WHERE end_date is not null; 

# --- !Downs

ALTER TABLE institutes DROP COLUMN perseo_id;
ALTER TABLE institutes_history DROP COLUMN perseo_id;

ALTER TABLE office DROP COLUMN perseo_id;
ALTER TABLE office_history DROP COLUMN perseo_id;

ALTER TABLE persons DROP COLUMN perseo_id;
ALTER TABLE persons_history DROP COLUMN perseo_id;

ALTER TABLE contracts DROP COLUMN perseo_id;

ALTER TABLE contracts DROP COLUMN is_temporary;


