# --- Ups!

ALTER TABLE general_setting ADD COLUMN saturday_holiday_for_shift BOOLEAN;
ALTER TABLE general_setting_history ADD COLUMN saturday_holiday_for_shift BOOLEAN;

# --- Downs!

ALTER TABLE general_setting DROP COLUMN saturday_holiday_for_shift;
ALTER TABLE general_setting_history DROP COLUMN saturday_holiday_for_shift;
