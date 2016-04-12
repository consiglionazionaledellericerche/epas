# ---!Ups

# -- Chiavi primarie mancanti

ALTER TABLE conf_period ADD PRIMARY KEY(id);
ALTER TABLE configurations_history ADD PRIMARY KEY (id, _revision);
ALTER TABLE shift_type_history ADD PRIMARY KEY (id, _revision);

DROP TABLE persons_working_time_types;

ALTER TABLE contract_month_recap DROP COLUMN abs_fap_usate;
ALTER TABLE contract_month_recap DROP COLUMN abs_fac_usate;
ALTER TABLE contract_month_recap DROP COLUMN abs_p_usati;

# ---!Downs

ALTER TABLE conf_period DROP CONSTRAINT conf_period_pkey;
ALTER TABLE configurations_history DROP CONSTRAINT configurations_history_pkey;
ALTER TABLE shift_type_history DROP CONSTRAINT shift_type_history_pkey;

CREATE TABLE persons_working_time_types (
  id SERIAL PRIMARY KEY,
  begin_date DATE,
  end_date DATE,
  working_time_type_id INT REFERENCES working_time_types(id),
  person_id REFERENCES persons(id)
);

ALTER TABLE contract_month_recap ADD COLUMN abs_fap_usate INT;
ALTER TABLE contract_month_recap ADD COLUMN abs_fac_usate INT;
ALTER TABLE contract_month_recap ADD COLUMN abs_p_usati INT;