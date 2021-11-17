# ---!Ups

ALTER TABLE absence_types ADD COLUMN to_update BOOLEAN default TRUE;
ALTER TABLE absence_types_history ADD COLUMN to_update BOOLEAN;

# ---!Downs

ALTER TABLE absence_types_history DROP COLUMN to_update;
ALTER TABLE absence_types DROP COLUMN to_update;