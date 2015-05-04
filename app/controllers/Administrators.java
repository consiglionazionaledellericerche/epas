package controllers;

import java.util.List;

import javax.inject.Inject;

import manager.OfficeManager;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;

import org.joda.time.LocalDate;

import play.Play;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gdata.util.common.base.Preconditions;

import controllers.Resecure.NoCheck;
import dao.OfficeDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.UserDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

@With( {Resecure.class, RequestInit.class} )
public class Administrators extends Controller {

	private static final String SUDO_USERNAME = "sudo.username";
	private static final String USERNAME = "username";

	@Inject static UsersRolesOfficesDao usersRolesOfficesDao;
	@Inject static OfficeManager officeManager;
	@Inject	static OfficeDao officeDao;
	@Inject	static IWrapperFactory wrapperFactory;
	
	public static void insertNewAdministrator(Long officeId, Long roleId) {
		
		Office office = officeDao.getOfficeById(officeId);
		if(office==null) {
			flash.error("La sede per la quale si vuole definire l'amministratore è inesistente. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}
		
		Role role = RoleDao.getRoleById(roleId);

		if(role==null) {
			flash.error("Il ruolo selezionato è inesistente. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}
		
		String name = null;
		List<Person> personList = PersonDao.list(Optional.fromNullable(name), 
				officeDao.getOfficeAllowed(Security.getUser().get()), false, 
				LocalDate.now(), LocalDate.now(), true).list();

		render(office, role, personList);
	}
	
	public static void saveNewAdministrator(Person person, Office office, Role role) {
		
		if(person==null || office==null || role==null) {
			
			flash.error("Errore nell'inserimento parametri. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}
		
		IWrapperOffice wOffice = wrapperFactory.create(office);
		
		//Per adesso faccio inserire solo alle sedi
		if( !wOffice.isSeat() ) {
			
			flash.error("Impossibile assegnare amministratori a livello diverso da quello Sede. Operazione annullata.");
			Offices.showOffices();
		}
		
		if( !officeManager.setUroIfImprove(person.user, office, role, true) ) {
		
			flash.error("La persona dispone già dei permessi associati al ruolo selezionato. Operazione annullata.");
			Offices.showOffices();
		}
		
		flash.success("Nuovo amministratore inserito con successo.");
		Offices.showOffices();
	}
	

	public static void deleteAdministrator(Long officeId, Long personId) {
		
		Office office = officeDao.getOfficeById(officeId);
		if(office==null) {
			flash.error("La sede per la quale si vuole rimuovere l'amministratore è inesistente. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}
		
		Person person = PersonDao.getPersonById(personId);

		if(person == null) {
			
			flash.error("La persona per la quale si vuole rimuovere il ruolo di ammninistratore è inesistente. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}
		
		Optional<UsersRolesOffices> uro = usersRolesOfficesDao.getUsersRolesOfficesByUserAndOffice(person.user, office);
		if( !uro.isPresent()) {
			
			flash.error("La persona non dispone di alcun ruolo amministrativo. Operazione annullata.");
			Offices.showOffices();
		}
		Role role = uro.get().role;
		
		//Rimozione ruolo sola lettura
		if(role.name.equals(Role.PERSONNEL_ADMIN_MINI)) {
			
			uro.get().delete();
			flash.success("Rimozione amministratore avvenuta con successo.");
			Offices.showOffices();
		}
		
		//controllo che l'office non rimanga senza amministratori generali
		boolean atLeastAnother = false;
		for(UsersRolesOffices uroOffice : office.usersRolesOffices) {
			
			//if( uroOffice.role.id.equals(role.id) && !uroOffice.user.isAdmin()
			if(uroOffice.role.id.equals(role.id) && UserDao.isAdmin(uroOffice.user)
					&& !uroOffice.id.equals(uro.get().id) ) {
				atLeastAnother = true;
				break;
			}
		} 
		if( !atLeastAnother) {
			
			flash.error("La sede non può rimanere senza amministratori generali. Operazione annullata.");
			Offices.showOffices();
		}
		
		uro.get().delete();
		flash.success("Rimozione amministratore avvenuta con successo.");
		Offices.showOffices();
	
	}
	
	public static void deleteSelfAsAdministrator(Long officeId) {
		
		Office office = officeDao.getOfficeById(officeId);
		if(office==null) {
			flash.error("La sede per la quale si vuole rimuovere l'amministratore è inesistente. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}

		Person person = Security.getUser().get().person;
		render(office, person);
	
	}
	
	//@NoCheck
	public static void insertAccountSystem(Long officeId) {
	
		Office office = officeDao.getOfficeById(officeId);
		if(office==null) {
			flash.error("La sede per la quale si vuole definire l'account di sistema "
					+ "è inesistente. Riprovare o effettuare una segnalazione.");
			Offices.showOffices();
		}
		
		List<Role> systemRoles = usersRolesOfficesDao.getSystemRolesOffices();
		
		// TODO: riportare nella select una lista di user già presente in ePAS.
		// esempio l'accaunto del lettore d'area dovrebbe essere dispobile per essere
		// assegnato anche agli altri istituti d'area e non essere duplicato.
		
		IWrapperOffice wrapperOffice = wrapperFactory.create(office);
		
		render(wrapperOffice, systemRoles);
		
	}
	
	//@NoCheck
	public static void saveAccountSystem(Office office, User user, Role role) {
		
		Preconditions.checkNotNull(office);
		Preconditions.checkState(office.isPersistent());
		Preconditions.checkNotNull(role);
		Preconditions.checkState(role.isPersistent());
		
		Preconditions.checkState(user.username != null && user.username != "");
		Preconditions.checkState(user.password != null && user.password != "");
		
		// TODO: effettuare ulteriori controlli sul nome dell'utente
		
		user.password = Codec.hexMD5(user.password);
		
		user.save();
		
		UsersRolesOffices uro = new UsersRolesOffices();
		uro.user = user;
		uro.office = office;
		uro.role = role;
		uro.save();
		
		flash.success("Account creato con successo.");
	
		Offices.showOffices();
		
	}
	
	//@NoCheck
	public static void deleteAccountSystem(Long systemUroId) {
		
		UsersRolesOffices systemUro = usersRolesOfficesDao.getById(systemUroId);
		
		Preconditions.checkNotNull(systemUro);
		
		//Check Account di sistema
		List<Role> systemRoles = usersRolesOfficesDao.getSystemRolesOffices();
		
		boolean isSystemRole = false;
		for(Role role : systemRoles) 
			if(role.name.equals(systemUro.role.name))
				isSystemRole = true;
		
		Preconditions.checkState(isSystemRole);
		
		// TODO: se l'user di sistema è presente in altri uro non andrebbe eliminato.
		User user = systemUro.user;
		
		systemUro.delete();
		
		user.delete();
		
		flash.success("Account rimosso con successo.");
		
		Offices.showOffices();
		
	}
			
		
	/**
	 * Switch in un'altra persona
	 */ 
	public static void switchUserTo(long id) {
		final User user = UserDao.getUserById(id, Optional.<String>absent());
		//final User user = User.findById(id);
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
