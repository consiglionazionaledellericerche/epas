# ---!Ups

ALTER TABLE badge_readers_badge_systems_history DROP CONSTRAINT badge_readers_badge_systems_history_pkey;
ALTER TABLE badge_readers_badge_systems_history DROP COLUMN IF EXISTS id;
ALTER TABLE badge_readers_badge_systems_history ADD PRIMARY KEY (_revision, _revision_type, badgereaders_id, badgesystems_id);

# ---!Downs

