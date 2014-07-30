package controllers;

import java.util.List;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import controllers.Resecure.NoCheck;
import dao.PersonDao;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import play.Play;
import play.mvc.Controller;
import play.mvc.With;

@With( {Resecure.class, RequestInit.class} )
public class Administrators extends Controller {

	private static final String SUDO_USERNAME = "sudo.username";
	private static final String USERNAME = "username";

	@NoCheck
	public static void insertNewAdministrator(Long officeId, Long roleId) {
		
		Office office = Office.findById(officeId);
		if(office==null) {
			
			flash.error("La sede per la quale si vuole definire l'amministratore è inesistente. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}
		
		Role role = Role.findById(roleId);
		if(office==null) {
			
			flash.error("Il ruolo selezionato è inesistente. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}
		
		String name = null;
		List<Person> personList = PersonDao.list(Optional.fromNullable(name), 
					Sets.newHashSet(Security.getOfficeAllowed()), false, 
					LocalDate.now(), LocalDate.now(), true).list();
		
		render(office, role, personList);
	}
	
	@NoCheck
	public static void saveNewAdministrator(Person person, Office office, Role role) {
		
		if(person==null || office==null || role==null) {
			
			flash.error("Errore nell'inserimento parametri. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}
		
		//Per adesso faccio inserire solo alle sedi
		if( !office.isSeat() ) {
			
			flash.error("Impossibile assegnare amministratori a livello diverso da quello Sede. Operazione annullata.");
			Offices.showOffices();
		}
		
		if( !Office.setUroIfImprove(person.user, office, role, true) ) {
		
			flash.error("La persona dispone già dei permessi associati al ruolo selezionato. Operazione annullata.");
			Offices.showOffices();
		}
		
		flash.success("Nuovo amministratore inserito con successo.");
		Offices.showOffices();
	}
	
	@NoCheck
	public static void deleteAdministrator(Long officeId, Long personId) {
		
		Office office = Office.findById(officeId);
		if(office==null) {
			
			flash.error("La sede per la quale si vuole rimuovere l'amministratore è inesistente. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}
		
		Person person = Person.findById(personId);
		if(person == null) {
			
			flash.error("La persona per la quale si vuole rimuovere il ruolo di ammninistratore è inesistente. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}
		
		UsersRolesOffices uro = Office.getUro(person.user, office);
		if(uro == null) {
			
			flash.error("La persona non dispone di alcun ruolo amministrativo. Operazione annullata.");
			Offices.showOffices();
		}
		Role role = uro.role;
		
		//Rimozione ruolo sola lettura
		if(role.name.equals(Role.PERSONNEL_ADMIN_MINI)) {
			
			uro.delete();
			flash.success("Rimozione amministratore avvenuta con successo.");
			Offices.showOffices();
		}
		
		//controllo che l'office non rimanga senza amministratori generali
		boolean atLeastAnother = false;
		for(UsersRolesOffices uroOffice : office.usersRolesOffices) {
			
			if( uroOffice.role.id.equals(role.id) && !uroOffice.user.isAdmin() 
					&& !uroOffice.id.equals(uro.id) ) {
				atLeastAnother = true;
				break;
			}
		} 
		if( !atLeastAnother) {
			
			flash.error("La sede non può rimanere senza amministratori generali. Operazione annullata.");
			Offices.showOffices();
		}
		
		uro.delete();
		flash.success("Rimozione amministratore avvenuta con successo.");
		Offices.showOffices();
	
	}
	
	@NoCheck //TODO IMPORTANTE VA TOLTO!!!! admin non può chiamarlo
	public static void deleteSelfAsAdministrator(Long officeId) {
		
		Office office = Office.findById(officeId);
		if(office==null) {
			
			flash.error("La sede per la quale si vuole rimuovere l'amministratore è inesistente. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}
		
		Person person = Security.getUser().get().person;
		render(office, person);
	
	}
	
	
	
	/**
	 * Switch in un'altra persona
	 */ 
	@NoCheck
	public static void switchUserTo(long id) {
		final User user = User.findById(id);
		notFoundIfNull(user);
		
		//Per adesso permettiamo questa funzionalità solo all'utente admin
		User userLogged = Security.getUser().get();
		if(userLogged != null && userLogged.username.equals("admin") ){

			// salva il precedente
			session.put(SUDO_USERNAME, session.get(USERNAME));
			// recupera 
			session.put(USERNAME, user.username);
			// redirect alla radice
			redirect(Play.ctxPath + "/");
		}
		
		forbidden();
	}
	
	/**
	 * ritorna alla precedente persona.
	 */
	@NoCheck
	public static void restoreUser() {
		if (session.contains(SUDO_USERNAME)) {
			session.put(USERNAME, session.get(SUDO_USERNAME));
			session.remove(SUDO_USERNAME);
		}
		// redirect alla radice
		redirect(Play.ctxPath + "/");
	}
	
	
}
