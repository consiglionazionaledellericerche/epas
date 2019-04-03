# --- !Ups

ALTER TABLE contracts ALTER COLUMN perseo_id TYPE text;
ALTER TABLE contracts_history ALTER COLUMN perseo_id TYPE text;
ALTER TABLE persons ALTER COLUMN number TYPE text;
ALTER TABLE persons_history ALTER COLUMN number TYPE text;
ALTER TABLE persons DROP COLUMN iid;
ALTER TABLE persons_history DROP COLUMN iid;

# --- !Downs

ALTER TABLE contracts ALTER COLUMN perseo_id TYPE bigint USING perseo_id::bigint;
ALTER TABLE contracts_history ALTER COLUMN perseo_id TYPE bigint USING perseo_id::bigint;
ALTER TABLE persons ALTER COLUMN number TYPE integer USING perseo_id::integer;
ALTER TABLE persons_history ALTER COLUMN number TYPE integer USING perseo_id::integer;
ALTER TABLE persons ADD COLUMN iid TYPE integer;
ALTER TABLE persons_history ADD COLUMN iid TYPE integer;

