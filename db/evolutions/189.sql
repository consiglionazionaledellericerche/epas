# --- !Ups

ALTER TABLE absence_types ADD COLUMN meal_ticket_behaviour TEXT;
UPDATE absence_types SET meal_ticket_behaviour = 'notAllowMealTicket' WHERE time_for_mealticket = false;
UPDATE absence_types SET meal_ticket_behaviour = 'allowMealTicket' WHERE time_for_mealticket = true;
UPDATE absence_types SET meal_ticket_behaviour = 'preventMealTicket' WHERE code = '103RT';
ALTER TABLE absence_types DROP COLUMN time_for_mealticket;

# --- !Downs