package dao;

import helpers.ModelQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import models.Office;
import models.User;
import models.UsersRolesOffices;
import models.query.QOffice;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import controllers.Security;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

/**
 * 
 * @author dario
 *
 */
public class OfficeDao extends DaoBase {

	OfficeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	@Inject
	public IWrapperFactory wrapperFactory;
	
	

	private final static QOffice office = QOffice.office1;
	
	
	/**
	 * 
	 * @param id
	 * @return l'ufficio identificato dall'id passato come parametro
	 */
	public Office getOfficeById(Long id){
		
		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.id.eq(id));
		return query.singleResult(office);
	}
	
	/**
	 * 
	 * @return la lista di tutti gli uffici presenti sul database
	 */
	public List<Office> getAllOffices(){
		
		QOffice office = QOffice.office1;
		
		final JPQLQuery query = ModelQuery.queryFactory().from(office);
		
		return query.list(office);
				
	}
	
	/**
	 * 
	 * @param contraction
	 * @return  
	 */
	public Office getOfficeByContraction(String contraction){
		
		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.contraction.eq(contraction));
		
		return query.singleResult(office);
	}
	
	/**
	 * 
	 * @param name
	 * @return  
	 */
	public Office getOfficeByName(String name){
		QOffice office = QOffice.office1;
		
		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.name.eq(name));
		
		return query.singleResult(office);
	}
	
	/**
	 * 
	 * @param code
	 * @return l'ufficio associato al codice passato come parametro
	 */
	public Office getOfficeByCode(Integer code){
		QOffice office = QOffice.office1;
		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.code.eq(code));
		return query.singleResult(office);
		
	}
	
	/**
	 * 
	 * @param code
	 * @return la lista di uffici che possono avere associato il codice code passato come parametro
	 */
	public List<Office> getOfficesByCode(Integer code){
		QOffice office = QOffice.office1;
		final JPQLQuery query = getQueryFactory().from(office)
				.where(office.code.eq(code));
		return query.list(office);
	}
	
	/**
	 *  La lista di tutte le Aree definite nel db ePAS (Area -> campo office = null)
	 * @return la lista delle aree presenti in anagrafica
	 */
	public List<Office> getAreas(){
		QOffice office = QOffice.office1;
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
	public Set<Office> getOfficeAllowed(Optional<User> user) {
		
		User u = user.isPresent() ? user.get() : Security.getUser().get();
// 		L'utente standard non ha nessun userRoleoffice ed è necessario restituire il suo ufficio di appartenenza
//		FIXME Non sarebbe meglio avere un ruolo base per gli utenti???
		if(u.usersRolesOffices.isEmpty()){
			if(u.person != null){
				return Sets.newHashSet(u.person.office);
			}
			else
				return Sets.newHashSet();
		}
		
		return	FluentIterable.from(u.usersRolesOffices).transform(new Function<UsersRolesOffices,Office>() {
			@Override
			public Office apply(UsersRolesOffices uro) {
				return uro.office;
			}}).toSet();

//     FIXME Capire se è indispensabile restituire solo le sedi
//			filter(new Predicate<Office>() {
//	    	    @Override
//	    	    public boolean apply(Office o) {
//	    	        return o.isSeat();
//	    	    }}).toSet();
		
	}
}
