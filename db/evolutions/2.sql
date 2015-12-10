# --- !Ups

ALTER TABLE ONLY absence_type_groups
    ADD CONSTRAINT fk71b9ff7730325be3 FOREIGN KEY (replacing_absence_type_id) REFERENCES absence_types(id);

ALTER TABLE ONLY absence_types_history
    ADD CONSTRAINT fk2631804cd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);
