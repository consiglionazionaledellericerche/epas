# ---!Ups

ALTER TABLE initialization_groups RENAME COLUMN initialization_date TO date;
ALTER TABLE initialization_groups ADD COLUMN units_input INT;
ALTER TABLE initialization_groups ADD COLUMN hours_input INT;
ALTER TABLE initialization_groups ADD COLUMN minutes_input INT;
ALTER TABLE initialization_groups ADD COLUMN average_week_time INT;

ALTER TABLE initialization_groups_history RENAME COLUMN initialization_date TO date;
ALTER TABLE initialization_groups_history ADD COLUMN units_input INT;
ALTER TABLE initialization_groups_history ADD COLUMN hours_input INT;
ALTER TABLE initialization_groups_history ADD COLUMN minutes_input INT;
ALTER TABLE initialization_groups_history ADD COLUMN average_week_time INT;

CREATE TABLE category_tabs (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  priority INTEGER NOT NULL,
  is_default BOOLEAN NOT NULL,
  description TEXT
);

CREATE TABLE category_tabs_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  name TEXT,
  priority INTEGER,
  is_default BOOLEAN,
  description TEXT,
  CONSTRAINT category_tabs_history_pk PRIMARY KEY(id, _revision)
);

ALTER TABLE category_group_absence_types 
  ADD COLUMN category_tab_id BIGINT REFERENCES category_tabs (id);
  
ALTER TABLE category_group_absence_types_history 
  ADD COLUMN category_tab_id BIGINT;
  
ALTER TABLE group_absence_types ADD COLUMN automatic BOOLEAN DEFAULT false;
ALTER TABLE group_absence_types_history ADD COLUMN automatic BOOLEAN DEFAULT false;

ALTER TABLE group_absence_types ADD COLUMN initializable BOOLEAN DEFAULT false;
ALTER TABLE group_absence_types_history ADD COLUMN initializable BOOLEAN DEFAULT false;

# ---!Downs

ALTER TABLE group_absence_types DROP COLUMN automatic;
ALTER TABLE group_absence_types_history  DROP COLUMN automatic;
ALTER TABLE group_absence_types DROP COLUMN initializable;
ALTER TABLE group_absence_types_history  DROP COLUMN initializable;

ALTER TABLE category_group_absence_types DROP COLUMN category_tab_id;
ALTER TABLE category_group_absence_types_history  DROP COLUMN category_tab_id;
DROP TABLE category_tabs;
DROP TABLE category_tabs_history;

ALTER TABLE initialization_groups RENAME COLUMN date TO initialization_date;
ALTER TABLE initialization_groups DROP COLUMN units_input;
ALTER TABLE initialization_groups DROP COLUMN hours_input;
ALTER TABLE initialization_groups DROP COLUMN minutes_input;
ALTER TABLE initialization_groups DROP COLUMN average_week_time;

ALTER TABLE initialization_groups_history RENAME COLUMN date TO initialization_date;
ALTER TABLE initialization_groups_history DROP COLUMN units_input;
ALTER TABLE initialization_groups_history DROP COLUMN hours_input;
ALTER TABLE initialization_groups_history DROP COLUMN minutes_input;
ALTER TABLE initialization_groups_history DROP COLUMN average_week_time; 