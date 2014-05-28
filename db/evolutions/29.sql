# --- !Ups

ALTER TABLE office
  ADD COLUMN contraction character varying(255);
  
ALTER TABLE office_history
  ADD COLUMN contraction character varying(255);

  
CREATE SEQUENCE seq_roles
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE;
    
CREATE SEQUENCE seq_users_roles_offices
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE;
  
CREATE TABLE roles
(
  id bigint DEFAULT nextval('seq_roles'::regclass) NOT NULL,
  name character varying(255),
  CONSTRAINT roles_pkey PRIMARY KEY (id )
);

CREATE TABLE roles_history
(
  id bigint NOT NULL,
  _revision integer NOT NULL,
  _revision_type smallint,
  name character varying(255),
  CONSTRAINT roles_history_pkey PRIMARY KEY (id , _revision ),
  CONSTRAINT fk1c620d12d54d10ea FOREIGN KEY (_revision)
      REFERENCES revinfo (rev)
);

CREATE TABLE roles_permissions
(
  roles_id bigint NOT NULL,
  permissions_id bigint NOT NULL,
  CONSTRAINT fk250ae0234428ea9 FOREIGN KEY (permissions_id)
      REFERENCES permissions (id),
  CONSTRAINT fk250ae024001c377 FOREIGN KEY (roles_id)
      REFERENCES roles (id)
);

CREATE TABLE users_roles_offices
(
  id bigint DEFAULT nextval('seq_users_roles_offices'::regclass) NOT NULL,
  office_id bigint,
  role_id bigint,
  user_id bigint,
  CONSTRAINT users_roles_offices_pkey PRIMARY KEY (id ),
  CONSTRAINT fk4a11f3be2d0fa45e FOREIGN KEY (office_id)
      REFERENCES office (id),
  CONSTRAINT fk4a11f3be47140efe FOREIGN KEY (user_id)
      REFERENCES users (id),
  CONSTRAINT fk4a11f3bea1e94b1e FOREIGN KEY (role_id)
      REFERENCES roles (id)
);

DROP TABLE users_permissions_history;
DROP TABLE users_permissions;
DROP TABLE users_permissions_offices;


# ---!Downs

ALTER TABLE office
  DROP COLUMN contraction;
  
ALTER TABLE office_history
  DROP COLUMN contraction;
 