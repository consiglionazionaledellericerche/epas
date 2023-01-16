# --- !Ups

UPDATE configurations SET field_value = 'electronic' WHERE field_value = 'false' AND epas_param = 'MEAL_TICKET_BLOCK_TYPE';

# --- !Downs

-- non è necessaria una down