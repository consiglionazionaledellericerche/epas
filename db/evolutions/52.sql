# ---!Ups

ALTER TABLE persons ADD COLUMN person_id bigint;
ALTER TABLE persons ADD COLUMN is_person_in_charge BOOLEAN DEFAULT FALSE;
ALTER TABLE persons ADD CONSTRAINT person_id_fk FOREIGN KEY (person_id) references persons (id);

ALTER TABLE persons_history ADD COLUMN person_id bigint;
ALTER TABLE persons_history ADD COLUMN is_person_in_charge boolean default false;


# ---!Downs

ALTER TABLE persons_history DROP COLUMN person_id;
ALTER TABLE persons_history DROP COLUMN is_person_in_charge;
ALTER TABLE persons DROP CONSTRAINT person_id_fk;
ALTER TABLE persons DROP COLUMN person_id;
ALTER TABLE persons DROP COLUMN is_person_in_charge;
