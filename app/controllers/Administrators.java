package controllers;

import java.util.ArrayList;
import java.util.List;

import models.Permission;
import models.Person;
import models.User;
import models.UsersPermissionsOffices;
import play.Play;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Administrators extends Controller {

	private static final String SUDO_USERNAME = "sudo.username";
	private static final String USERNAME = "username";

	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void list(){
		/**
		 * TODO: cambiare i permessi in relazione al fatto che l'utente loggato sia effettivamente attivo nella data in cui visita
		 * la pagina di lista amministratori
		 */
		List<Person> administratorList = Person.find("Select p from Person p where p.name <> ? order by p.surname", "Admin").fetch();
		List<User> userList = new ArrayList<User>();
		for(Person person : administratorList)
		{
			userList.add(person.user);
		}

		render(userList);
	}

	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void discard(){
		Administrators.list();
	}

	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void edit(Long adminId){
		if(adminId != null){
			Person person = Person.findById(adminId);
			render(person);
		}
	}
	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void update(){
		long personId = params.get("personId", Long.class);
		
		Person person = Person.findById(personId);
		User user = person.user;
		String viewPersonList = params.get("viewPersonList");
		String insertAndUpdatePerson = params.get("insertAndUpdatePerson");
		String deletePerson = params.get("deletePerson");
		String insertAndUpdateStamping = params.get("insertAndUpdateStamping");
		String insertAndUpdatePassword = params.get("insertAndUpdatePassword");
		String insertAndUpdateWorkingTime = params.get("insertAndUpdateWorkingTime");
		String insertAndUpdateAbsence = params.get("insertAndUpdateAbsence");
		String insertAndUpdateConfiguration = params.get("insertAndUpdateConfiguration");
		String insertAndUpdateAdministrator = params.get("insertAndUpdateAdministrator");
		String insertAndUpdateOffices = params.get("insertAndUpdateOffices");
		String insertAndUpdateVacations = params.get("insertAndUpdateVacations");
		String insertAndUpdateCompetences = params.get("insertAndUpdateCompetences");
		String uploadSituation = params.get("uploadSituation");
		if(viewPersonList.equals("true") && !user.isViewPersonAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "viewPersonList").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
		}
		else if(viewPersonList.equals("false") && user.isViewPersonAvailable()){
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "viewPersonList").first();
			//Permission p = Permission.find("Select p from Permission p where p.description = ? ", "viewPersonList").first();
			//user.permissions.remove(p);
			user.userPermissionOffices.remove(upo);
		}
		
		if(insertAndUpdatePerson.equals("true") && !user.isInsertAndUpdatePersonAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdatePerson").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
	//		user.permissions.add(p);
		}
		else if(insertAndUpdatePerson.equals("false") && user.isInsertAndUpdatePersonAvailable()){
	//		Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdatePerson").first();
	//		user.permissions.remove(p);
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "insertAndUpdatePerson").first();
			user.userPermissionOffices.remove(upo);
		}
		
		if(deletePerson.equals("true") && !user.isDeletePersonAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "deletePerson").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
	//		user.permissions.add(p);
		}
		else if(deletePerson.equals("false") && user.isDeletePersonAvailable()){
//			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "deletePerson").first();
//			user.permissions.remove(p);
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "deletePerson").first();
			user.userPermissionOffices.remove(upo);
		}
		
		if(insertAndUpdateStamping.equals("true") && !user.isInsertAndUpdateStampingAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateStamping").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
	//		user.permissions.add(p);
		}
		else if(insertAndUpdateStamping.equals("false") && user.isInsertAndUpdateStampingAvailable()){
//			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateStamping").first();
//			user.permissions.remove(p);
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "insertAndUpdateStamping").first();
			user.userPermissionOffices.remove(upo);
		}
		
		if(insertAndUpdatePassword.equals("true") && !user.isInsertAndUpdatePasswordAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdatePassword").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
	//		user.permissions.add(p);
		}	
		else if(insertAndUpdatePassword.equals("false") && user.isInsertAndUpdatePasswordAvailable()){
//			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdatePassword").first();
//			user.permissions.remove(p);
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "insertAndUpdatePassword").first();
			user.userPermissionOffices.remove(upo);
		}
		
		if(insertAndUpdateWorkingTime.equals("true") && !user.isInsertAndUpdateWorkinTimeAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateWorkingTime").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
	//		user.permissions.add(p);
		}	
		else if(insertAndUpdateWorkingTime.equals("false") && user.isInsertAndUpdateWorkinTimeAvailable()){
//			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateWorkingTime").first();
//			user.permissions.remove(p);
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "insertAndUpdateWorkingTime").first();
			user.userPermissionOffices.remove(upo);
		}
		
		if(insertAndUpdateAbsence.equals("true") && !user.isInsertAndUpdateAbsenceAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateAbsence").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
	//		user.permissions.add(p);
		}
		else if(insertAndUpdateAbsence.equals("false") && user.isInsertAndUpdateAbsenceAvailable()){
//			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateAbsence").first();
//			user.permissions.remove(p);
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "insertAndUpdateAbsence").first();
			user.userPermissionOffices.remove(upo);
		}
		
		if(insertAndUpdateConfiguration.equals("true") && !user.isInsertAndUpdateConfigurationAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateConfiguration").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
	//		user.permissions.add(p);
		}
		else if(insertAndUpdateConfiguration.equals("false") && user.isInsertAndUpdateConfigurationAvailable()){
//			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateConfiguration").first();
//			user.permissions.remove(p);
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "insertAndUpdateConfiguration").first();
			user.userPermissionOffices.remove(upo);
		}
		
		if(insertAndUpdateAdministrator.equals("true") && !user.isInsertAndUpdateAdministratorAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateAdministrator").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
	//		user.permissions.add(p);
		}
		else if(insertAndUpdateAdministrator.equals("false") && user.isInsertAndUpdateAdministratorAvailable()){
//			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateAdministrator").first();
//			user.permissions.remove(p);
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "insertAndUpdateAdministrator").first();
			user.userPermissionOffices.remove(upo);
		}
		
		if(insertAndUpdateOffices.equals("true") && !user.isInsertAndUpdateOfficesAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateOffices").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
	//		user.permissions.add(p);
		}
		else if(insertAndUpdateOffices.equals("false") && user.isInsertAndUpdateOfficesAvailable()){
//			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateOffices").first();
//			user.permissions.remove(p);
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "insertAndUpdateOffices").first();
			user.userPermissionOffices.remove(upo);
			
		}
		
		if(insertAndUpdateCompetences.equals("true") && !user.isInsertAndUpdateCompetenceAndOvertimeAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateCompetences").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
	//		user.permissions.add(p);
		}
		else if(insertAndUpdateCompetences.equals("false") && user.isInsertAndUpdateCompetenceAndOvertimeAvailable()){
//			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateCompetences").first();
//			user.permissions.remove(p);
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "insertAndUpdateCompetences").first();
			user.userPermissionOffices.remove(upo);
		}
		
		if(insertAndUpdateVacations.equals("true") && !user.isInsertAndUpdateVacationsAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateVacations").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
	//		user.permissions.add(p);
		}
		else if(insertAndUpdateVacations.equals("false") && user.isInsertAndUpdateVacationsAvailable()){
//			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateVacations").first();
//			user.permissions.remove(p);
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "insertAndUpdateVacations").first();
			user.userPermissionOffices.remove(upo);
		}
		if(uploadSituation.equals("true") && !user.isUploadSituationAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "uploadSituation").first();
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.permission = p;
			upo.user = user;
			upo.office = user.person.office;
			upo.save();			
			user.userPermissionOffices.add(upo);
	//		user.permissions.add(p);
		}
		else if(uploadSituation.equals("false") && user.isUploadSituationAvailable()){
//			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "uploadSituation").first();
//			user.permissions.remove(p);
			UsersPermissionsOffices upo = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where" +
					" upo.user = ? and upo.office = ? and upo.permission.description = ?", user, user.person.office, "uploadSituation").first();
			user.userPermissionOffices.remove(upo);
		}
		user.save();
		flash.success(String.format("Aggiornati con successo i permessi per %s %s", user.person.name, user.person.surname));
		Administrators.list();
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void delete(Long adminId){
		Person person = Person.findById(adminId);
		person.user.userPermissionOffices.clear();
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
