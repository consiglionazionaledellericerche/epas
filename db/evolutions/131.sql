# --- !Ups

ALTER TABLE contract_month_recap_history ALTER contract_id DROP NOT NULL;

# --- !Downs

# -- non è necessaria una down