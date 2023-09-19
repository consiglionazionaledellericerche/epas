# --- !Ups

ALTER TABLE general_setting ADD COLUMN show_overtime_request BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting_history ADD COLUMN show_overtime_request BOOLEAN DEFAULT FALSE;

ALTER TABLE competence_requests RENAME COLUMN reperibility_manager_approved TO manager_approved;
ALTER TABLE competence_requests RENAME COLUMN reperibility_manager_approval_required TO manager_approval_required;

ALTER TABLE competence_requests ADD COLUMN office_head_approval_required BOOLEAN default TRUE;
ALTER TABLE competence_requests ADD COLUMN office_head_approved timestamp without time zone;

ALTER TABLE competence_requests ADD COLUMN first_approval_required BOOLEAN default TRUE;
ALTER TABLE competence_requests ADD COLUMN first_approved timestamp without time zone;

ALTER TABLE competence_requests_history RENAME COLUMN reperibility_manager_approved TO manager_approved;
ALTER TABLE competence_requests_history RENAME COLUMN reperibility_manager_approval_required TO manager_approval_required;

ALTER TABLE competence_requests_history ADD COLUMN office_head_approval_required BOOLEAN;
ALTER TABLE competence_requests_history ADD COLUMN office_head_approved timestamp without time zone;

ALTER TABLE competence_requests_history ADD COLUMN first_approval_required BOOLEAN;
ALTER TABLE competence_requests_history ADD COLUMN first_approved timestamp without time zone;

UPDATE competence_request_events
SET event_type = 'MANAGER_APPROVAL' WHERE event_type = 'REPERIBILITY_MANAGER_APPROVAL'; 

UPDATE competence_requests
SET first_approval_required = false WHERE type = 'CHANGE_REPERIBILITY_REQUEST';

# --- !Downs

ALTER TABLE general_setting DROP COLUMN show_overtime_request;
ALTER TABLE general_setting_history DROP COLUMN show_overtime_request;