package dao;

import helpers.jpa.PerseoModelQuery;
import helpers.jpa.PerseoModelQuery.PerseoSimpleResults;

import java.util.List;

import javax.persistence.EntityManager;

import models.Institute;
import models.Office;
import models.Role;
import models.User;
import models.query.QInstitute;
import models.query.QOffice;
import models.query.QUsersRolesOffices;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

/**
 * 
 * @author dario
 *
 */
public class OfficeDao extends DaoBase {

	public static final Splitter TOKEN_SPLITTER = Splitter.on(' ')
			.trimResults().omitEmptyStrings();
	
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
	
	private BooleanBuilder matchInstituteName(QInstitute institute, String name) {
		final BooleanBuilder nameCondition = new BooleanBuilder();
		for (String token : TOKEN_SPLITTER.split(name)) {
			nameCondition.and(institute.name.containsIgnoreCase(token)
					.or(institute.code.containsIgnoreCase(token)));
		}
		return nameCondition.or(institute.name.startsWithIgnoreCase(name))
				.or(institute.code.startsWithIgnoreCase(name));
	}
	
	private BooleanBuilder matchOfficeName(QOffice office, String name) {
		final BooleanBuilder nameCondition = new BooleanBuilder();
		for (String token : TOKEN_SPLITTER.split(name)) {
			nameCondition.and(office.name.containsIgnoreCase(token));
		}
		return nameCondition.or(office.name.containsIgnoreCase(name));
				
	}
	
	/**
	 * Gli istituti che contengono sede sulle quali l'user ha il ruolo role.
	 * @param user
	 * @param role
	 * @return
	 */
	public PerseoSimpleResults<Institute> institutes(Optional<String> name, User user, Role role) {
		
		final QInstitute institute = QInstitute.institute;
		final QOffice office = QOffice.office;
		final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
		
		final BooleanBuilder condition = new BooleanBuilder();
		if (name.isPresent()) {
			condition.and(matchInstituteName(institute, name.get()));
		}
		
		if(user.isSystemUser()) {
			final JPQLQuery query = getQueryFactory()
					.from(institute)
					.where(condition);
			return PerseoModelQuery.wrap(query, institute);
		}
		
		final JPQLQuery query = getQueryFactory()
				.from(institute)
				.rightJoin(institute.seats, office)
				.rightJoin(office.usersRolesOffices, uro)
				.where(condition.and(uro.user.eq(user).and(uro.role.eq(role))))
				.distinct();
				
		return PerseoModelQuery.wrap(query, institute);
		
	}
	
	/**
	 * Le sedi sulle quali l'user ha il ruolo role.
	 * @param user
	 * @param role
	 * @return
	 */
	public PerseoSimpleResults<Office> offices(Optional<String> name,
			User user, Role role) {
		
		final QOffice office = QOffice.office;
		final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
		final QInstitute institute = QInstitute.institute;
		
		final BooleanBuilder condition = new BooleanBuilder();
		if (name.isPresent()) {
			condition.and(matchOfficeName(office, name.get()));
			condition.and(matchInstituteName(institute, name.get()));
		}
		
		if(user.isSystemUser()) {
			final JPQLQuery query = getQueryFactory()
					.from(office)
					.leftJoin(office.institute, institute).fetch()	
					.where(condition)
					.distinct()
					.orderBy(office.institute.name.asc());
			return PerseoModelQuery.wrap(query, office);
		}
		
		final JPQLQuery query = getQueryFactory()
				.from(office)
				.leftJoin(office.usersRolesOffices, uro)
				.leftJoin(office.institute, institute).fetch()
				.where(condition.and(uro.user.eq(user).and(uro.role.eq(role))))
				.distinct()
				.orderBy(office.institute.name.asc());
				
		return PerseoModelQuery.wrap(query, office);
		
	}

}
