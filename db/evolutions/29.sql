# --- !Ups

ALTER TABLE office
  ADD COLUMN contraction TEXT;
  
ALTER TABLE office_history
  ADD COLUMN contraction TEXT;

CREATE TABLE roles
(
  id BIGSERIAL PRIMARY KEY,
  name TEXT
);

CREATE TABLE roles_history
(
  id bigint NOT NULL,
  _revision integer NOT NULL REFERENCES revinfo (rev), 
  _revision_type smallint,
  name TEXT,
  CONSTRAINT roles_history_pkey PRIMARY KEY (id , _revision )
);

CREATE TABLE roles_permissions
(
  roles_id bigint NOT NULL REFERENCES roles (id),
  permissions_id bigint NOT NULL REFERENCES permissions (id)
);

CREATE TABLE roles_permissions_history
(
  roles_id bigint,
  permissions_id bigint,
  _revision integer NOT NULL REFERENCES revinfo (rev), 
  _revision_type smallint
);

CREATE TABLE users_roles_offices
(
  id BIGSERIAL PRIMARY KEY,
  office_id BIGINT REFERENCES office (id),
  role_id BIGINT REFERENCES roles (id),
  user_id BIGINT REFERENCES users (id),
  CONSTRAINT uro_unique_index UNIQUE (office_id, role_id, user_id)
);

DROP TABLE users_permissions_history;
DROP TABLE users_permissions;
DROP TABLE users_permissions_offices;

ALTER TABLE shift_time_table ALTER COLUMN id set DEFAULT nextval('seq_shift_time_table');

# ---!Downs

ALTER TABLE office
  DROP COLUMN contraction;
  
ALTER TABLE office_history
  DROP COLUMN contraction;
  
DROP TABLE roles_permissions_history;  

DROP TABLE roles_permissions;


DROP TABLE users_roles_offices;

DROP TABLE roles_history;

DROP TABLE roles;









CREATE TABLE users_permissions
(
  users_id bigint NOT NULL REFERENCES users (id),
  permissions_id bigint NOT NULL REFERENCES permissions (id)
);

CREATE TABLE users_permissions_history
(
  users_id bigint,
  permissions_id bigint,
  _revision integer NOT NULL REFERENCES revinfo (rev), 
  _revision_type smallint
);

CREATE TABLE users_permissions_offices
(
  id BIGSERIAL PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users (id),
  permission_id bigint NOT NULL REFERENCES permissions (id),
  office_id bigint NOT NULL REFERENCES office (id)
);




  
  
 