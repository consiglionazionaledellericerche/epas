package dao;

import java.util.ArrayList;
import java.util.List;

import helpers.ModelQuery;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import models.Office;
import models.Person;
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
	public static List<Office> getOfficeAllowed(User user) {
		
		List<Office> officeList = new ArrayList<Office>();
		for(UsersRolesOffices uro : user.usersRolesOffices){
			if(uro.office.isSeat())
				officeList.add(uro.office);
		}
		//TODO riscrivere col nuovo concetto di ruoli e permessi e funzionale al tipo di ruolo che si cerca
//		if (this.person != null) {
//			officeList.add(this.person.office);
//		}
//		else {
//			
//			officeList = Office.findAll(); 
//		}
		return officeList;
			
		//return Office.find("select distinct o from Office o join "
		//		+ "o.userPermissionOffices as upo where upo.user = ?",this).fetch();
		
	}
	
	
	/**
	 * 
	 * @return la lista delle sedi visibili alla persona che ha chiamato il metodo
	 */
	public static List<Office> getOfficeAllowed(Person person){
		
		List<Office> officeList = new ArrayList<Office>();
		officeList.add(person.office);
		if(!person.office.subOffices.isEmpty()){
			
			for(Office office : person.office.subOffices){
				officeList.add(office);
			}
		}
		
		return officeList;
	}
}
