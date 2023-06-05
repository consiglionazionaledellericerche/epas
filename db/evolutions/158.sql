# ---!Ups

ALTER TABLE absence_types ADD COLUMN is_real_absence BOOLEAN default TRUE;
ALTER TABLE absence_types_history ADD COLUMN is_real_absence BOOLEAN;

UPDATE absence_types SET is_real_absence = FALSE WHERE code in ('COVID19','COVID19BP','103','103BP','105BP');

# ---!Downs

ALTER TABLE absence_types_history DROP COLUMN is_real_absence;
ALTER TABLE absence_types DROP COLUMN is_real_absence;