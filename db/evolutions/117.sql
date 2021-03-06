# ---!Ups

CREATE INDEX contracts_begin_date_idx ON contracts(begin_date);
CREATE INDEX contracts_end_contract_idx ON contracts(end_contract);
CREATE INDEX contracts_end_date_idx ON contracts(end_date);

CREATE INDEX contracts_working_time_types_begin_date_idx ON contracts_working_time_types(begin_date);
CREATE INDEX notifications_read_idx ON notifications(read);

CREATE INDEX person_days_date_idx ON person_days(date);

-- Generate automaticamente, vedi https://stackoverflow.com/questions/2970050/postgresql-how-to-index-all-foreign-keys

CREATE INDEX working_time_types_office_id_idx ON working_time_types (office_id);
CREATE INDEX person_shift_days_history__revision_idx ON person_shift_days_history (_revision);
CREATE INDEX absences_personday_id_idx ON absences (personday_id);
CREATE INDEX badge_readers_history__revision_id_idx ON badge_readers_history (_revision,id);
CREATE INDEX working_time_type_days_history__revision_idx ON working_time_type_days_history (_revision);
CREATE INDEX absence_troubles_history__revision_idx ON absence_troubles_history (_revision);
CREATE INDEX badge_systems_office_id_idx ON badge_systems (office_id);
CREATE INDEX competences_history__revision_idx ON competences_history (_revision);
CREATE INDEX badge_readers_user_id_idx ON badge_readers (user_id);
-- CREATE INDEX takable_codes_group_history_takable_behaviour_id__revision__revision_type_absence_types_id_idx ON takable_codes_group_history (takable_behaviour_id,_revision,_revision_type,absence_types_id);
CREATE INDEX users_history__revision_idx ON users_history (_revision);
CREATE INDEX absences_history__revision_idx ON absences_history (_revision);
CREATE INDEX users_office_owner_id_idx ON users (office_owner_id);
CREATE INDEX badges_badge_system_id_idx ON badges (badge_system_id);
CREATE INDEX contracts_working_time_types_contract_id_idx ON contracts_working_time_types (contract_id);
CREATE INDEX person_reperibility_types_supervisor_idx ON person_reperibility_types (supervisor);
CREATE INDEX badge_systems_history__revision_id_idx ON badge_systems_history (_revision,id);
CREATE INDEX absence_types_qualifications_history__revision_idx ON absence_types_qualifications_history (_revision);
CREATE INDEX institutes_history__revision_idx ON institutes_history (_revision);
CREATE INDEX takable_codes_group_absence_types_id_idx ON takable_codes_group (absence_types_id);
CREATE INDEX shift_type_shift_categories_id_idx ON shift_type (shift_categories_id);
CREATE INDEX contract_stamp_profiles_history__revision_idx ON contract_stamp_profiles_history (_revision);
CREATE INDEX badges_badge_reader_id_idx ON badges (badge_reader_id);
CREATE INDEX conf_general_office_id_field_idx ON conf_general (office_id,field);
CREATE INDEX absences_absence_type_id_idx ON absences (absence_type_id);
CREATE INDEX users_roles_offices_user_id_idx ON users_roles_offices (user_id);
CREATE INDEX person_shift_days_person_shift_id_idx ON person_shift_days (person_shift_id);
CREATE INDEX badge_readers_badge_systems_history__revision_idx ON badge_readers_badge_systems_history (_revision);
CREATE INDEX person_shift_days_history__revision_id__revision_type_idx ON person_shift_days_history (_revision,id,_revision_type);
CREATE INDEX absence_types_history__revision_idx ON absence_types_history (_revision);
CREATE INDEX shift_type_month_shift_type_id_year_month_idx ON shift_type_month (shift_type_id,year_month);
CREATE INDEX vacation_periods_contract_id_idx ON vacation_periods (contract_id);
CREATE INDEX contract_stamp_profiles_history__revision_id__revision_type_idx ON contract_stamp_profiles_history (_revision,id,_revision_type);
CREATE INDEX taken_codes_group_absence_types_id_idx ON taken_codes_group (absence_types_id);
-- CREATE INDEX complation_codes_group_history__revision_absence_types_id_complation_behaviour_id__revision_type_idx ON complation_codes_group_history (_revision,absence_types_id,complation_behaviour_id,_revision_type);
CREATE INDEX person_shift_shift_type_shifttypes_id_idx ON person_shift_shift_type (shifttypes_id);
CREATE INDEX stampings_personday_id_idx ON stampings (personday_id);
CREATE INDEX user_roles_history__revision_idx ON user_roles_history (_revision);
CREATE INDEX stampings_stamp_modification_type_id_idx ON stampings (stamp_modification_type_id);
CREATE INDEX shift_categories_history_supervisor_idx ON shift_categories_history (supervisor);
CREATE INDEX group_absence_types_history__revision_idx ON group_absence_types_history (_revision);
CREATE INDEX shift_categories_supervisor_idx ON shift_categories (supervisor);
CREATE INDEX absence_troubles_absence_id_idx ON absence_troubles (absence_id);
CREATE INDEX person_months_recap_history__revision_idx ON person_months_recap_history (_revision);
CREATE INDEX certificated_data_person_id_idx ON certificated_data (person_id);
CREATE INDEX users_roles_offices_role_id_idx ON users_roles_offices (role_id);
CREATE INDEX persons_competence_codes_person_id_idx ON persons_competence_codes (person_id);
CREATE INDEX replacing_codes_group_complation_behaviour_id_idx ON replacing_codes_group (complation_behaviour_id);
CREATE INDEX vacation_periods_history__revision_idx ON vacation_periods_history (_revision);
CREATE INDEX user_roles_user_id_idx ON user_roles (user_id);
CREATE INDEX persons_office_id_idx ON persons (office_id);
CREATE INDEX absence_types_qualifications_absencetypes_id_idx ON absence_types_qualifications (absencetypes_id);
CREATE INDEX person_months_recap_person_id_idx ON person_months_recap (person_id);
CREATE INDEX competence_codes_history__revision_idx ON competence_codes_history (_revision);
CREATE INDEX configurations_history__revision_id_idx ON configurations_history (_revision,id);
CREATE INDEX person_days_in_trouble_history__revision_idx ON person_days_in_trouble_history (_revision);
CREATE INDEX shift_cancelled_shift_type_id_idx ON shift_cancelled (shift_type_id);
CREATE INDEX meal_ticket_office_id_idx ON meal_ticket (office_id);
CREATE INDEX absence_types_justified_types_justified_types_id_idx ON absence_types_justified_types (justified_types_id);
CREATE INDEX persons_history__revision_idx ON persons_history (_revision);
CREATE INDEX absence_types_qualifications_qualifications_id_idx ON absence_types_qualifications (qualifications_id);
CREATE INDEX person_reperibility_days_date_person_reperibility_id_idx ON person_reperibility_days (date,person_reperibility_id);
CREATE INDEX stamp_modification_types_history__revision_id_idx ON stamp_modification_types_history (_revision,id);
-- CREATE INDEX absence_types_qualifications_history_qualifications_id__revision_absencetypes_id_idx ON absence_types_qualifications_history (qualifications_id,_revision,absencetypes_id);
CREATE INDEX absence_types_justified_types_absence_types_id_idx ON absence_types_justified_types (absence_types_id);
CREATE INDEX competences_competence_code_id_idx ON competences (competence_code_id);
CREATE INDEX meal_ticket_history__revision_idx ON meal_ticket_history (_revision);
CREATE INDEX zone_to_zones_history__revision_idx ON zone_to_zones_history (_revision);
CREATE INDEX conf_general_office_id_idx ON conf_general (office_id);
CREATE INDEX configurations_office_id_idx ON configurations (office_id);
CREATE INDEX group_absence_types_next_group_to_check_id_idx ON group_absence_types (next_group_to_check_id);
CREATE INDEX taken_codes_group_takable_behaviour_id_idx ON taken_codes_group (takable_behaviour_id);
CREATE INDEX working_time_types_history__revision_id_idx ON working_time_types_history (_revision,id);
CREATE INDEX conf_general_history__revision_idx ON conf_general_history (_revision);
CREATE INDEX shift_time_table_office_id_idx ON shift_time_table (office_id);
CREATE INDEX absence_types_justified_types_history__revision_idx ON absence_types_justified_types_history (_revision);
CREATE INDEX person_reperibility_types_office_id_idx ON person_reperibility_types (office_id);
CREATE INDEX users_roles_offices_history__revision_idx ON users_roles_offices_history (_revision);
CREATE INDEX person_days_history__revision_idx ON person_days_history (_revision);
CREATE INDEX conf_period_history__revision_id_idx ON conf_period_history (_revision,id);
CREATE INDEX person_configurations_history__revision_idx ON person_configurations_history (_revision);
CREATE INDEX revinfo_owner_id_idx ON revinfo (owner_id);
CREATE INDEX replacing_codes_group_absence_types_id_idx ON replacing_codes_group (absence_types_id);
CREATE INDEX replacing_codes_group_history__revision_idx ON replacing_codes_group_history (_revision);
CREATE INDEX contractual_references_history__revision_idx ON contractual_references_history (_revision);
CREATE INDEX contractual_clauses_history__revision_idx ON contractual_clauses_history (_revision);
CREATE INDEX users_roles_offices_office_id_idx ON users_roles_offices (office_id);
CREATE INDEX zone_to_zones_zone_linked_id_idx ON zone_to_zones (zone_linked_id);
CREATE INDEX contracts_working_time_types_working_time_type_id_idx ON contracts_working_time_types (working_time_type_id);
CREATE INDEX taken_codes_group_history__revision_idx ON taken_codes_group_history (_revision);
CREATE INDEX person_reperibility_person_reperibility_type_id_idx ON person_reperibility (person_reperibility_type_id);
CREATE INDEX certifications_history__revision_idx ON certifications_history (_revision);
CREATE INDEX reperibility_type_month_person_reperibility_type_id_idx ON reperibility_type_month (person_reperibility_type_id);
CREATE INDEX badge_readers_badge_systems_badgesystems_id_idx ON badge_readers_badge_systems (badgesystems_id);
CREATE INDEX person_reperibility_days_history__revision_idx ON person_reperibility_days_history (_revision);
CREATE INDEX person_reperibility_types_history__revision_idx ON person_reperibility_types_history (_revision);
CREATE INDEX justified_behaviours_history__revision_idx ON justified_behaviours_history (_revision);
CREATE INDEX justified_types_history__revision_idx ON justified_types_history (_revision);
CREATE INDEX zones_badge_reader_id_idx ON zones (badge_reader_id);
CREATE INDEX attachments_history__revision_id_idx ON attachments_history (_revision,id);
CREATE INDEX person_reperibility_types_persons_reperibilities_id_idx ON person_reperibility_types_persons (reperibilities_id);
CREATE INDEX category_tabs_history__revision_idx ON category_tabs_history (_revision);
-- CREATE INDEX contractual_clauses_contractual_references_history_contractual_references_id_idx ON contractual_clauses_contractual_references_history (contractual_references_id);
CREATE INDEX attachments_history__revision_idx ON attachments_history (_revision);
CREATE INDEX shift_categories_office_id_idx ON shift_categories (office_id);
CREATE INDEX notifications_recipient_id_idx ON notifications (recipient_id);
CREATE INDEX takable_codes_group_takable_behaviour_id_idx ON takable_codes_group (takable_behaviour_id);
CREATE INDEX roles_history__revision_id_idx ON roles_history (_revision,id);
CREATE INDEX competence_codes_competence_code_group_id_idx ON competence_codes (competence_code_group_id);
CREATE INDEX takable_absence_behaviours_history__revision_idx ON takable_absence_behaviours_history (_revision);
CREATE INDEX person_days_stamp_modification_type_id_idx ON person_days (stamp_modification_type_id);
CREATE INDEX person_reperibility_person_id_idx ON person_reperibility (person_id);
CREATE INDEX shift_categories_persons_history__revision_idx ON shift_categories_persons_history (_revision);
CREATE INDEX shift_type_month_history__revision_id__revision_type_idx ON shift_type_month_history (_revision,id,_revision_type);
CREATE INDEX competences_person_id_idx ON competences (person_id);
CREATE INDEX group_absence_types_complation_behaviour_id_idx ON group_absence_types (complation_behaviour_id);
CREATE INDEX user_roles_history__revision_type__revision_user_id_roles_idx ON user_roles_history (_revision_type,_revision,user_id,roles);
CREATE INDEX stampings_history__revision_idx ON stampings_history (_revision);
CREATE INDEX badge_systems_history__revision_idx ON badge_systems_history (_revision);
CREATE INDEX shift_categories_persons_categories_id_idx ON shift_categories_persons (categories_id);
-- CREATE INDEX contractual_clauses_contractual_references_contractual_references_id_idx ON contractual_clauses_contractual_references (contractual_references_id);
-- CREATE INDEX contractual_clauses_contractual_references_history_contractual_clauses_id_idx ON contractual_clauses_contractual_references_history (contractual_clauses_id);
CREATE INDEX total_overtime_office_id_idx ON total_overtime (office_id);
CREATE INDEX institutes_history__revision_id_idx ON institutes_history (_revision,id);
CREATE INDEX contract_month_recap_contract_id_idx ON contract_month_recap (contract_id);
CREATE INDEX time_variations_absence_id_idx ON time_variations (absence_id);
CREATE INDEX qualifications_history__revision_idx ON qualifications_history (_revision);
CREATE INDEX working_time_type_days_working_time_type_id_idx ON working_time_type_days (working_time_type_id);
CREATE INDEX category_group_absence_types_contractual_clause_id_idx ON category_group_absence_types (contractual_clause_id);
CREATE INDEX badge_readers_history__revision_idx ON badge_readers_history (_revision);
-- CREATE INDEX contractual_clauses_contractual_references_history__revision_idx ON contractual_clauses_contractual_references_history (_revision);
CREATE INDEX badges_history__revision_idx ON badges_history (_revision);
CREATE INDEX shift_type_shift_time_table_id_idx ON shift_type (shift_time_table_id);
CREATE INDEX absences_justified_type_id_idx ON absences (justified_type_id);
CREATE INDEX zones_history_badge_reader_id_idx ON zones_history (badge_reader_id);
CREATE INDEX complation_codes_group_history__revision_idx ON complation_codes_group_history (_revision);
-- CREATE INDEX complation_codes_group_complation_behaviour_id_absence_types_id_idx ON complation_codes_group (complation_behaviour_id,absence_types_id);
CREATE INDEX office_history__revision_idx ON office_history (_revision);
-- CREATE INDEX users_roles_offices_history__revision_type_id__revision_idx ON users_roles_offices_history (_revision_type,id,_revision);
CREATE INDEX shift_type_month_shift_type_id_idx ON shift_type_month (shift_type_id);
CREATE INDEX meal_ticket_contract_id_idx ON meal_ticket (contract_id);
CREATE INDEX vacation_periods_history__revision_id_idx ON vacation_periods_history (_revision,id);
CREATE INDEX conf_period_office_id_idx ON conf_period (office_id);
CREATE INDEX shift_type_month_history__revision_idx ON shift_type_month_history (_revision);
CREATE INDEX competence_codes_history__revision_id_idx ON competence_codes_history (_revision,id);
CREATE INDEX person_shift_day_in_trouble_person_shift_day_id_idx ON person_shift_day_in_trouble (person_shift_day_id);
CREATE INDEX person_children_person_id_idx ON person_children (person_id);
CREATE INDEX absence_types_justified_behaviours_absence_type_id_idx ON absence_types_justified_behaviours (absence_type_id);
CREATE INDEX person_days_person_id_idx ON person_days (person_id);
CREATE INDEX zone_to_zones_zone_base_id_idx ON zone_to_zones (zone_base_id);
CREATE INDEX reperibility_type_month_history__revision_idx ON reperibility_type_month_history (_revision);
CREATE INDEX reperibility_type_month_history_id__revision_type__revision_idx ON reperibility_type_month_history (id,_revision_type,_revision);
CREATE INDEX absence_types_history__revision_id_idx ON absence_types_history (_revision,id);
CREATE INDEX initialization_groups_group_absence_type_id_idx ON initialization_groups (group_absence_type_id);
CREATE INDEX complation_codes_group_absence_types_id_idx ON complation_codes_group (absence_types_id);
CREATE INDEX persons_person_in_charge_idx ON persons (person_in_charge);
CREATE INDEX shift_categories_persons_managers_id_idx ON shift_categories_persons (managers_id);
CREATE INDEX office_institute_id_idx ON office (institute_id);
CREATE INDEX group_absence_types_takable_behaviour_id_idx ON group_absence_types (takable_behaviour_id);
CREATE INDEX conf_general_history__revision_id_idx ON conf_general_history (_revision,id);
CREATE INDEX complation_absence_behaviours_history__revision_idx ON complation_absence_behaviours_history (_revision);
CREATE INDEX meal_ticket_admin_id_idx ON meal_ticket (admin_id);
CREATE INDEX conf_year_office_id_idx ON conf_year (office_id);
CREATE INDEX person_hour_for_overtime_person_id_idx ON person_hour_for_overtime (person_id);
CREATE INDEX working_time_types_history__revision_idx ON working_time_types_history (_revision);
CREATE INDEX person_shift_days_shift_type_id_idx ON person_shift_days (shift_type_id);
CREATE INDEX category_group_absence_types_history__revision_idx ON category_group_absence_types_history (_revision);
CREATE INDEX competence_code_groups_history__revision_idx ON competence_code_groups_history (_revision);
CREATE INDEX person_days_history__revision_id_idx ON person_days_history (_revision,id);
CREATE INDEX conf_period_history__revision_idx ON conf_period_history (_revision);
CREATE INDEX category_group_absence_types_category_tab_id_idx ON category_group_absence_types (category_tab_id);
CREATE INDEX persons_history__revision_id_idx ON persons_history (_revision,id);
CREATE INDEX person_reperibility_history__revision_idx ON person_reperibility_history (_revision);
CREATE INDEX person_reperibility_types_persons_history__revision_idx ON person_reperibility_types_persons_history (_revision);
CREATE INDEX attachments_office_id_idx ON attachments (office_id);
CREATE INDEX persons_competence_codes_competence_code_id_idx ON persons_competence_codes (competence_code_id);
CREATE INDEX time_variations_history__revision_idx ON time_variations_history (_revision);
CREATE INDEX configurations_history__revision_idx ON configurations_history (_revision);
-- CREATE INDEX replacing_codes_group_history__revision_type__revision_absence_types_id_complation_behaviour_id_idx ON replacing_codes_group_history (_revision_type,_revision,absence_types_id,complation_behaviour_id);
CREATE INDEX absence_types_justified_behaviours_history__revision_idx ON absence_types_justified_behaviours_history (_revision);
CREATE INDEX shift_categories_history__revision_idx ON shift_categories_history (_revision);
CREATE INDEX person_shift_day_in_trouble_history__revision_idx ON person_shift_day_in_trouble_history (_revision);
-- CREATE INDEX contractual_clauses_contractual_references_contractual_clauses_id_idx ON contractual_clauses_contractual_references (contractual_clauses_id);
CREATE INDEX person_shift_shift_type_personshifts_id_idx ON person_shift_shift_type (personshifts_id);
CREATE INDEX competences_competence_code_id_month_year_person_id_idx ON competences (competence_code_id,month,year,person_id);
CREATE INDEX stamp_modification_types_history__revision_idx ON stamp_modification_types_history (_revision);
CREATE INDEX person_days_in_trouble_personday_id_idx ON person_days_in_trouble (personday_id);
CREATE INDEX person_reperibility_days_reperibility_type_idx ON person_reperibility_days (reperibility_type);
CREATE INDEX complation_codes_group_complation_behaviour_id_idx ON complation_codes_group (complation_behaviour_id);
CREATE INDEX absence_types_justified_behaviours_justified_behaviour_id_idx ON absence_types_justified_behaviours (justified_behaviour_id);
-- CREATE INDEX badge_readers_badge_systems_history_badgesystems_id__revision__revision_type_badgereaders_id_idx ON badge_readers_badge_systems_history (badgesystems_id,_revision,_revision_type,badgereaders_id);
CREATE INDEX justified_behaviours_history__revision_id_idx ON justified_behaviours_history (_revision,id);
CREATE INDEX initialization_groups_history__revision_idx ON initialization_groups_history (_revision);
CREATE INDEX badges_person_id_idx ON badges (person_id);
CREATE INDEX person_children_history__revision_idx ON person_children_history (_revision);
CREATE INDEX person_days_date_person_id_idx ON person_days (date,person_id);
CREATE INDEX persons_qualification_id_idx ON persons (qualification_id);
CREATE INDEX roles_history__revision_idx ON roles_history (_revision);
CREATE INDEX badge_readers_badge_systems_badgereaders_id_idx ON badge_readers_badge_systems (badgereaders_id);
CREATE INDEX zones_history__revision_idx ON zones_history (_revision);
CREATE INDEX shift_type_history__revision_idx ON shift_type_history (_revision);
CREATE INDEX certificated_data_history__revision_idx ON certificated_data_history (_revision);
CREATE INDEX contracts_person_id_idx ON contracts (person_id);
CREATE INDEX person_reperibility_types_persons_managers_id_idx ON person_reperibility_types_persons (managers_id);
CREATE INDEX contracts_history__revision_idx ON contracts_history (_revision);
CREATE INDEX person_configurations_person_id_idx ON person_configurations (person_id);
CREATE INDEX conf_year_history__revision_idx ON conf_year_history (_revision);
CREATE INDEX certifications_person_id_idx ON certifications (person_id);
CREATE INDEX users_roles_offices_user_id_office_id_role_id_idx ON users_roles_offices (user_id,office_id,role_id);
CREATE INDEX contract_month_recap_contract_id_year_month_idx ON contract_month_recap (contract_id,year,month);
CREATE INDEX takable_codes_group_history__revision_idx ON takable_codes_group_history (_revision);
CREATE INDEX initialization_groups_person_id_idx ON initialization_groups (person_id);
CREATE INDEX certifications_history__revision_id_idx ON certifications_history (_revision,id);
CREATE INDEX person_reperibility_days_person_reperibility_id_idx ON person_reperibility_days (person_reperibility_id);
CREATE INDEX person_reperibility_types_history__revision_id_idx ON person_reperibility_types_history (_revision,id);
CREATE INDEX persons_user_id_idx ON persons (user_id);
--CREATE INDEX contractual_clauses_contractual_references_history_contractual_references_id__revision__revision_type_contractual_clauses_id_idx ON contractual_clauses_contractual_references_history (contractual_references_id,_revision,_revision_type,contractual_clauses_id);
CREATE INDEX group_absence_types_category_type_id_idx ON group_absence_types (category_type_id);
CREATE INDEX contract_stamp_profiles_contract_id_idx ON contract_stamp_profiles (contract_id);
CREATE INDEX time_variations_history__revision_id__revision_type_idx ON time_variations_history (_revision,id,_revision_type);


