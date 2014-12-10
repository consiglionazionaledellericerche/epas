package dao;

import java.util.List;

import helpers.ModelQuery;

import com.mysema.query.jpa.JPQLQuery;

import models.CompetenceCode;
import models.query.QCompetenceCode;

public class CompetenceCodeDao {

	/**
	 * 
	 * @param code
	 * @return il competenceCode relativo al codice passato come parametro
	 */
	public static CompetenceCode getCompetenceCodeByCode(String code){
		QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
		final JPQLQuery query = ModelQuery.queryFactory().from(competenceCode)
				.where(competenceCode.code.eq(code));
		if(query.list(competenceCode).size() > 0)
			return query.list(competenceCode).get(0);
		else
			return null;
	}
	
	/**
	 * 
	 * @param description
	 * @return il codice competenza relativo alla descrizione passata come parametro
	 */
	public static CompetenceCode getCompetenceCodeByDescription(String description){
		QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
		final JPQLQuery query = ModelQuery.queryFactory().from(competenceCode)
				.where(competenceCode.description.eq(description));
		if(query.list(competenceCode).size() > 0)
			return query.list(competenceCode).get(0);
		else
			return null;
	}
	
	/**
	 * 
	 * @param id
	 * @return il codice di competenza relativo all'id passato come parametro
	 */
	public static CompetenceCode getCompetenceCodeById(Long id){
		QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
		final JPQLQuery query = ModelQuery.queryFactory().from(competenceCode)
				.where(competenceCode.id.eq(id));
		if(query.list(competenceCode).size() > 0)
			return query.list(competenceCode).get(0);
		else
			return null;
	}
	
	/**
	 * 
	 * @return la lista di tutti i codici di competenza presenti nel database
	 */
	public static List<CompetenceCode> getAllCompetenceCode(){
		QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
		final JPQLQuery query = ModelQuery.queryFactory().from(competenceCode);
		return query.list(competenceCode);
	}
}
