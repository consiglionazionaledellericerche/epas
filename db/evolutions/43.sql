# --- !Ups

ALTER TABLE persons_working_time_types DROP constraint "fkb9432476e7a7b1be";

# ---!Downs

ALTER TABLE persons_working_time_types ADD CONSTRAINT "fkb9432476e7a7b1be" foreign key (person_id) REFERENCES persons (id);