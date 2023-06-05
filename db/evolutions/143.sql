# --- !Ups

ALTER TABLE general_setting ADD COLUMN handle_groups_by_institute BOOLEAN default true;
ALTER TABLE general_setting_history ADD COLUMN handle_groups_by_institute BOOLEAN;


# --- !Downs
-- non è necessaria una down