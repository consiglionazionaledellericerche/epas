# ---!Ups

ALTER TABLE information_requests ADD COLUMN manager_approved TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE information_requests ADD COLUMN manager_approval_required BOOLEAN;

ALTER TABLE information_requests_history ADD COLUMN manager_approved TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE information_requests_history ADD COLUMN manager_approval_required BOOLEAN;

# ---!Downs

-- non è necessaria una down