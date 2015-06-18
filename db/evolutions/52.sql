# ---!Ups

ALTER TABLE persons ADD COLUMN person_in_charge bigint;
ALTER TABLE persons ADD COLUMN is_person_in_charge BOOLEAN DEFAULT FALSE;
ALTER TABLE persons ADD CONSTRAINT person_in_charge_fk FOREIGN KEY (person_in_charge) references persons (id);

ALTER TABLE persons_history ADD COLUMN person_in_charge bigint;
ALTER TABLE persons_history ADD COLUMN is_person_in_charge boolean default false;


# ---!Downs

ALTER TABLE persons_history DROP COLUMN person_in_charge;
ALTER TABLE persons_history DROP COLUMN is_person_in_charge;
ALTER TABLE persons DROP CONSTRAINT person_in_charge_fk;
ALTER TABLE persons DROP COLUMN person_in_charge;
ALTER TABLE persons DROP COLUMN is_person_in_charge;
