create view covid_19_view as select 
		i.code as sigla_istituto, i.name as nome_istituto, o.name as nome_sede, 
		o.code as codice_sede, o.code_id as sede_id, 
		pd.date, count(*) as numero_codici_covid_19 
	from institutes i join office o 
		on o.institute_id = i.id and (o.end_date <= now() or o.end_date is null) 
	join persons p on p.office_id = o.id 
	join person_days pd on pd.person_id = p.id 
	join absences a on a.person_day_id = pd.id 
	join absence_types at on a.absence_type_id = at.id 
	where at.code = 'COVID19' 
	group by pd.date, o.id, i.code, i.name 
	order by sede_id asc;
