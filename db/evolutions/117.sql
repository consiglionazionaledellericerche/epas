# ---!Ups

CREATE INDEX absences_justified_type_id_key ON absences(justified_type_id);
CREATE INDEX absences_absence_type_id_key ON absences(absence_type_id);
CREATE INDEX absences_personday_id_key ON absences(personday_id);

CREATE INDEX badge_readers_user_id_key ON badge_readers(user_id);

CREATE INDEX certifications_person_id_key ON certification(person_id);

CREATE INDEX contracts_person_id_key ON contracts(person_id);
CREATE INDEX contracts_begin_date_key ON contracts(begin_date);
CREATE INDEX contracts_end_contract_key ON contracts(end_contract);
CREATE INDEX contracts_end_date_key ON contracts(end_date);

CREATE INDEX contract_month_recap_contract_id_key ON contract_month_recap(contract_id);

CREATE INDEX contract_stamp_profiles_contract_id_key ON contract_stamp_profiles(contract_id);

CREATE INDEX contracts_working_time_types_begin_date_key ON contracts_working_time_types(begin_date);
CREATE INDEX contracts_working_time_types_contract_id_key ON contracts_working_time_types(contract_id);
CREATE INDEX contracts_working_time_types_working_time_type_id_key ON contracts_working_time_types(working_time_type_id);

CREATE INDEX meal_ticket_admin_id_key ON meal_ticket(admin_id);
CREATE INDEX meal_ticket_contract_id_key ON meal_ticket(contract_id);
CREATE INDEX meal_ticket_office_id_key ON meal_ticket(office_id);

CREATE INDEX notifications_recipient_id_key ON notifications(recipient_id);
CREATE INDEX notifications_read_key ON notifications(read);

CREATE INDEX users_office_owner_id_key ON users(office_owner_id);

CREATE INDEX persons_office_id_key ON persons(office_id);
CREATE INDEX persons_user_id_key ON persons(user_id);
CREATE INDEX persons_qualification_id_key ON persons(qualification_id);
CREATE INDEX persons_person_in_charge_key ON persons(person_in_charge);

CREATE INDEX person_configurations_person_id_key ON person_configurations(person_id);

CREATE INDEX person_days_in_trouble_personday_id_key ON person_days_in_trouble(personday_id);

CREATE INDEX person_days_date_key ON person_days(date);
CREATE INDEX person_days_person_id_key ON person_days(person_id);
CREATE INDEX person_days_stamp_modification_type_id_key ON person_days(stamp_modification_type_id);

CREATE INDEX person_hour_for_overtime_person_id_key ON person_hour_for_overtime(person_id);

CREATE INDEX stampings_personday_id_key ON stampings(personday_id);
CREATE INDEX stampings_stamp_modification_type_id_key ON stampings(stamp_modification_type_id);

CREATE INDEX vacation_periods_contract_id_key ON vacation_periods(contract_id);


-- CREATE INDEX _key ON ();
-- CREATE INDEX _key ON ();
-- CREATE INDEX _key ON ();

# ---!Downs

DROP INDEX absences_justified_type_id_key;
DROP INDEX absences_absence_type_id_key;
DROP INDEX absences_personday_id_key;

DROP INDEX badge_readers_user_id_key;

DROP INDEX certifications_person_id_key;

DROP INDEX contracts_person_id_key;
DROP INDEX contracts_begin_date_key;
DROP INDEX contracts_end_contract_key;
DROP INDEX contracts_end_date_key;

DROP INDEX contract_month_recap_contract_id_key;
DROP INDEX contract_stamp_profiles_contract_id_key;

DROP INDEX contracts_working_time_types_begin_date_key;
DROP INDEX contracts_working_time_types_contract_id_key;
DROP INDEX contracts_working_time_types_working_time_type_id_key;

DROP INDEX meal_ticket_admin_id_key;
DROP INDEX meal_ticket_contract_id_key;
DROP INDEX meal_ticket_office_id_key;

DROP INDEX notifications_recipient_id_key;
DROP INDEX notifications_read_key;

DROP INDEX persons_office_id_key;
DROP INDEX persons_user_id_key;
DROP INDEX persons_qualification_id_key;
DROP INDEX persons_person_in_charge_key;

DROP INDEX person_configurations_person_id_key;

DROP INDEX person_days_in_trouble_personday_id_key;

DROP INDEX person_days_date_key;
DROP INDEX person_days_person_id_key;
DROP INDEX person_days_stamp_modification_type_id_key;

DROP INDEX person_hour_for_overtime_person_id_key;

DROP INDEX stampings_personday_id_key;
DROP INDEX stampings_stamp_modification_type_id_key;

DROP INDEX users_office_owner_id_key;

DROP INDEX vacation_periods_contract_id_key;

