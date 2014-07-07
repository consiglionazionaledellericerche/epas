# ---!Ups


CREATE TABLE stamp_profiles_contracts(
id BIGSERIAL PRIMARY KEY,
start_from date,
end_to date,
contract_id bigint not null REFERENCES contracts (id),
stamp_profile_id bigint not null REFERENCES stamp_profiles (id)
      
);


CREATE TABLE stamp_profiles_tmp(
id BIGSERIAL PRIMARY KEY,
fixed_working_time boolean,
description text
);


insert into stamp_profiles_contracts(start_from, end_to

# ---!Downs


DROP SEQUENCE seq_stamp_profiles_contracts;

DROP TABLE stamp_profiles_contracts;

ALTER TABLE stamp_profiles ADD COLUMN person_id SET NOT NULL BIGINT;
ALTER TABLE stamp_profiles ADD CONSTRAINT person_id_fk FOREIGN KEY (person_id) references persons (id);
ALTER TABLE stamp_profiles ADD COLUMN start_from date;
ALTER TABLE stamp_profiles ADD COLUMN end_to date;
ALTER TABLE stamp_profiles DROP COLUMN description;




