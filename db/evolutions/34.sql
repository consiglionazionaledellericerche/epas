# ---!Ups

CREATE TABLE contract_stamp_profiles(
id BIGSERIAL PRIMARY KEY,
fixed_working_time boolean,
start_from date,
end_to date,
contract_id bigint NOT NULL REFERENCES contracts (id)
);

# ---!Downs

DROP TABLE contract_stamp_profiles;



