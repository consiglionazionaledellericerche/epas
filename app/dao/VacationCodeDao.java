package dao;

import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import models.VacationCode;
import models.query.QVacationCode;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * 
 * @author dario
 *
 */
public class VacationCodeDao extends DaoBase {

	@Inject
	VacationCodeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	/**
	 * 
	 * @return la lista di tutti i vacationCode presenti sul database
	 */
	public List<VacationCode> getAllVacationCodes(){
		final QVacationCode code = QVacationCode.vacationCode;
		JPQLQuery query = getQueryFactory().from(code);
		return query.list(code);
	}

	/**
	 * 
	 * @param id
	 * @return il vacationCode associato all'id passato come parametro
	 */
	public VacationCode getVacationCodeById(Long id){
		final QVacationCode code = QVacationCode.vacationCode;
		JPQLQuery query = getQueryFactory().from(code)
				.where(code.id.eq(id));
		return query.singleResult(code);

	}

	/**
	 * 
	 * @param description
	 * @return il vacationCode associato alla stringa passata come parametro
	 */
	public VacationCode getVacationCodeByDescription(String description){
		final QVacationCode code = QVacationCode.vacationCode;
		JPQLQuery query = getQueryFactory().from(code)
				.where(code.description.eq(description));
		return query.singleResult(code);

	}
}
