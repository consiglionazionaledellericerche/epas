# ---!Ups

ALTER TABLE absence_troubles ADD COLUMN version INT DEFAULT 0;
ALTER TABLE absence_types ADD COLUMN version INT DEFAULT 0;
ALTER TABLE absences ADD COLUMN version INT DEFAULT 0;
ALTER TABLE attachments ADD COLUMN version INT DEFAULT 0;
ALTER TABLE badge_readers ADD COLUMN version INT DEFAULT 0;
ALTER TABLE badge_systems ADD COLUMN version INT DEFAULT 0;
ALTER TABLE badges ADD COLUMN version INT DEFAULT 0;
ALTER TABLE category_group_absence_types ADD COLUMN version INT DEFAULT 0;
ALTER TABLE certificated_data ADD COLUMN version INT DEFAULT 0;
ALTER TABLE certifications ADD COLUMN version INT DEFAULT 0;
ALTER TABLE competence_code_groups ADD COLUMN version INT DEFAULT 0;
ALTER TABLE competence_codes ADD COLUMN version INT DEFAULT 0;
ALTER TABLE competences ADD COLUMN version INT DEFAULT 0;
ALTER TABLE complation_absence_behaviours ADD COLUMN version INT DEFAULT 0;
ALTER TABLE conf_general ADD COLUMN version INT DEFAULT 0;
ALTER TABLE conf_period ADD COLUMN version INT DEFAULT 0;
ALTER TABLE conf_year ADD COLUMN version INT DEFAULT 0;
ALTER TABLE configurations ADD COLUMN version INT DEFAULT 0;
ALTER TABLE contract_month_recap ADD COLUMN version INT DEFAULT 0;
ALTER TABLE contract_stamp_profiles ADD COLUMN version INT DEFAULT 0;

# ---!Downs

ALTER TABLE absence_troubles DROP COLUMN version;
ALTER TABLE absence_types DROP COLUMN version;
ALTER TABLE absences DROP COLUMN version;
ALTER TABLE attachments DROP COLUMN version;
ALTER TABLE badge_readers DROP COLUMN version;
ALTER TABLE badge_systems DROP COLUMN version;
ALTER TABLE badges DROP COLUMN version;
ALTER TABLE category_group_absence_types DROP COLUMN version;
ALTER TABLE category_tabs DROP COLUMN version;
ALTER TABLE certificated_data DROP COLUMN version;
ALTER TABLE certifications DROP COLUMN version;
ALTER TABLE competence_code_groups DROP COLUMN version;
ALTER TABLE competence_codes DROP COLUMN version;
ALTER TABLE competences DROP COLUMN version;
ALTER TABLE complation_absence_behaviours DROP COLUMN version;
ALTER TABLE conf_general DROP COLUMN version;
ALTER TABLE conf_period DROP COLUMN version;
ALTER TABLE conf_year DROP COLUMN version;
ALTER TABLE configurations DROP COLUMN version;
ALTER TABLE contract_month_recap DROP COLUMN version;
ALTER TABLE contract_stamp_profiles DROP COLUMN version;

