# ---!Ups
-- Adattamento alle convenzioni di naming di spring-boot.

ALTER TABLE absence_types_qualifications RENAME COLUMN absencetypes_id TO absence_types_id;
ALTER TABLE absence_types_qualifications_history RENAME COLUMN absencetypes_id TO absence_types_id;

ALTER TABLE absences RENAME COLUMN personday_id TO person_day_id;
ALTER TABLE absences_history RENAME COLUMN personday_id TO person_day_id;

ALTER TABLE badge_readers_badge_systems RENAME COLUMN badgereaders_id TO badge_readers_id;
ALTER TABLE badge_readers_badge_systems_history RENAME COLUMN badgereaders_id TO badge_readers_id;

ALTER TABLE badge_readers_badge_systems RENAME COLUMN badgesystems_id TO badge_systems_id;
ALTER TABLE badge_readers_badge_systems_history RENAME COLUMN badgesystems_id TO badge_systems_id;

ALTER TABLE competence_codes RENAME COLUMN codeToPresence TO code_to_presence;
ALTER TABLE competence_codes_history RENAME COLUMN codeToPresence TO code_to_presence;

ALTER TABLE competences RENAME COLUMN valueApproved TO value_approved;
ALTER TABLE competences RENAME COLUMN valueRequested TO value_requested;

ALTER TABLE competences_history RENAME COLUMN valueApproved TO value_approved;
ALTER TABLE competences_history RENAME COLUMN valueRequested TO value_requested;

ALTER TABLE contracts RENAME COLUMN onCertificate TO on_certificate;
ALTER TABLE contracts_history RENAME COLUMN onCertificate TO on_certificate;

ALTER TABLE office RENAME COLUMN headQuarter TO head_quarter;
ALTER TABLE office_history RENAME COLUMN headQuarter TO head_quarter;

ALTER TABLE person_children RENAME COLUMN bornDate TO born_date;
ALTER TABLE person_children_history RENAME COLUMN bornDate TO born_date;

ALTER TABLE person_days_in_trouble RENAME TO person_day_in_trouble;
ALTER TABLE person_days_in_trouble_history RENAME TO person_day_in_trouble_history;
ALTER TABLE person_day_in_trouble RENAME COLUMN emailSent TO email_sent;
ALTER TABLE person_day_in_trouble_history RENAME COLUMN emailSent to email_sent;
ALTER TABLE person_day_in_trouble RENAME COLUMN personday_id TO person_day_id;
ALTER TABLE person_day_in_trouble_history RENAME COLUMN personday_id TO person_day_id;

ALTER TABLE person_hour_for_overtime RENAME COLUMN numberOfHourForOvertime TO number_of_hour_for_overtime;

ALTER TABLE person_months_recap RENAME TO person_month_recap;
ALTER TABLE person_months_recap_history RENAME TO person_month_recap_history;
ALTER TABLE person_month_recap RENAME COLUMN fromDate TO from_date;
ALTER TABLE person_month_recap RENAME COLUMN toDate TO to_date;

ALTER TABLE person_reperibility_types_persons RENAME TO person_reperibility_types_managers;
ALTER TABLE person_reperibility_types_persons_history RENAME TO person_reperibility_types_managers_history;

ALTER TABLE persons RENAME COLUMN badgeNumber TO badge_number;
ALTER TABLE persons RENAME COLUMN iid TO i_id;
ALTER TABLE persons RENAME COLUMN oldid TO old_id;
ALTER TABLE persons RENAME COLUMN other_surnames TO others_surnames;
ALTER TABLE persons RENAME COLUMN person_in_charge TO person_in_charge_id;
ALTER TABLE persons_history RENAME COLUMN badgeNumber TO badge_number;
ALTER TABLE persons_history RENAME COLUMN iid TO i_id;
ALTER TABLE persons_history RENAME COLUMN oldid TO old_id;
ALTER TABLE persons_history RENAME COLUMN other_surnames TO others_surnames;
ALTER TABLE persons_history RENAME COLUMN person_in_charge TO person_in_charge_id;

ALTER TABLE person_reperibility_types RENAME COLUMN supervisor TO supervisor_id;
ALTER TABLE person_reperibility_types_history RENAME COLUMN supervisor TO supervisor_id;

ALTER TABLE shift_categories_persons RENAME TO shift_categories_managers;
ALTER TABLE shift_categories_persons_history RENAME TO shift_categories_managers_history;

ALTER TABLE stampings RENAME COLUMN personday_id TO person_day_id;
ALTER TABLE stampings_history RENAME COLUMN personday_id TO person_day_id;

