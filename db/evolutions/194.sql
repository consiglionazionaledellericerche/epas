# ---!Ups

ALTER TABLE information_requests ADD COLUMN manager_approved TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE information_requests ADD COLUMN manager_approval_required BOOLEAN;

ALTER TABLE information_requests_history ADD COLUMN manager_approved TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE information_requests_history ADD COLUMN manager_approval_required BOOLEAN;

UPDATE information_requests SET manager_approved = null, manager_approval_required = false;
UPDATE information_requests_history SET manager_approved = null, manager_approval_required = false;

# ---!Downs

-- non è necessaria una down