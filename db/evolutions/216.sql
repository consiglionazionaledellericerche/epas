# --- !Ups

UPDATE working_time_types SET description = 'Allattamento' WHERE description = 'Maternità CNR';

# --- !Downs
-- Non è necessaria una down 