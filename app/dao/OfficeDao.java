package dao;

import java.util.List;
import java.util.Set;

import helpers.ModelQuery;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import controllers.Security;
import controllers.Wizard.WizardStep;
import models.Office;
import models.User;
import models.UsersRolesOffices;
import models.query.QOffice;

/**
 * 
 * @author dario
 *
 */
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
		return query.singleResult(office);
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
	
	/**
	 * 
	 * @param name
	 * @return l'ufficio con nome o contrazione uguali a quelli passati come parametro. I parametri sono opzionali, il metodo va usato scegliendo
	 * quale fra i due parametri si vuole passare per fare la ricerca dell'ufficio. 
	 */
	public static Office getOfficeByNameOrByContraction(Optional<String> name, Optional<String> contraction){
		QOffice office = QOffice.office1;
		final BooleanBuilder condition = new BooleanBuilder();
		if(name.isPresent())
			condition.and(office.name.eq(name.get()));
		if(contraction.isPresent())
			condition.and(office.contraction.eq(contraction.get()));
		
		final JPQLQuery query = ModelQuery.queryFactory().from(office)
				.where(condition);
		return query.singleResult(office);
	}
	
	/**
	 * 
	 * @param code
	 * @return l'ufficio associato al codice passato come parametro
	 */
	public static Office getOfficeByCode(Integer code){
		QOffice office = QOffice.office1;
		final JPQLQuery query = ModelQuery.queryFactory().from(office)
				.where(office.code.eq(code));
		return query.singleResult(office);
		
	}
	
	/**
	 * 
	 * @param code
	 * @return la lista di uffici che possono avere associato il codice code passato come parametro
	 */
	public static List<Office> getOfficesByCode(Integer code){
		QOffice office = QOffice.office1;
		final JPQLQuery query = ModelQuery.queryFactory().from(office)
				.where(office.code.eq(code));
		return query.list(office);
	}
	
	/**
	 * 
	 * @return la lista delle aree presenti in anagrafica (per convenzione sono quelle col campo office = null)
	 */
	public static List<Office> getAreas(){
		QOffice office = QOffice.office1;
		final JPQLQuery query = ModelQuery.queryFactory().from(office)
				.where(office.office.isNull());
		return query.list(office);
	}
	
	/**
	 * 
	 * @param user
	 * @return la lista degli uffici permessi per l'utente user passato come parametro
	 */
	public static Set<Office> getOfficeAllowed(Optional<User> user) {
		
		User u = user.or(Security.getUser().get());
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
