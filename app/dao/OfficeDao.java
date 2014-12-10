package dao;

import java.util.List;

import helpers.ModelQuery;

import com.mysema.query.jpa.JPQLQuery;

import models.Office;
import models.query.QOffice;

public class OfficeDao {

	/**
	 * 
	 * @param id
	 * @return l'ufficio identificato dall'id passato come parametro
	 */
	public static Office getOfficeById(Long id){
		QOffice office = QOffice.office1;
		final JPQLQuery query = ModelQuery.queryFactory().from(office)
				.where(office.id.eq(id));
		return query.list(office).get(0);
	}
	
	/**
	 * 
	 * @return la lista di tutti gli uffici presenti sul database
	 */
	public static List<Office> getAllOffices(){
		QOffice office = QOffice.office1;
		final JPQLQuery query = ModelQuery.queryFactory().from(office);
		
		return query.list(office);
				
	}
}
