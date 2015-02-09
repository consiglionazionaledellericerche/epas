package dao;

import helpers.ModelQuery;

import java.util.List;

import models.Person;
import models.PersonChildren;
import models.query.QPersonChildren;

import com.mysema.query.jpa.JPQLQuery;


/**
 * 
 * @author dario
 *
 */

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
	
	
	/**
	 * 
	 * @param person
	 * @return la lista di tutti i figli della persona person passata come parametro
	 */
	public static List<PersonChildren> getAllPersonChildren(Person person){
		QPersonChildren personChildren = QPersonChildren.personChildren;
		final JPQLQuery query = ModelQuery.queryFactory().from(personChildren)
				.where(personChildren.person.eq(person));
		query.orderBy(personChildren.bornDate.asc());
		return query.list(personChildren);
	}
}
