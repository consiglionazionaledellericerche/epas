package dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import models.ConfGeneral;
import models.Office;
import models.User;
import models.UsersRolesOffices;
import models.enumerate.Parameter;
import models.query.QOffice;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
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

	private final IWrapperFactory wrapperFactory;
	private final ConfGeneralDao confGeneralDao;

	@Inject
	OfficeDao(IWrapperFactory wrapperFactory,ConfGeneralDao confGeneralDao,
			JPQLQueryFactory queryFactory,Provider<EntityManager> emp) {
		super(queryFactory, emp);
		this.wrapperFactory = wrapperFactory;
		this.confGeneralDao = confGeneralDao;
	}

	/**
	 * 
	 * @param id
	 * @return l'ufficio identificato dall'id passato come parametro
	 */
	public Office getOfficeById(Long id){

		final QOffice office = QOffice.office1;

		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.id.eq(id));
		return query.singleResult(office);
	}

	/**
	 * 
	 * @return la lista di tutti gli uffici presenti sul database
	 */
	public List<Office> getAllOffices(){

		final QOffice office = QOffice.office1;

		final JPQLQuery query = getQueryFactory().from(office);

		return query.list(office);

	}

	/**
	 * 
	 * @param contraction
	 * @return  
	 */
	public Optional<Office> getOfficeByContraction(String contraction){

		final QOffice office = QOffice.office1;

		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.contraction.eq(contraction));

		return Optional.fromNullable(query.singleResult(office));
	}


	/**
	 *  La lista di tutte le Aree definite nel db ePAS (Area -> campo office = null)
	 * @return la lista delle aree presenti in anagrafica
	 */
	public List<Office> getAreas(){

		final QOffice office = QOffice.office1;

		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.office.isNull());
		return query.list(office);
	}

	/**
	 * Ritorna la lista di tutte le sedi gerarchicamente sotto a Office
	 * @return
	 */
	public List<Office> getSubOfficeTree(Office o) {

		List<Office> officeToCompute = new ArrayList<Office>();
		List<Office> officeComputed = new ArrayList<Office>();

		officeToCompute.add(o);
		while(officeToCompute.size() != 0) {

			Office office = officeToCompute.get(0);
			officeToCompute.remove(office);

			for(Office remoteOffice : office.subOffices) {

				officeToCompute.add((Office)remoteOffice);
			}

			officeComputed.add(office);
		}
		return officeComputed;
	}

	/**
	 * Ritorna l'area padre se office è un istituto o una sede
	 * @return
	 */
	public Office getSuperArea(Office office) {

		IWrapperOffice wOffice = wrapperFactory.create(office);

		if(wOffice.isSeat())
			return office.office.office;

		if(wOffice.isInstitute())
			return office.office;

		return null;
	}

	/**
	 * Ritorna l'istituto padre se this è una sede
	 * @return 
	 */
	public Office getSuperInstitute(Office office) {

		IWrapperOffice wOffice = wrapperFactory.create(office);

		if(!wOffice.isSeat())
			return null;
		return office.office;
	}

	/**
	 * 
	 * @param user
	 * @return la lista degli uffici permessi per l'utente user passato come parametro
	 */

	public Set<Office> getOfficeAllowed(User user) {

		Preconditions.checkNotNull(user);
		Preconditions.checkState(user.isPersistent());

		return	FluentIterable.from(user.usersRolesOffices).transform(
				new Function<UsersRolesOffices,Office>() {
					@Override
					public Office apply(UsersRolesOffices uro) {
						return uro.office;
					}}).filter(
							new Predicate<Office>() {
								@Override
								public boolean apply(Office o) {
									return wrapperFactory.create(o).isSeat();
								}}).toSet();

	}

	/**
	 * @param o
	 * @return true se esiste già un ufficio che utilizza lo stesso nome,sigla o codice,
	 * false altrimenti
	 */
	public boolean checkForDuplicate(Office o){

		final QOffice office = QOffice.office1;

		final BooleanBuilder condition = new BooleanBuilder();
		condition.or(office.name.equalsIgnoreCase(o.name));

		if(o.contraction!=null){
			condition.or(office.contraction.equalsIgnoreCase(o.contraction));
		}
		if(o.codeId!=null){
			condition.or(office.codeId.eq(o.codeId));
		}
		if(o.code!=null){
			condition.or(office.code.eq(o.code));
		}
		if(o.cds!=null){
			condition.or(office.cds.eq(o.cds));
		}
		if(o.id!=null){
			condition.and(office.id.ne(o.id));
		}

		return getQueryFactory().from(office)
				.where(condition).exists();
	}

	public Set<Office> getOfficesWithAllowedIp(String ip){

		Preconditions.checkNotNull(ip);

		return FluentIterable.from(confGeneralDao.containsValue(
				Parameter.ADDRESSES_ALLOWED.description, ip)).transform(
						new Function<ConfGeneral, Office>() {

							@Override
							public Office apply(ConfGeneral input) {
								return input.office;
							}
						}).toSet();
	}
	
	public Optional<Office> byCds(String cds){
		Preconditions.checkState(!Strings.isNullOrEmpty(cds));
		
		final QOffice office = QOffice.office1;
		
		final JPQLQuery query = getQueryFactory().from(office).where(office.cds.eq(cds));
		
		return Optional.fromNullable(query.singleResult(office));
	}
	
	public Optional<Office> byCode(String code){
		Preconditions.checkState(!Strings.isNullOrEmpty(code));
		
		final QOffice office = QOffice.office1;
		
		final JPQLQuery query = getQueryFactory().from(office).where(office.code.eq(code));
		
		return Optional.fromNullable(query.singleResult(office));
	}
	
	public Optional<Office> byCodeId(String codeId){
		Preconditions.checkState(!Strings.isNullOrEmpty(codeId));
		
		final QOffice office = QOffice.office1;
		
		final JPQLQuery query = getQueryFactory().from(office).where(office.codeId.eq(codeId));
		
		return Optional.fromNullable(query.singleResult(office));
	}
	
	public Optional<Office> byContraction(String contraction){
		Preconditions.checkState(!Strings.isNullOrEmpty(contraction));
		
		final QOffice office = QOffice.office1;
		
		final JPQLQuery query = getQueryFactory().from(office).where(office.contraction.eq(contraction));
		
		return Optional.fromNullable(query.singleResult(office));
	}
}
