# --- !Ups

-- Permette più periodi di validità per lo stesso parametro della persona (es. AGILE_WORK_MONTHLY_LIMIT)
ALTER TABLE person_configurations DROP CONSTRAINT unique_param;
ALTER TABLE person_configurations
ADD CONSTRAINT unique_param UNIQUE (person_id, epas_param, begin_date);

# --- !Downs

ALTER TABLE person_configurations DROP CONSTRAINT unique_param;
ALTER TABLE person_configurations
ADD CONSTRAINT unique_param UNIQUE (person_id, epas_param);
