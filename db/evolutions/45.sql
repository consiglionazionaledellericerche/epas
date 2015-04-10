# ---!Ups

CREATE TABLE shift_categories
(
  id BIGSERIAL PRIMARY KEY,
  description TEXT,
  supervisor bigint REFERENCES persons(id)
);

CREATE TABLE shift_categories_history
(
  id bigint NOT NULL,
  _revision integer NOT NULL REFERENCES revinfo (rev), 
  _revision_type smallint,
  description TEXT,
  supervisor bigint REFERENCES persons(id),
  CONSTRAINT shift_categories_history_pkey PRIMARY KEY (id , _revision )
);


ALTER TABLE shift_type
  ADD COLUMN shift_categories_id bigint REFERENCES shift_categories(id);
  
  
ALTER TABLE shift_type_history
  ADD COLUMN shift_categories_id bigint;

ALTER TABLE shift_type DROP COLUMN supervisor;
ALTER TABLE shift_type_history DROP COLUMN supervisor;

ALTER TABLE competences ADD COLUMN exceeded_mins integer;
ALTER TABLE competences ALTER COLUMN valuerequested TYPE numeric(5,2);

# ---!Downs

ALTER TABLE shift_type ADD COLUMN supervisor bigint REFERENCES persons(id);
ALTER TABLE shift_type_history ADD COLUMN supervisor bigint REFERENCES persons(id);

ALTER TABLE shift_type DROP COLUMN shift_categories_id;
ALTER TABLE shift_type_history DROP COLUMN shift_categories_id;

ALTER TABLE competences DROP COLUMN exceeded_mins;
ALTER TABLE competences ALTER COLUMN valuerequested TYPE numeric(4,1);

  
DROP TABLE shift_categories_history;  
DROP TABLE shift_categories;