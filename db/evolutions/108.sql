# ---!Ups

ALTER TABLE absence_troubles ADD COLUMN version INT DEFAULT 0;
ALTER TABLE absence_types ADD COLUMN version INT DEFAULT 0;
ALTER TABLE absences ADD COLUMN version INT DEFAULT 0;
ALTER TABLE attachments ADD COLUMN version INT DEFAULT 0;
ALTER TABLE badge_readers ADD COLUMN version INT DEFAULT 0;
ALTER TABLE badge_systems ADD COLUMN version INT DEFAULT 0;
ALTER TABLE badges ADD COLUMN version INT DEFAULT 0;
ALTER TABLE category_group_absence_types ADD COLUMN version INT DEFAULT 0;
ALTER TABLE category_tabs ADD COLUMN version INT DEFAULT 0;
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
ALTER TABLE contracts ADD COLUMN version INT DEFAULT 0;
ALTER TABLE contracts_working_time_types ADD COLUMN version INT DEFAULT 0;
ALTER TABLE group_absence_types ADD COLUMN version INT DEFAULT 0;
ALTER TABLE initialization_groups ADD COLUMN version INT DEFAULT 0;
ALTER TABLE institutes ADD COLUMN version INT DEFAULT 0;
ALTER TABLE justified_types ADD COLUMN version INT DEFAULT 0;
ALTER TABLE meal_ticket ADD COLUMN version INT DEFAULT 0;
ALTER TABLE office ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_children ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_configurations ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_days ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_days_in_trouble ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_hour_for_overtime ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_months_recap ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_reperibility ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_reperibility_days ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_reperibility_types ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_reperibility_types_persons ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_shift ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_shift_day_in_trouble ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_shift_days ADD COLUMN version INT DEFAULT 0;
ALTER TABLE person_shift_shift_type ADD COLUMN version INT DEFAULT 0;
ALTER TABLE persons_competence_codes ADD COLUMN version INT DEFAULT 0;
ALTER TABLE qualifications ADD COLUMN version INT DEFAULT 0;
ALTER TABLE replacing_codes_group ADD COLUMN version INT DEFAULT 0;
ALTER TABLE roles ADD COLUMN version INT DEFAULT 0;
ALTER TABLE shift_cancelled ADD COLUMN version INT DEFAULT 0;
ALTER TABLE shift_categories ADD COLUMN version INT DEFAULT 0;
ALTER TABLE shift_categories_persons ADD COLUMN version INT DEFAULT 0;
ALTER TABLE shift_time_table ADD COLUMN version INT DEFAULT 0;
ALTER TABLE shift_type ADD COLUMN version INT DEFAULT 0;
ALTER TABLE stamp_modification_types ADD COLUMN version INT DEFAULT 0;
ALTER TABLE stampings ADD COLUMN version INT DEFAULT 0;
ALTER TABLE takable_absence_behaviours ADD COLUMN version INT DEFAULT 0;
ALTER TABLE takable_codes_group ADD COLUMN version INT DEFAULT 0;
ALTER TABLE taken_codes_group ADD COLUMN version INT DEFAULT 0;
ALTER TABLE total_overtime ADD COLUMN version INT DEFAULT 0;
ALTER TABLE users ADD COLUMN version INT DEFAULT 0;
ALTER TABLE users_roles_offices ADD COLUMN version INT DEFAULT 0;
ALTER TABLE vacation_periods ADD COLUMN version INT DEFAULT 0;
ALTER TABLE working_time_type_days ADD COLUMN version INT DEFAULT 0;
ALTER TABLE working_time_types ADD COLUMN version INT DEFAULT 0;
ALTER TABLE zone_to_zones ADD COLUMN version INT DEFAULT 0;
ALTER TABLE zones ADD COLUMN version INT DEFAULT 0;

UPDATE notifications SET version = 0 WHERE version IS NULL;
ALTER TABLE notifications ALTER COLUMN version SET DEFAULT 0;

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
ALTER TABLE contracts DROP COLUMN version;
ALTER TABLE contracts_working_time_types DROP COLUMN version;
ALTER TABLE group_absence_types DROP COLUMN version;
ALTER TABLE initialization_groups DROP COLUMN version;
ALTER TABLE institutes DROP COLUMN version;
ALTER TABLE justified_types DROP COLUMN version;
ALTER TABLE meal_ticket DROP COLUMN version;
ALTER TABLE office DROP COLUMN version;
ALTER TABLE person_children DROP COLUMN version;
ALTER TABLE person_configurations DROP COLUMN version;
ALTER TABLE person_days DROP COLUMN version;
ALTER TABLE person_days_in_trouble DROP COLUMN version;
ALTER TABLE person_hour_for_overtime DROP COLUMN version;
ALTER TABLE person_months_recap DROP COLUMN version;
ALTER TABLE person_reperibility DROP COLUMN version;
ALTER TABLE person_reperibility_days DROP COLUMN version;
ALTER TABLE person_reperibility_types DROP COLUMN version;
ALTER TABLE person_reperibility_types_persons DROP COLUMN version;
ALTER TABLE person_shift DROP COLUMN version;
ALTER TABLE person_shift_day_in_trouble DROP COLUMN version;
ALTER TABLE person_shift_days DROP COLUMN version;
ALTER TABLE person_shift_shift_type DROP COLUMN version;
ALTER TABLE persons_competence_codes DROP COLUMN version;
ALTER TABLE qualifications DROP COLUMN version;
ALTER TABLE replacing_codes_group DROP COLUMN version;
ALTER TABLE roles DROP COLUMN version;
ALTER TABLE shift_cancelled DROP COLUMN version;
ALTER TABLE shift_categories DROP COLUMN version;
ALTER TABLE shift_categories_persons DROP COLUMN version;
ALTER TABLE shift_time_table DROP COLUMN version;
ALTER TABLE shift_type DROP COLUMN version;
ALTER TABLE stamp_modification_types DROP COLUMN version;
ALTER TABLE stampings DROP COLUMN version;
ALTER TABLE takable_absence_behaviours DROP COLUMN version;
ALTER TABLE takable_codes_group DROP COLUMN version;
ALTER TABLE taken_codes_group DROP COLUMN version;
ALTER TABLE total_overtime DROP COLUMN version;
ALTER TABLE users DROP COLUMN version;
ALTER TABLE users_roles_offices DROP COLUMN version;
ALTER TABLE vacation_periods DROP COLUMN version;
ALTER TABLE working_time_type_days DROP COLUMN version;
ALTER TABLE working_time_types DROP COLUMN version;
ALTER TABLE zone_to_zones DROP COLUMN version;
ALTER TABLE zones DROP COLUMN version;

