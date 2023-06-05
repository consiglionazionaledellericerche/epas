COPY (
	select 
		pa.sigla_istituto, pa.nome_istituto, pa.nome_sede, 
		pa.codice_sede, pa.sede_id, pa.personale_attivo, 
		coalesce(c19.numero_codici_covid_19, 0) as numero_codici_covid_19 
	from personale_attivo_sedi_view pa 
	left join covid_19_view c19 on pa.sede_id = c19.sede_id and c19.date = now()::date) 
To '/tmp/dati-codiv-19.csv' With CSV HEADER;