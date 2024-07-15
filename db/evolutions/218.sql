# --- !Ups

UPDATE working_time_types SET description = 'Allattamento fino a 30-06-2024' WHERE description = 'Allattamento fino a 31-06-2024' and office_id is null;

# --- !Downs
-- Non è necessaria una down 