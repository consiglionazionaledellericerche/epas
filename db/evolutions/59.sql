# ---!Ups

ALTER TABLE absence_types DROP COLUMN meal_ticket_calculation;
ALTER TABLE absence_types DROP COLUMN multiple_use;
ALTER TABLE absence_types DROP COLUMN replacing_absence;

ALTER TABLE absence_types_history DROP COLUMN meal_ticket_calculation;
ALTER TABLE absence_types_history DROP COLUMN multiple_use;
ALTER TABLE absence_types_history DROP COLUMN replacing_absence;

# ---!Downs

ALTER TABLE absence_types ADD COLUMN meal_ticket_calculation boolean;
ALTER TABLE absence_types ADD COLUMN multiple_use boolean;
ALTER TABLE absence_types ADD COLUMN replacing_absence boolean;

ALTER TABLE absence_types_history ADD COLUMN meal_ticket_calculation boolean;
ALTER TABLE absence_types_history ADD COLUMN multiple_use boolean;
ALTER TABLE absence_types_history ADD COLUMN replacing_absence boolean;