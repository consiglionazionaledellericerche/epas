# ---!Ups

# -- Chiavi primarie mancanti

ALTER TABLE conf_period ADD PRIMARY KEY(id);
ALTER TABLE configurations_history ADD PRIMARY KEY (id, _revision);
ALTER TABLE shift_type_history ADD PRIMARY KEY (id, _revision);
DELETE FROM persons_working_time_types WHERE id IN
  (SELECT pwtt.id FROM persons_working_time_types pwtt LEFT JOIN persons p on pwtt.person_id = p.id WHERE p.id IS NULL);
ALTER TABLE persons_working_time_types ADD CONSTRAINT persons_working_time_types_person_id FOREIGN KEY (person_id) REFERENCES persons (id);

# ---!Downs

ALTER TABLE conf_period DROP CONSTRAINT conf_period_pkey;
ALTER TABLE configurations_history DROP CONSTRAINT configurations_history_pkey;
ALTER TABLE shift_type_history DROP CONSTRAINT shift_type_history_pkey;
ALTER TABLE persons_working_time_types DROP CONSTRAINT persons_working_time_types_person_id;