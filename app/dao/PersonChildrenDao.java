package dao;

import helpers.ModelQuery;
import models.PersonChildren;
import models.query.QPersonChildren;

import com.mysema.query.jpa.JPQLQuery;

public class PersonChildrenDao {

	
	/**
	 * 
	 * @param id
	 * @return il personChildren relativo all'id passato come parametro
	 */
	public static PersonChildren getPersonChildrenById(Long id){
		QPersonChildren personChildren = QPersonChildren.personChildren;
		final JPQLQuery query = ModelQuery.queryFactory().from(personChildren)
				.where(personChildren.id.eq(id));
		return query.singleResult(personChildren);
	}
}
