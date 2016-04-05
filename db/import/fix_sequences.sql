SELECT SETVAL('public.contract_stamp_profiles_id_seq', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.contract_stamp_profiles;
SELECT SETVAL('public.meal_ticket_id_seq', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.meal_ticket;
SELECT SETVAL('public.roles_id_seq', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.roles;
SELECT SETVAL('public.seq_absence_type_groups', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.absence_type_groups;
SELECT SETVAL('public.seq_absence_types', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.absence_types;
SELECT SETVAL('public.seq_absences', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.absences;
SELECT SETVAL('public.seq_badge_readers', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.badge_readers;
SELECT SETVAL('public.seq_certificated_data', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.certificated_data;
SELECT SETVAL('public.seq_competence_codes', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.competence_codes;
SELECT SETVAL('public.seq_competences', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.competences;
SELECT SETVAL('public.seq_conf_general', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.conf_general;
SELECT SETVAL('public.seq_conf_year', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.conf_year;
SELECT SETVAL('public.seq_contracts', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.contracts;
SELECT SETVAL('public.seq_contracts_working_time_types', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.contracts_working_time_types;
SELECT SETVAL('public.seq_office', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.office;
SELECT SETVAL('public.seq_person_children', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.person_children;
SELECT SETVAL('public.seq_person_days', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.person_days;
SELECT SETVAL('public.seq_person_days_in_trouble', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.person_days_in_trouble;
SELECT SETVAL('public.seq_person_hour_for_overtime', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.person_hour_for_overtime;
SELECT SETVAL('public.seq_person_months_recap', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.person_months_recap;
SELECT SETVAL('public.seq_persons', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.persons;
SELECT SETVAL('public.seq_persons_working_time_types', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.persons_working_time_types;
SELECT SETVAL('public.seq_qualifications', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.qualifications;
SELECT SETVAL('public.seq_revinfo', COALESCE(MAX(rev), 1) ) FROM public.revinfo;
SELECT SETVAL('public.seq_shift_time_table', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.shift_time_table;
SELECT SETVAL('public.seq_stamp_modification_types', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.stamp_modification_types;
SELECT SETVAL('public.seq_stampings', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.stampings;
SELECT SETVAL('public.seq_total_overtime', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.total_overtime;
SELECT SETVAL('public.seq_users', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.users;
SELECT SETVAL('public.seq_vacation_periods', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.vacation_periods;
SELECT SETVAL('public.seq_working_time_type_days', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.working_time_type_days;
SELECT SETVAL('public.seq_working_time_types', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.working_time_types;
SELECT SETVAL('public.shift_categories_id_seq', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.shift_categories;
SELECT SETVAL('public.users_roles_offices_id_seq', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM public.users_roles_offices;
