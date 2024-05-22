# --- !Ups
UPDATE working_time_types SET disabled = true where id in (2, 6);

INSERT INTO working_time_types (description, shift, meal_ticket_enabled, office_id, disabled, horizontal, version, enable_adjustment_for_quantity, external_id, updated_at)
VALUES ('Maternità CNR', false, true, null, false, true, 0, false, null, now());

INSERT INTO working_time_type_days (break_ticket_time, day_of_week, holiday, meal_ticket_time, time_meal_from, time_meal_to, time_slot_entrance_from,time_slot_entrance_to, 
time_slot_exit_from, time_slot_exit_to, working_time, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time, version, updated_at)
VALUES (0, 1, false, 240, 0, 0, 0, 0, 0, 0, 312, (select id from working_time_types where description = 'Maternità CNR'), 0, 0, 0, now());

INSERT INTO working_time_type_days (break_ticket_time, day_of_week, holiday, meal_ticket_time, time_meal_from, time_meal_to, time_slot_entrance_from,time_slot_entrance_to, 
time_slot_exit_from, time_slot_exit_to, working_time, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time, version, updated_at)
VALUES (0, 2, false, 240, 0, 0, 0, 0, 0, 0, 312, (select id from working_time_types where description = 'Maternità CNR'), 0, 0, 0, now());

INSERT INTO working_time_type_days (break_ticket_time, day_of_week, holiday, meal_ticket_time, time_meal_from, time_meal_to, time_slot_entrance_from,time_slot_entrance_to, 
time_slot_exit_from, time_slot_exit_to, working_time, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time, version, updated_at)
VALUES (0, 3, false, 240, 0, 0, 0, 0, 0, 0, 312, (select id from working_time_types where description = 'Maternità CNR'), 0, 0, 0, now());

INSERT INTO working_time_type_days (break_ticket_time, day_of_week, holiday, meal_ticket_time, time_meal_from, time_meal_to, time_slot_entrance_from,time_slot_entrance_to, 
time_slot_exit_from, time_slot_exit_to, working_time, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time, version, updated_at)
VALUES (0, 4, false, 240, 0, 0, 0, 0, 0, 0, 312, (select id from working_time_types where description = 'Maternità CNR'), 0, 0, 0, now());

INSERT INTO working_time_type_days (break_ticket_time, day_of_week, holiday, meal_ticket_time, time_meal_from, time_meal_to, time_slot_entrance_from,time_slot_entrance_to, 
time_slot_exit_from, time_slot_exit_to, working_time, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time, version, updated_at)
VALUES (0, 5, false, 240, 0, 0, 0, 0, 0, 0, 312, (select id from working_time_types where description = 'Maternità CNR'), 0, 0, 0, now());

INSERT INTO working_time_type_days (break_ticket_time, day_of_week, holiday, meal_ticket_time, time_meal_from, time_meal_to, time_slot_entrance_from,time_slot_entrance_to, 
time_slot_exit_from, time_slot_exit_to, working_time, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time, version, updated_at)
VALUES (0, 6, true, 0, 0, 0, 0, 0, 0, 0, 312, (select id from working_time_types where description = 'Maternità CNR'), 0, 0, 0, now());

INSERT INTO working_time_type_days (break_ticket_time, day_of_week, holiday, meal_ticket_time, time_meal_from, time_meal_to, time_slot_entrance_from,time_slot_entrance_to, 
time_slot_exit_from, time_slot_exit_to, working_time, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time, version, updated_at)
VALUES (0, 7, true, 0, 0, 0, 0, 0, 0, 0, 312, (select id from working_time_types where description = 'Maternità CNR'), 0, 0, 0, now());

# --- !Downs
-- Non è necessaria una down