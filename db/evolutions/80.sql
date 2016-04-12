# ---!Ups

# -- Chiavi primarie mancanti

ALTER TABLE conf_period ADD PRIMARY KEY(id);
ALTER TABLE configurations_history ADD PRIMARY KEY (id, _revision);
ALTER TABLE shift_type_history ADD PRIMARY KEY (id, _revision);
ALTER TABLE persons_working_time_types DROP COLUMN person_id;

# ---!Downs

ALTER TABLE conf_period DROP CONSTRAINT conf_period_pkey;
ALTER TABLE configurations_history DROP CONSTRAINT configurations_history_pkey;
ALTER TABLE shift_type_history DROP CONSTRAINT shift_type_history_pkey;
ALTER TABLE persons_working_time_types ADD COLUMN person_id INT;