ALTER TABLE total_overtime RENAME COLUMN numberOfHours TO number_of_hours;

ALTER TABLE working_time_type_days RENAME COLUMN breaktickettime TO break_ticket_time;
ALTER TABLE working_time_type_days RENAME COLUMN dayofweek TO day_of_week;
ALTER TABLE working_time_type_days RENAME COLUMN mealtickettime TO meal_ticket_time;
ALTER TABLE working_time_type_days RENAME COLUMN timemealfrom TO time_meal_from;
ALTER TABLE working_time_type_days RENAME COLUMN timemealto TO time_meal_to;
ALTER TABLE working_time_type_days RENAME COLUMN timeslotentrancefrom TO time_slot_entrance_from;
ALTER TABLE working_time_type_days RENAME COLUMN timeslotentranceto TO time_slot_entrance_to;
ALTER TABLE working_time_type_days RENAME COLUMN timeslotexitfrom TO time_slot_exit_from;
ALTER TABLE working_time_type_days RENAME COLUMN timeslotexitto TO time_slot_exit_to;
ALTER TABLE working_time_type_days RENAME COLUMN workingtime TO working_time;

ALTER TABLE working_time_type_days_history RENAME COLUMN breaktickettime TO break_ticket_time;
ALTER TABLE working_time_type_days_history RENAME COLUMN dayofweek TO day_of_week;
ALTER TABLE working_time_type_days_history RENAME COLUMN mealtickettime TO meal_ticket_time;
ALTER TABLE working_time_type_days_history RENAME COLUMN timemealfrom TO time_meal_from;
ALTER TABLE working_time_type_days_history RENAME COLUMN timemealto TO time_meal_to;
ALTER TABLE working_time_type_days_history RENAME COLUMN timeslotentrancefrom TO time_slot_entrance_from;
ALTER TABLE working_time_type_days_history RENAME COLUMN timeslotentranceto TO time_slot_entrance_to;
ALTER TABLE working_time_type_days_history RENAME COLUMN timeslotexitfrom TO time_slot_exit_from;
ALTER TABLE working_time_type_days_history RENAME COLUMN timeslotexitto TO time_slot_exit_to;
ALTER TABLE working_time_type_days_history RENAME COLUMN workingtime TO working_time;


# ---!Downs

ALTER TABLE working_time_type_days RENAME COLUMN break_ticket_time TO breaktickettime;
ALTER TABLE working_time_type_days RENAME COLUMN day_of_week TO dayofweek;
ALTER TABLE working_time_type_days RENAME COLUMN meal_ticket_time TO mealtickettime;
ALTER TABLE working_time_type_days RENAME COLUMN time_meal_from TO timemealfrom;
ALTER TABLE working_time_type_days RENAME COLUMN time_meal_to TO timemealto;
ALTER TABLE working_time_type_days RENAME COLUMN time_slot_entrance_from TO timeslotentrancefrom;
ALTER TABLE working_time_type_days RENAME COLUMN time_slot_entrance_to TO timeslotentranceto;
ALTER TABLE working_time_type_days RENAME COLUMN time_slot_exit_from TO timeslotexitfrom;
ALTER TABLE working_time_type_days RENAME COLUMN time_slot_exit_to TO timeslotexitto;
ALTER TABLE working_time_type_days RENAME COLUMN working_time TO workingtime;

ALTER TABLE working_time_type_days_history RENAME COLUMN break_ticket_time TO breaktickettime;
ALTER TABLE working_time_type_days_history RENAME COLUMN day_of_week TO dayofweek;
ALTER TABLE working_time_type_days_history RENAME COLUMN meal_ticket_time TO mealtickettime;
ALTER TABLE working_time_type_days_history RENAME COLUMN time_meal_from TO timemealfrom;
ALTER TABLE working_time_type_days_history RENAME COLUMN time_meal_to TO timemealto;
ALTER TABLE working_time_type_days_history RENAME COLUMN time_slot_entrance_from TO timeslotentrancefrom;
ALTER TABLE working_time_type_days_history RENAME COLUMN time_slot_entrance_to TO timeslotentranceto;
ALTER TABLE working_time_type_days_history RENAME COLUMN time_slot_exit_from TO timeslotexitfrom;
ALTER TABLE working_time_type_days_history RENAME COLUMN time_slot_exit_to TO timeslotexitto;
ALTER TABLE working_time_type_days_history RENAME COLUMN working_time TO workingtime;

ALTER TABLE total_overtime RENAME COLUMN number_of_hours TO numberOfHours;

