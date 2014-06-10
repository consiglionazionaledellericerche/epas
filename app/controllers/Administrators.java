package controllers;

import java.util.List;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import dao.PersonDao;
import models.Office;
import models.Person;
import models.User;
import play.Play;
import play.mvc.Controller;
import play.mvc.With;

@With( {Resecure.class, RequestInit.class} )
public class Administrators extends Controller {

	private static final String SUDO_USERNAME = "sudo.username";
	private static final String USERNAME = "username";

	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void list(String name) {
		

		List<Person> personList = PersonDao.list(Optional.fromNullable(name), 
				Sets.newHashSet(Security.getOfficeAllowed()), false, LocalDate.now(), 
				LocalDate.now()).list();
		
		List<Office> officeList = Office.findAll();
		
		render(personList, officeList);
	}

	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void discard(){
		Administrators.list(null);
	}

	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void edit(Long personId, Long officeId){
		if(personId != null){
			Person person = Person.findById(personId);
			render(person);
		}
	}
	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void update(){
		

		Administrators.list(null);
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void delete(Long adminId){
		Person person = Person.findById(adminId);
		person.user.usersRolesOffices.clear();
		person.save();
		flash.success(String.format("Eliminati i permessi per l'utente %s %s", person.name, person.surname));
		Application.indexAdmin();
	}
	
	/**
	 * Switch in un'altra persona
	 */
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void switchUserTo(long id) {
		final User user = User.findById(id);
		notFoundIfNull(user);
		
		
		// salva il precedente
		session.put(SUDO_USERNAME, session.get(USERNAME));
		// recupera 
		session.put(USERNAME, user.username);
		// redirect alla radice
		redirect(Play.ctxPath + "/");
	}
	
	/**
	 * ritorna alla precedente persona.
	 */
	public static void restoreUser() {
		if (session.contains(SUDO_USERNAME)) {
			session.put(USERNAME, session.get(SUDO_USERNAME));
			session.remove(SUDO_USERNAME);
		}
		// redirect alla radice
		redirect(Play.ctxPath + "/");
	}
	
	
}
