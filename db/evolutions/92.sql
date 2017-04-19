# ---!Ups

ALTER TABLE group_absence_types ADD COLUMN priority INTEGER NOT NULL DEFAULT 0;
ALTER TABLE group_absence_types_history ADD COLUMN priority INTEGER;
UPDATE group_absence_types_history SET priority = 0;

# ---!Downs

ALTER TABLE group_absence_types DROP COLUMN priority;
ALTER TABLE group_absence_types_history DROP COLUMN priority;