ALTER TABLE stampings RENAME COLUMN person_day_id TO personday_id;
ALTER TABLE stampings_history RENAME COLUMN person_day_id TO personday_id;

ALTER TABLE shift_categories_managers RENAME TO shift_categories_persons;
ALTER TABLE shift_categories_managers_history RENAME TO shift_categories_persons_history;

ALTER TABLE person_reperibility_types RENAME COLUMN supervisor_id TO supervisor;
ALTER TABLE person_reperibility_types_history RENAME COLUMN supervisor_id TO supervisor;

ALTER TABLE persons RENAME COLUMN badge_number TO badgeNumber;
ALTER TABLE persons RENAME COLUMN i_id TO iid;
ALTER TABLE persons RENAME COLUMN old_id TO oldid;
ALTER TABLE persons RENAME COLUMN others_surnames TO other_surnames;
ALTER TABLE persons RENAME COLUMN person_in_charge_id TO person_in_charge;
ALTER TABLE persons_history RENAME COLUMN badge_number TO badgeNumber;
ALTER TABLE persons_history RENAME COLUMN i_id TO iid;
ALTER TABLE persons_history RENAME COLUMN old_id TO oldid;
ALTER TABLE persons_history RENAME COLUMN others_surnames to other_surnames;
ALTER TABLE persons_history RENAME COLUMN person_in_charge_id TO person_in_charge;

ALTER TABLE person_reperibility_types_managers RENAME TO person_reperibility_types_persons;
ALTER TABLE person_reperibility_types_managers_history RENAME TO person_reperibility_types_persons_history;

ALTER TABLE person_month_recap RENAME TO person_months_recap;
ALTER TABLE person_month_recap_history RENAME TO person_months_recap_history;
ALTER TABLE person_months_recap RENAME COLUMN from_date TO fromDate;
ALTER TABLE person_months_recap RENAME COLUMN to_date TO toDate;

ALTER TABLE person_hour_for_overtime RENAME COLUMN number_of_hour_for_overtime TO numberOfHourForOvertime;

ALTER TABLE person_day_in_trouble RENAME TO person_days_in_trouble;
ALTER TABLE person_day_in_trouble_history RENAME TO person_days_in_trouble_history;
ALTER TABLE person_days_in_trouble RENAME COLUMN email_sent TO emailSent;
ALTER TABLE person_days_in_trouble_history RENAME COLUMN email_sent to emailSent;
ALTER TABLE person_days_in_trouble RENAME COLUMN person_day_id TO personday_id;
ALTER TABLE person_days_in_trouble_history RENAME COLUMN person_day_id TO personday_id;

ALTER TABLE person_children RENAME COLUMN born_date to bornDate;
ALTER TABLE person_children_history RENAME COLUMN born_date to bornDate;

ALTER TABLE office RENAME COLUMN head_quarter TO headQuarter;
ALTER TABLE office_history RENAME COLUMN head_quarter TO headQuarter;

ALTER TABLE contracts RENAME COLUMN on_certificate TO onCertificate;
ALTER TABLE contracts_history RENAME COLUMN on_certificate TO onCertificate;

ALTER TABLE competences RENAME COLUMN value_approved TO valueApproved;
ALTER TABLE competences RENAME COLUMN value_requested TO valueRequested;

ALTER TABLE competences_history RENAME COLUMN value_approved TO valueApproved;
ALTER TABLE competences_history RENAME COLUMN value_requested TO valueRequested;

ALTER TABLE competence_codes RENAME COLUMN code_to_presence TO codeToPresence;
ALTER TABLE competence_codes_history RENAME COLUMN code_to_presence TO codeToPresence;

ALTER TABLE badge_readers_badge_systems RENAME COLUMN badge_readers_id TO badgereaders_id;
ALTER TABLE badge_readers_badge_systems_history RENAME COLUMN badge_readers_id TO badgereaders_id;

ALTER TABLE badge_readers_badge_systems RENAME COLUMN badge_systems_id TO badgesystems_id;
ALTER TABLE badge_readers_badge_systems_history RENAME COLUMN badge_systems_id TO badgesystems_id;

ALTER TABLE absences RENAME COLUMN person_day_id TO personday_id;
ALTER TABLE absences_history RENAME COLUMN person_day_id TO personday_id;

ALTER TABLE absence_types_qualifications RENAME COLUMN absence_types_id TO absencetypes_id;
ALTER TABLE absence_types_qualifications_history RENAME COLUMN absence_types_id TO absencetypes_id;
