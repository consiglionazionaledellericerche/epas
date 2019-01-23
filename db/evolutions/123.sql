# --- !Ups

ALTER TABLE contracts ALTER COLUMN perseo_id TYPE text;
ALTER TABLE contracts_history ALTER COLUMN perseo_id TYPE text;

# --- !Downs

ALTER TABLE contracts ALTER COLUMN perseo_id TYPE bigint USING perseo_id::bigint;
ALTER TABLE contracts_history ALTER COLUMN perseo_id TYPE bigint USING perseo_id::bigint;

