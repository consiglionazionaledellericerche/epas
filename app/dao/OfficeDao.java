package dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.query.QOffice;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

/**
 * 
 * @author dario
 *
 */
public class OfficeDao extends DaoBase {

	@Inject
	OfficeDao(JPQLQueryFactory queryFactory,Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	/**
	 * 
	 * @param id
	 * @return l'ufficio identificato dall'id passato come parametro
	 */
	public Office getOfficeById(Long id){

		final QOffice office = QOffice.office;

		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.id.eq(id));
		return query.singleResult(office);
	}

	/**
	 * 
	 * @return la lista di tutti gli uffici presenti sul database
	 */
	public List<Office> getAllOffices(){

		final QOffice office = QOffice.office;

		final JPQLQuery query = getQueryFactory().from(office);

		return query.list(office);

	}

	/**
	 * 
	 * @param name
	 * @return  
	 */
	public Optional<Office> getOfficeByName(String name){

		final QOffice office = QOffice.office;

		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.name.eq(name));

		return Optional.fromNullable(query.singleResult(office));
	}

	/**
	 * 
	 * @param code
	 * @return l'ufficio associato al codice passato come parametro
	 */
	public Optional<Office> getOfficeByCode(Integer code){

		final QOffice office = QOffice.office;

		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.code.eq(code));
		return Optional.fromNullable(query.singleResult(office));

	}

	/**
	 * 
	 * @param code
	 * @return la lista di uffici che possono avere associato il codice code passato come parametro
	 */
	public List<Office> getOfficesByCode(Integer code){

		final QOffice office = QOffice.office;

		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.code.eq(code));
		return query.list(office);
	}

	/**
	 *  La lista di tutte le Aree definite nel db ePAS (Area -> campo office = null)
	 * @return la lista delle aree presenti in anagrafica
	 */
	public List<Office> getAreas(){

		final QOffice office = QOffice.office;

		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.office.isNull());
		return query.list(office);
	}

	public boolean checkForDuplicate(Office o){

		final QOffice office = QOffice.office;

		final BooleanBuilder condition = new BooleanBuilder();
		condition.or(office.name.equalsIgnoreCase(o.name));

		if(o.code!=null){
			condition.or(office.code.eq(o.code));
		}

		if(o.id!=null){
			condition.and(office.id.ne(o.id));
		}

		return getQueryFactory().from(office)
				.where(condition).exists();
	}

}
