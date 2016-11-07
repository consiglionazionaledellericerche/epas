# ---!Ups

ALTER TABLE badge_readers_badge_systems_history DROP CONSTRAINT badge_readers_badge_systems_history_pkey;
ALTER TABLE badge_readers_badge_systems_history DROP COLUMN id;
ALTER TABLE badge_readers_badge_systems_history ADD PRIMARY KEY (_revision, _revision_type, badgereaders_id, badgesystems_id);

# ---!Downs

ALTER TABLE badge_readers_badge_systems_history ADD COLUMN id BIGINT;
