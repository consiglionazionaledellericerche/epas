# ---!Ups

ALTER TABLE contracts ADD COLUMN source_remaining_meal_ticket integer;
UPDATE contracts set source_remaining_meal_ticket = 0;

ALTER TABLE contract_month_recap ADD COLUMN s_r_bp_init integer;
UPDATE contract_month_recap set s_r_bp_init = 0;

ALTER TABLE persons ADD COLUMN eppn varchar(255);
ALTER TABLE persons_history ADD COLUMN eppn varchar(255);

# ---!Downs

ALTER TABLE contracts DROP COLUMN source_remaining_meal_ticket;
ALTER TABLE contract_month_recap DROP COLUMN s_r_bp_init;

ALTER TABLE persons DROP COLUMN eppn;
ALTER TABLE persons_history DROP COLUMN eppn;
