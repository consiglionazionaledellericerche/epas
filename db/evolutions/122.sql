# --- !Ups

ALTER TABLE shift_type ADD COLUMN allow_unpair_slots BOOLEAN DEFAULT false;
ALTER TABLE shift_type_history ADD COLUMN allow_unpair_slots BOOLEAN;

# --- !Downs

ALTER TABLE shift_type_history DROP COLUMN allow_unpair_slots;
ALTER TABLE shift_type DROP COLUMN allow_unpair_slots;