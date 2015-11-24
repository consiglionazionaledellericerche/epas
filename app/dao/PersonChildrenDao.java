package dao;

import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import models.Person;
import models.PersonChildren;
import models.query.QPersonChildren;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;


/**
 * 
 * @author dario
 *
 */

public class PersonChildrenDao extends DaoBase {

	@Inject
	PersonChildrenDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}


	/**
	 * 
	 * @param id
	 * @return il personChildren relativo all'id passato come parametro
	 */
	public PersonChildren getById(Long id){
		QPersonChildren personChildren = QPersonChildren.personChildren;
		final JPQLQuery query = getQueryFactory().from(personChildren)
				.where(personChildren.id.eq(id));
		return query.singleResult(personChildren);
	}


	/**
	 * 
	 * @param person
	 * @return la lista di tutti i figli della persona person passata come parametro
	 */
	public List<PersonChildren> getAllPersonChildren(Person person){
		QPersonChildren personChildren = QPersonChildren.personChildren;
		final JPQLQuery query = getQueryFactory().from(personChildren)
				.where(personChildren.person.eq(person));
		query.orderBy(personChildren.bornDate.asc());
		return query.list(personChildren);
	}
}
