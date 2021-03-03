# --- !Ups

CREATE TABLE meal_ticket
(
  id BIGSERIAL PRIMARY KEY,
  code text,
  block integer,
  number integer,
  year int,
  quarter int,
  date date,
  person_id bigint NOT NULL REFERENCES persons (id),
  admin_id bigint NOT NULL REFERENCES persons (id)
);

CREATE TABLE meal_ticket_history
(
  id bigint NOT NULL,
  _revision integer NOT NULL REFERENCES revinfo (rev), 
  _revision_type smallint,
  code text,
  block integer,
  number integer,
  year int,
  quarter int,
  date date,
  person_id bigint,
  admin_id bigint,
  CONSTRAINT meal_ticket_history_pkey PRIMARY KEY (id , _revision )
);

# ---!Downs

DROP TABLE meal_ticket_history;
DROP TABLE meal_ticket;