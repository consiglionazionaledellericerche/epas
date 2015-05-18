# ---!Ups

-- elimina le sequenze inutili

DROP SEQUENCE seq_auth_users;
DROP SEQUENCE seq_configurations;
DROP SEQUENCE seq_groups;
DROP SEQUENCE seq_options;
DROP SEQUENCE seq_users_permissions_offices;
DROP SEQUENCE seq_web_stamping_address;
DROP SEQUENCE seq_year_recaps;
DROP SEQUENCE seq_valuable_competences;

-- riassegna le sequenze alla tabella proprietaria

ALTER SEQUENCE seq_permissions OWNED BY permissions.id;
ALTER SEQUENCE seq_total_overtime OWNED BY total_overtime.id;
ALTER SEQUENCE seq_person_children OWNED BY person_children.id;
ALTER SEQUENCE seq_vacation_codes OWNED BY vacation_codes.id;
ALTER SEQUENCE seq_users OWNED BY users.id;
ALTER SEQUENCE seq_persons OWNED BY persons.id;
ALTER SEQUENCE meal_ticket_id_seq OWNED BY meal_ticket.id;
ALTER SEQUENCE seq_person_days_in_trouble OWNED BY person_days_in_trouble.id;
ALTER SEQUENCE seq_contracts_working_time_types OWNED BY contracts_working_time_types.id;
ALTER SEQUENCE seq_stampings OWNED BY stampings.id;
ALTER SEQUENCE seq_working_time_types OWNED BY working_time_types.id;
ALTER SEQUENCE seq_certificated_data OWNED BY certificated_data.id;
ALTER SEQUENCE seq_office OWNED BY office.id;
ALTER SEQUENCE seq_contracts OWNED BY contracts.id;
ALTER SEQUENCE seq_person_years OWNED BY person_years.id;
ALTER SEQUENCE seq_revinfo OWNED BY revinfo.rev;
ALTER SEQUENCE seq_person_days OWNED BY person_days.id;
ALTER SEQUENCE users_roles_offices_id_seq OWNED BY users_roles_offices.id;
ALTER SEQUENCE shift_categories_id_seq OWNED BY shift_categories.id;
ALTER SEQUENCE seq_stamp_profiles OWNED BY stamp_profiles.id;
ALTER SEQUENCE seq_initialization_absences OWNED BY initialization_absences.id;
ALTER SEQUENCE seq_vacation_periods OWNED BY vacation_periods.id;
ALTER SEQUENCE seq_badge_readers OWNED BY badge_readers.id;
ALTER SEQUENCE seq_competence_codes OWNED BY competence_codes.id;
ALTER SEQUENCE seq_qualifications OWNED BY qualifications.id;
ALTER SEQUENCE roles_id_seq OWNED BY roles.id;
ALTER SEQUENCE seq_contract_year_recap OWNED BY contract_year_recap.id;
ALTER SEQUENCE seq_working_time_type_days OWNED BY working_time_type_days.id;
ALTER SEQUENCE seq_conf_general OWNED BY conf_general.id;
ALTER SEQUENCE seq_stamp_types OWNED BY stamp_types.id;
ALTER SEQUENCE seq_initialization_times OWNED BY initialization_times.id;
ALTER SEQUENCE contract_stamp_profiles_id_seq OWNED BY contract_stamp_profiles.id;
ALTER SEQUENCE seq_shift_time_table OWNED BY shift_time_table.id;
ALTER SEQUENCE seq_absence_type_groups OWNED BY absence_type_groups.id;
ALTER SEQUENCE seq_competences OWNED BY competences.id;
ALTER SEQUENCE seq_person_hour_for_overtime OWNED BY person_hour_for_overtime.id;
ALTER SEQUENCE seq_persons_working_time_types OWNED BY persons_working_time_types.id;
ALTER SEQUENCE seq_person_months_recap OWNED BY person_months_recap.id;
ALTER SEQUENCE seq_stamp_modification_types OWNED BY stamp_modification_types.id;
ALTER SEQUENCE seq_absence_types OWNED BY absence_types.id;
ALTER SEQUENCE seq_conf_year OWNED BY conf_year.id;
ALTER SEQUENCE seq_absences OWNED BY absences.id;

# ---!Downs
