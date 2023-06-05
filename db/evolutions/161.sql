# --- !Ups

ALTER TABLE persons ADD COLUMN updated_at TIMESTAMP default now();
ALTER TABLE office ADD COLUMN updated_at TIMESTAMP default now();
ALTER TABLE contracts ADD COLUMN updated_at TIMESTAMP default now();
ALTER TABLE affiliation ADD COLUMN updated_at TIMESTAMP default now();
ALTER TABLE working_time_types ADD COLUMN updated_at TIMESTAMP default now();
ALTER TABLE working_time_type_days ADD COLUMN updated_at TIMESTAMP default now();
ALTER TABLE contracts_working_time_types ADD COLUMN updated_at TIMESTAMP default now();

# --- !Downs

ALTER TABLE persons DROP COLUMN updated_at;
ALTER TABLE office DROP COLUMN updated_at;
ALTER TABLE contracts DROP COLUMN updated_at;
ALTER TABLE affiliation DROP COLUMN updated_at;
ALTER TABLE working_time_types DROP COLUMN updated_at;
ALTER TABLE working_time_type_days DROP COLUMN updated_at;
ALTER TABLE contracts_working_time_types DROP COLUMN updated_at;