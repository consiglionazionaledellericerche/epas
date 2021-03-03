# --- !Ups

INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO initialization_groups_history (id, _revision, _revision_type, person_id, group_absence_type_id, date, forced_begin,
forced_end, takable_total, takable_used, complation_used, vacation_year, residual_minutes_last_year, residual_minutes_current_year,
units_input, hours_input, minutes_input, average_week_time)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 2, person_id, group_absence_type_id, date, forced_begin,
forced_end, takable_total, takable_used, complation_used, vacation_year, residual_minutes_last_year, residual_minutes_current_year,
units_input, hours_input, minutes_input, average_week_time
FROM initialization_groups WHERE group_absence_type_id = 10;

DELETE FROM initialization_groups where group_absence_type_id = 10;

# --- !Downs

# -- non è necessaria una down