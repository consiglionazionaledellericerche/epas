package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import models.CompetenceCode;
import models.query.QCompetenceCode;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * 
 * @author dario
 *
 */
public class CompetenceCodeDao extends DaoBase{

	@Inject
	CompetenceCodeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	/**
	 * 
	 * @param code
	 * @return il competenceCode relativo al codice passato come parametro
	 */
	public CompetenceCode getCompetenceCodeByCode(String code){
		final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

		final JPQLQuery query = getQueryFactory().from(competenceCode)
				.where(competenceCode.code.eq(code));

		return query.singleResult(competenceCode);

	}

	/**
	 * 
	 * @param description
	 * @return il codice competenza relativo alla descrizione passata come parametro
	 */
	public CompetenceCode getCompetenceCodeByDescription(String description){

		final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

		final JPQLQuery query = getQueryFactory().from(competenceCode)
				.where(competenceCode.description.eq(description));

		return query.singleResult(competenceCode);

	}

	/**
	 * 
	 * @param id
	 * @return il codice di competenza relativo all'id passato come parametro
	 */
	public CompetenceCode getCompetenceCodeById(Long id){

		final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

		final JPQLQuery query = getQueryFactory().from(competenceCode)
				.where(competenceCode.id.eq(id));

		return query.singleResult(competenceCode);

	}

	/**
	 * 
	 * @return la lista di tutti i codici di competenza presenti nel database
	 */
	public List<CompetenceCode> getAllCompetenceCode(){

		final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

		final JPQLQuery query = getQueryFactory().from(competenceCode);
		return query.orderBy(competenceCode.id.asc()).list(competenceCode);
	}
}
