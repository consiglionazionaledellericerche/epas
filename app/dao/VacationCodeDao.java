package dao;

import helpers.ModelQuery;

import java.util.List;

import models.VacationCode;
import models.query.QVacationCode;

import com.mysema.query.jpa.JPQLQuery;

/**
 * 
 * @author dario
 *
 */
public class VacationCodeDao {

	/**
	 * 
	 * @return la lista di tutti i vacationCode presenti sul database
	 */
	public static List<VacationCode> getAllVacationCodes(){
		QVacationCode code = QVacationCode.vacationCode;
		JPQLQuery query = ModelQuery.queryFactory().from(code);
		return query.list(code);
	}
	
	/**
	 * 
	 * @param id
	 * @return il vacationCode associato all'id passato come parametro
	 */
	public static VacationCode getVacationCodeById(Long id){
		QVacationCode code = QVacationCode.vacationCode;
		JPQLQuery query = ModelQuery.queryFactory().from(code)
				.where(code.id.eq(id));
		return query.singleResult(code);
		
	}
	
	/**
	 * 
	 * @param description
	 * @return il vacationCode associato alla stringa passata come parametro
	 */
	public static VacationCode getVacationCodeByDescription(String description){
		QVacationCode code = QVacationCode.vacationCode;
		JPQLQuery query = ModelQuery.queryFactory().from(code)
				.where(code.description.eq(description));
		return query.singleResult(code);
		
	}
}
