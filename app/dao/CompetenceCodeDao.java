package dao;

import java.util.List;

import helpers.ModelQuery;

import com.mysema.query.jpa.JPQLQuery;

import models.CompetenceCode;
import models.query.QCompetenceCode;

/**
 * 
 * @author dario
 *
 */
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
		
		return query.singleResult(competenceCode);
		
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
		
		return query.singleResult(competenceCode);
		
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
		
		return query.singleResult(competenceCode);
		
	}
	
	/**
	 * 
	 * @return la lista di tutti i codici di competenza presenti nel database
	 */
	public static List<CompetenceCode> getAllCompetenceCode(){
		QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
		final JPQLQuery query = ModelQuery.queryFactory().from(competenceCode);
		return query.orderBy(competenceCode.id.asc()).list(competenceCode);
	}
}
