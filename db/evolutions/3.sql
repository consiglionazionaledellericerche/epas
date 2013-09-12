# --- !Ups

--
--

ALTER TABLE ONLY absence_type_groups
    ADD CONSTRAINT fk71b9ff7730325be3 FOREIGN KEY (replacing_absence_type_id) REFERENCES absence_types(id);