# ---!Downs

DROP INDEX contracts_begin_date_idx;
DROP INDEX contracts_end_contract_idx;
DROP INDEX contracts_end_date_idx;
DROP INDEX contracts_working_time_types_begin_date_idx;
DROP INDEX notifications_read_idx;
DROP INDEX person_days_date_idx;

-- generate automaticamente

DROP INDEX working_time_types_office_id_idx;
DROP INDEX person_shift_days_history__revision_idx;
DROP INDEX absences_personday_id_idx;
DROP INDEX badge_readers_history__revision_id_idx;
DROP INDEX working_time_type_days_history__revision_idx;
DROP INDEX absence_troubles_history__revision_idx;
DROP INDEX badge_systems_office_id_idx;
DROP INDEX competences_history__revision_idx;
DROP INDEX badge_readers_user_id_idx;
-- DROP INDEX takable_codes_group_history_takable_behaviour_id__revision__revision_type_absence_types_id_idx;
DROP INDEX users_history__revision_idx;
DROP INDEX absences_history__revision_idx;
DROP INDEX users_office_owner_id_idx;
DROP INDEX badges_badge_system_id_idx;
DROP INDEX contracts_working_time_types_contract_id_idx;
DROP INDEX person_reperibility_types_supervisor_idx;
DROP INDEX badge_systems_history__revision_id_idx;
DROP INDEX absence_types_qualifications_history__revision_idx;
DROP INDEX institutes_history__revision_idx;
DROP INDEX takable_codes_group_absence_types_id_idx;
DROP INDEX shift_type_shift_categories_id_idx;
DROP INDEX contract_stamp_profiles_history__revision_idx;
DROP INDEX badges_badge_reader_id_idx;
DROP INDEX conf_general_office_id_field_idx;
DROP INDEX absences_absence_type_id_idx;
DROP INDEX users_roles_offices_user_id_idx;
DROP INDEX person_shift_days_person_shift_id_idx;
DROP INDEX badge_readers_badge_systems_history__revision_idx;
DROP INDEX person_shift_days_history__revision_id__revision_type_idx;
DROP INDEX absence_types_history__revision_idx;
DROP INDEX shift_type_month_shift_type_id_year_month_idx;
DROP INDEX vacation_periods_contract_id_idx;
DROP INDEX contract_stamp_profiles_history__revision_id__revision_type_idx;
DROP INDEX taken_codes_group_absence_types_id_idx;
-- DROP INDEX complation_codes_group_history__revision_absence_types_id_complation_behaviour_id__revision_type_idx;
DROP INDEX person_shift_shift_type_shifttypes_id_idx;
DROP INDEX stampings_personday_id_idx;
DROP INDEX user_roles_history__revision_idx;
DROP INDEX stampings_stamp_modification_type_id_idx;
DROP INDEX shift_categories_history_supervisor_idx;
DROP INDEX group_absence_types_history__revision_idx;
DROP INDEX shift_categories_supervisor_idx;
DROP INDEX absence_troubles_absence_id_idx;
DROP INDEX person_months_recap_history__revision_idx;
DROP INDEX certificated_data_person_id_idx;
DROP INDEX users_roles_offices_role_id_idx;
DROP INDEX persons_competence_codes_person_id_idx;
DROP INDEX replacing_codes_group_complation_behaviour_id_idx;
DROP INDEX vacation_periods_history__revision_idx;
DROP INDEX user_roles_user_id_idx;
DROP INDEX persons_office_id_idx;
DROP INDEX absence_types_qualifications_absencetypes_id_idx;
DROP INDEX person_months_recap_person_id_idx;
DROP INDEX competence_codes_history__revision_idx;
DROP INDEX configurations_history__revision_id_idx;
DROP INDEX person_days_in_trouble_history__revision_idx;
DROP INDEX shift_cancelled_shift_type_id_idx;
DROP INDEX meal_ticket_office_id_idx;
DROP INDEX absence_types_justified_types_justified_types_id_idx;
DROP INDEX persons_history__revision_idx;
DROP INDEX absence_types_qualifications_qualifications_id_idx;
DROP INDEX person_reperibility_days_date_person_reperibility_id_idx;
DROP INDEX stamp_modification_types_history__revision_id_idx;
-- DROP INDEX absence_types_qualifications_history_qualifications_id__revision_absencetypes_id_idx;
DROP INDEX absence_types_justified_types_absence_types_id_idx;
DROP INDEX competences_competence_code_id_idx;
DROP INDEX meal_ticket_history__revision_idx;
DROP INDEX zone_to_zones_history__revision_idx;
DROP INDEX conf_general_office_id_idx;
DROP INDEX configurations_office_id_idx;
DROP INDEX group_absence_types_next_group_to_check_id_idx;
DROP INDEX taken_codes_group_takable_behaviour_id_idx;
DROP INDEX working_time_types_history__revision_id_idx;
DROP INDEX conf_general_history__revision_idx;
DROP INDEX shift_time_table_office_id_idx;
DROP INDEX absence_types_justified_types_history__revision_idx;
DROP INDEX person_reperibility_types_office_id_idx;
DROP INDEX users_roles_offices_history__revision_idx;
DROP INDEX person_days_history__revision_idx;
DROP INDEX conf_period_history__revision_id_idx;
DROP INDEX person_configurations_history__revision_idx;
DROP INDEX revinfo_owner_id_idx;
DROP INDEX replacing_codes_group_absence_types_id_idx;
DROP INDEX replacing_codes_group_history__revision_idx;
DROP INDEX contractual_references_history__revision_idx;
DROP INDEX contractual_clauses_history__revision_idx;
DROP INDEX users_roles_offices_office_id_idx;
DROP INDEX zone_to_zones_zone_linked_id_idx;
DROP INDEX contracts_working_time_types_working_time_type_id_idx;
DROP INDEX taken_codes_group_history__revision_idx;
DROP INDEX person_reperibility_person_reperibility_type_id_idx;
DROP INDEX certifications_history__revision_idx;
DROP INDEX reperibility_type_month_person_reperibility_type_id_idx;
DROP INDEX badge_readers_badge_systems_badgesystems_id_idx;
DROP INDEX person_reperibility_days_history__revision_idx;
DROP INDEX person_reperibility_types_history__revision_idx;
DROP INDEX justified_behaviours_history__revision_idx;
DROP INDEX justified_types_history__revision_idx;
DROP INDEX zones_badge_reader_id_idx;
DROP INDEX attachments_history__revision_id_idx;
DROP INDEX person_reperibility_types_persons_reperibilities_id_idx;
DROP INDEX category_tabs_history__revision_idx;
-- DROP INDEX contractual_clauses_contractual_references_history_contractual_references_id_idx;
DROP INDEX attachments_history__revision_idx;
DROP INDEX shift_categories_office_id_idx;
DROP INDEX notifications_recipient_id_idx;
DROP INDEX takable_codes_group_takable_behaviour_id_idx;
DROP INDEX roles_history__revision_id_idx;
DROP INDEX competence_codes_competence_code_group_id_idx;
DROP INDEX takable_absence_behaviours_history__revision_idx;
DROP INDEX person_days_stamp_modification_type_id_idx;
DROP INDEX person_reperibility_person_id_idx;
DROP INDEX shift_categories_persons_history__revision_idx;
DROP INDEX shift_type_month_history__revision_id__revision_type_idx;
DROP INDEX competences_person_id_idx;
DROP INDEX group_absence_types_complation_behaviour_id_idx;
DROP INDEX user_roles_history__revision_type__revision_user_id_roles_idx;
DROP INDEX stampings_history__revision_idx;
DROP INDEX badge_systems_history__revision_idx;
DROP INDEX shift_categories_persons_categories_id_idx;
-- DROP INDEX contractual_clauses_contractual_references_contractual_references_id_idx;
-- DROP INDEX contractual_clauses_contractual_references_history_contractual_clauses_id_idx;
DROP INDEX total_overtime_office_id_idx;
DROP INDEX institutes_history__revision_id_idx;
DROP INDEX contract_month_recap_contract_id_idx;
DROP INDEX time_variations_absence_id_idx;
DROP INDEX qualifications_history__revision_idx;
DROP INDEX working_time_type_days_working_time_type_id_idx;
DROP INDEX category_group_absence_types_contractual_clause_id_idx;
DROP INDEX badge_readers_history__revision_idx;
-- DROP INDEX contractual_clauses_contractual_references_history__revision_idx;
DROP INDEX badges_history__revision_idx;
DROP INDEX shift_type_shift_time_table_id_idx;
DROP INDEX absences_justified_type_id_idx;
DROP INDEX zones_history_badge_reader_id_idx;
DROP INDEX complation_codes_group_history__revision_idx;
-- DROP INDEX complation_codes_group_complation_behaviour_id_absence_types_id_idx;
DROP INDEX office_history__revision_idx;
-- DROP INDEX users_roles_offices_history__revision_type_id__revision_idx;
DROP INDEX shift_type_month_shift_type_id_idx;
DROP INDEX meal_ticket_contract_id_idx;
DROP INDEX vacation_periods_history__revision_id_idx;
DROP INDEX conf_period_office_id_idx;
DROP INDEX shift_type_month_history__revision_idx;
DROP INDEX competence_codes_history__revision_id_idx;
DROP INDEX person_shift_day_in_trouble_person_shift_day_id_idx;
DROP INDEX person_children_person_id_idx;
DROP INDEX absence_types_justified_behaviours_absence_type_id_idx;
DROP INDEX person_days_person_id_idx;
DROP INDEX zone_to_zones_zone_base_id_idx;
DROP INDEX reperibility_type_month_history__revision_idx;
DROP INDEX reperibility_type_month_history_id__revision_type__revision_idx;
DROP INDEX absence_types_history__revision_id_idx;
DROP INDEX initialization_groups_group_absence_type_id_idx;
DROP INDEX complation_codes_group_absence_types_id_idx;
DROP INDEX persons_person_in_charge_idx;
DROP INDEX shift_categories_persons_managers_id_idx;
DROP INDEX office_institute_id_idx;
DROP INDEX group_absence_types_takable_behaviour_id_idx;
DROP INDEX conf_general_history__revision_id_idx;
DROP INDEX complation_absence_behaviours_history__revision_idx;
DROP INDEX meal_ticket_admin_id_idx;
DROP INDEX conf_year_office_id_idx;
DROP INDEX person_hour_for_overtime_person_id_idx;
DROP INDEX working_time_types_history__revision_idx;
DROP INDEX person_shift_days_shift_type_id_idx;
DROP INDEX category_group_absence_types_history__revision_idx;
DROP INDEX competence_code_groups_history__revision_idx;
DROP INDEX person_days_history__revision_id_idx;
DROP INDEX conf_period_history__revision_idx;
DROP INDEX category_group_absence_types_category_tab_id_idx;
DROP INDEX persons_history__revision_id_idx;
DROP INDEX person_reperibility_history__revision_idx;
DROP INDEX person_reperibility_types_persons_history__revision_idx;
DROP INDEX attachments_office_id_idx;
DROP INDEX persons_competence_codes_competence_code_id_idx;
DROP INDEX time_variations_history__revision_idx;
DROP INDEX configurations_history__revision_idx;
-- DROP INDEX replacing_codes_group_history__revision_type__revision_absence_types_id_complation_behaviour_id_idx;
DROP INDEX absence_types_justified_behaviours_history__revision_idx;
DROP INDEX shift_categories_history__revision_idx;
DROP INDEX person_shift_day_in_trouble_history__revision_idx;
-- DROP INDEX contractual_clauses_contractual_references_contractual_clauses_id_idx;
DROP INDEX person_shift_shift_type_personshifts_id_idx;
DROP INDEX competences_competence_code_id_month_year_person_id_idx;
DROP INDEX stamp_modification_types_history__revision_idx;
DROP INDEX person_days_in_trouble_personday_id_idx;
DROP INDEX person_reperibility_days_reperibility_type_idx;
DROP INDEX complation_codes_group_complation_behaviour_id_idx;
DROP INDEX absence_types_justified_behaviours_justified_behaviour_id_idx;
-- DROP INDEX badge_readers_badge_systems_history_badgesystems_id__revision__revision_type_badgereaders_id_idx;
DROP INDEX justified_behaviours_history__revision_id_idx;
DROP INDEX initialization_groups_history__revision_idx;
DROP INDEX badges_person_id_idx;
DROP INDEX person_children_history__revision_idx;
DROP INDEX person_days_date_person_id_idx;
DROP INDEX persons_qualification_id_idx;
DROP INDEX roles_history__revision_idx;
DROP INDEX badge_readers_badge_systems_badgereaders_id_idx;
DROP INDEX zones_history__revision_idx;
DROP INDEX shift_type_history__revision_idx;
DROP INDEX certificated_data_history__revision_idx;
DROP INDEX contracts_person_id_idx;
DROP INDEX person_reperibility_types_persons_managers_id_idx;
DROP INDEX contracts_history__revision_idx;
DROP INDEX person_configurations_person_id_idx;
DROP INDEX conf_year_history__revision_idx;
DROP INDEX certifications_person_id_idx;
DROP INDEX users_roles_offices_user_id_office_id_role_id_idx;
DROP INDEX contract_month_recap_contract_id_year_month_idx;
DROP INDEX takable_codes_group_history__revision_idx;
DROP INDEX initialization_groups_person_id_idx;
DROP INDEX certifications_history__revision_id_idx;
DROP INDEX person_reperibility_days_person_reperibility_id_idx;
DROP INDEX person_reperibility_types_history__revision_id_idx;
DROP INDEX persons_user_id_idx;
-- DROP INDEX contractual_clauses_contractual_references_history_contractual_references_id__revision__revision_type_contractual_clauses_id_idx;
DROP INDEX group_absence_types_category_type_id_idx;
DROP INDEX contract_stamp_profiles_contract_id_idx;
DROP INDEX time_variations_history__revision_id__revision_type_idx;
