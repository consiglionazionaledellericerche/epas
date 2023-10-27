# --- !Ups

CREATE TABLE group_overtimes
(
  id BIGSERIAL PRIMARY KEY,
  date_of_update TIMESTAMP WITHOUT TIME ZONE NOT NULL,
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
  date_of_update TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  year INTEGER,
  number_of_hours INTEGER,
  group_id bigint,
  CONSTRAINT group_oertimes_history_pkey PRIMARY KEY (id , _revision )
);

ALTER TABLE groups
  ADD COLUMN group_overtimes_id bigint REFERENCES group_overtimes(id);
  
  
ALTER TABLE groups_history
  ADD COLUMN group_overtimes_id bigint;

# --- !Downs

-- non è necessaria alcuna down