create view personale_attivo_sedi_view as select 
		i.code as sigla_istituto, i.name as nome_istituto, 
		o.name as nome_sede, o.code as codice_sede, o.code_id as sede_id, 
		count(*) as personale_attivo 
		from institutes i join office o on o.institute_id = i.id and (o.end_date <= now() or o.end_date is null) 
		join persons p on p.office_id = o.id 
		join contracts c on c.person_id = p.id 
		where c.begin_date < now()::date 
			and	(end_contract is null or end_contract >= now()::date) 
			and (c.end_date is null or c.end_date >= now()::date) 
			and on_certificate is true 
		group by o.id, i.code, i.name 
		order by sede_id asc;