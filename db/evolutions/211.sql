# --- !Ups

CREATE TABLE group_overtimes
(
  id BIGSERIAL PRIMARY KEY,
  date_of_update DATE NOT NULL,
  year INTEGER,
  number_of_hours INTEGER,
  group_id bigint REFERENCES groups(id),
  version INT DEFAULT 0
);

CREATE TABLE group_overtimes_history
(
  id bigint NOT NULL,
  _revision integer NOT NULL REFERENCES revinfo (rev), 
  _revision_type smallint,
  date_of_update DATE,
  year INTEGER,
  number_of_hours INTEGER,
  group_id bigint,
  CONSTRAINT group_overtimes_history_pkey PRIMARY KEY (id , _revision )
);

CREATE TABLE person_overtimes
(
  id BIGSERIAL PRIMARY KEY,
  date_of_update DATE NOT NULL,
  year INTEGER,
  number_of_hours INTEGER,
  person_id bigint REFERENCES persons(id),
  version INT DEFAULT 0
);

CREATE TABLE person_overtimes_history
(
  id bigint NOT NULL,
  _revision integer NOT NULL REFERENCES revinfo (rev), 
  _revision_type smallint,
  date_of_update DATE,
  year INTEGER,
  number_of_hours INTEGER,
  person_id bigint,
  CONSTRAINT person_overtimes_history_pkey PRIMARY KEY (id , _revision )
);

ALTER TABLE groups
  ADD COLUMN group_overtimes_id bigint REFERENCES group_overtimes(id);
    
ALTER TABLE groups_history
  ADD COLUMN group_overtimes_id bigint;
  
ALTER TABLE persons
  ADD COLUMN person_overtimes_id bigint REFERENCES person_overtimes(id);
  
ALTER TABLE persons_history
  ADD COLUMN person_overtimes_id bigint;

# --- !Downs

-- non è necessaria alcuna down