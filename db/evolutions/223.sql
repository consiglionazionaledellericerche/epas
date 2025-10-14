# --- !Ups

ALTER TABLE contract_month_recap ADD COLUMN s_pfps INT DEFAULT 0;

ALTER TABLE contract_month_recap_history ADD COLUMN s_pfps INT DEFAULT 0;

# --- !Downs
# -- Non è necessaria una down