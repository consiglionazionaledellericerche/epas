# ---!Ups

ALTER TABLE absence_types DROP COLUMN meal_ticket_calculation;
ALTER TABLE absence_types DROP COLUMN multiple_use;
ALTER TABLE absence_types DROP COLUMN replacing_absence;
ALTER TABLE absence_types RENAME COLUMN justified_time_at_work TO time_at_work_modifier;

ALTER TABLE absence_types_history DROP COLUMN meal_ticket_calculation;
ALTER TABLE absence_types_history DROP COLUMN multiple_use;
ALTER TABLE absence_types_history DROP COLUMN replacing_absence;
ALTER TABLE absence_types_history RENAME COLUMN justified_time_at_work TO time_at_work_modifier;

UPDATE absence_types SET time_at_work_modifier = concat('Justify',time_at_work_modifier) WHERE time_at_work_modifier is not null;
UPDATE absence_types_history SET time_at_work_modifier = concat('Justify',time_at_work_modifier) WHERE time_at_work_modifier is not null;

# ---!Downs

UPDATE absence_types SET time_at_work_modifier = replace(time_at_work_modifier, 'Justify','');
UPDATE absence_types_history SET time_at_work_modifier = replace(time_at_work_modifier, 'Justify','');

ALTER TABLE absence_types ADD COLUMN meal_ticket_calculation boolean;
ALTER TABLE absence_types ADD COLUMN multiple_use boolean;
ALTER TABLE absence_types ADD COLUMN replacing_absence boolean;
ALTER TABLE absence_types RENAME COLUMN time_at_work_modifier TO justified_time_at_work;

ALTER TABLE absence_types_history ADD COLUMN meal_ticket_calculation boolean;
ALTER TABLE absence_types_history ADD COLUMN multiple_use boolean;
ALTER TABLE absence_types_history ADD COLUMN replacing_absence boolean;
ALTER TABLE absence_types_history RENAME COLUMN time_at_work_modifier TO justified_time_at_work;