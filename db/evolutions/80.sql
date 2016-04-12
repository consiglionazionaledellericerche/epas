# ---!Ups

# -- Chiavi primarie mancanti

ALTER TABLE conf_period ADD PRIMARY KEY(id);
ALTER TABLE configurations_history ADD PRIMARY KEY (id, _revision);
ALTER TABLE shift_type_history ADD PRIMARY KEY (id, _revision);
ALTER TABLE persons_working_time_types DROP COLUMN person_id;

ALTER TABLE contract_month_recap DROP COLUMN abs_fap_usate;
ALTER TABLE contract_month_recap DROP COLUMN abs_fac_usate;
ALTER TABLE contract_month_recap DROP COLUMN abs_p_usati;
# ---!Downs

ALTER TABLE conf_period DROP CONSTRAINT conf_period_pkey;
ALTER TABLE configurations_history DROP CONSTRAINT configurations_history_pkey;
ALTER TABLE shift_type_history DROP CONSTRAINT shift_type_history_pkey;
ALTER TABLE persons_working_time_types ADD COLUMN person_id INT;

ALTER TABLE contract_month_recap ADD COLUMN abs_fap_usate INT;
ALTER TABLE contract_month_recap ADD COLUMN abs_fac_usate INT;
ALTER TABLE contract_month_recap ADD COLUMN abs_p_usati INT;