# --- !Ups

ALTER TABLE general_setting ADD COLUMN rounding_shift_quantity BOOLEAN;
ALTER TABLE general_setting_history ADD COLUMN rounding_shift_quantity BOOLEAN;

# --- !Downs

ALTER TABLE general_setting DROP COLUMN rounding_shift_quantity;
ALTER TABLE general_setting_history DROP COLUMN rounding_shift_quantity;
