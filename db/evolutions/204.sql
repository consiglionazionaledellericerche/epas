# --- !Ups

ALTER TABLE competence_requests RENAME COLUMN reperibility_manager_approved TO manager_approved;
ALTER TABLE competence_requests RENAME COLUMN reperibility_manager_approval_required TO manager_approval_required;

ALTER TABLE competence_requests ADD COLUMN office_head_approval_required BOOLEAN default TRUE;
ALTER TABLE competence_requests ADD COLUMN office_head_approved timestamp without time zone;

ALTER TABLE competence_requests_history RENAME COLUMN reperibility_manager_approved TO manager_approved;
ALTER TABLE competence_requests_history RENAME COLUMN reperibility_manager_approval_required TO manager_approval_required;

ALTER TABLE competence_requests_history ADD COLUMN office_head_approval_required BOOLEAN default TRUE;
ALTER TABLE competence_requests_history ADD COLUMN office_head_approved timestamp without time zone;

# --- !Downs

-- non è necessaria una down