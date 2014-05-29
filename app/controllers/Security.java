package controllers;


import java.util.ArrayList;
import java.util.List;
import models.Office;
import models.Permission;
import models.Person;
import models.User;
import models.UsersRolesOffices;
import play.Logger;
import play.cache.Cache;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.hash.Hashing;
import com.google.common.collect.Lists;


public class Security extends Secure.Security {
	
	/* Sviluppatore */
	
	public final static String DEVELOPER = "develop";
	
	/* Dipendente */
	
	public final static String EMPLOYEE = "employee";

	/* Amministratore Personale */
	
	public final static String VIEW_PERSON = "viewPerson";
	public final static String EDIT_PERSON = "editPerson";
	
	public final static String VIEW_PERSON_DAY = "viewPersonDay";
	public final static String EDIT_PERSON_DAY = "editPersonDay";

	public final static String VIEW_COMPETENCE = "viewCompetence";
	public final static String EDIT_COMPETENCE = "editCompetence";
	
	public final static String UPLOAD_SITUATION = "uploadSituation";
	

	
	/* Amministratore ePAS */
	
	public final static String VIEW_ABSENCE_TYPE = "viewAbsenceType";
	public final static String EDIT_ABSENCE_TYPE = "editAbsenceType";

	public final static String VIEW_CONFIGURATION = "viewConfiguration";
	public final static String EDIT_CONFIGURATION = "editConfiguration";

	public final static String VIEW_OFFICE = "viewOffice";
	public final static String EDIT_OFFICE = "editOffice";

	public final static String VIEW_WORKING_TIME_TYPE = "viewWorkingTimeType";
	public final static String EDIT_WORKING_TIME_TYPE = "editWorkingTimeType";
	
	public final static String VIEW_COMPETENCE_CODE = "viewCompetenceCode";
	public final static String EDIT_COMPETENCE_CODE = "editCompetenceCode";
	
	public final static String VIEW_ADMINISTRATOR = "viewAdministrator";
	public final static String EDIT_ADMINISTRATOR = "editAdministrator";
	
	
	
	

	
	
	
	
	
	public final static String VIEW_PERSON_LIST = "viewPersonList";

	public final static String INSERT_AND_UPDATE_PERSON = "insertAndUpdatePerson";
	public final static String INSERT_AND_UPDATE_STAMPING = "insertAndUpdateStamping";
	public final static String INSERT_AND_UPDATE_ABSENCE = "insertAndUpdateAbsence";


	public final static String INSERT_AND_UPDATE_WORKINGTIME = "insertAndUpdateWorkingTime";
	public final static String INSERT_AND_UPDATE_COMPETENCES = "insertAndUpdateCompetences";
	public final static String INSERT_AND_UPDATE_VACATIONS = "insertAndUpdateVacations";
	public final static String INSERT_AND_UPDATE_OFFICES = "insertAndUpdateOffices";
	public final static String INSERT_AND_UPDATE_CONFIGURATION = "insertAndUpdateConfiguration";
	public final static String INSERT_AND_UPDATE_ADMINISTRATOR = "insertAndUpdateAdministrator";
	

	

	public final static String INSERT_AND_UPDATE_PASSWORD = "insertAndUpdatePassword";	
	
	public final static String DELETE_PERSON = "deletePerson";
	
	
	
	
	
	public final static String PERMISSION_CACHE_PREFIX = "user-permission-office-";
		
	public final static String CACHE_DURATION = "30mn";
	

	static boolean authenticate(String username, String password) {
	    Logger.trace("Richiesta autenticazione di %s",username);

		User user = 
			User.find("SELECT u FROM User u where username = ? and password = ?", 
					username, Hashing.md5().hashString(password,  Charsets.UTF_8).toString()).first();

		if(user != null){
			Cache.set(username, user, CACHE_DURATION);
			Cache.set("userId", user.id, CACHE_DURATION);
			            
            //flash.success("Welcome, " + .name + ' ' + person.surname);
            Logger.info("user %s successfully logged in", user.username);
            //Logger.trace("Permission list for %s %s: %s", person.name, person.surname, person.permissions);
			return true;
		}
		
        // Oops
        Logger.info("Failed login for %s ", username);
        flash.put("username", username);
        flash.error("Login failed");
        return false;
    }
	
	private static Optional<User> getUser(String username){
		if (username == null || username.isEmpty()) {
			Logger.trace("getUSer failed for username %s", username);
			return Optional.<User>absent();
		}
		Logger.trace("Richiesta getUser(), username=%s", username);
		

		User user = User.find("byUsername", username).first();
		Logger.trace("USer.find('byUsername'), username=%s, e' %s", username, user);
		if (user == null){
			Logger.info("Security.getUser(): USer con username = %s non trovata nel database", username);
			return Optional.<User>absent();
		}
		return Optional.of(user);
	}
	
	public static Optional<User> getUser() {
		return getUser(connected());
	}
	
	static boolean check(String profile) {
		if (!getUser().isPresent()) {
			return false;
		}
//		User user = Security.getUser();
		
		User user = getUser().get();
		Permission permission = Permission.find("byDescription", profile).first();
		if(permission == null) {
			
			Logger.debug("Il Permission per la check del profilo %s è null o vuoto", profile);
			return false;
		}
		
		Long officeId = params.get("officeId") != null ? Long.valueOf(params.get("officeId")) : null;
		Long personId = params.get("personId") != null ? Long.valueOf(params.get("personId")) : null;

		/* caso richiesta solo su personId */
		if( personId != null && officeId == null) {
			
			Person person = Person.findById(personId);
			if( checkUro(user.userRoleOffices, permission, person.office) ) {
				
				return true;
			}
			return false;
		}
		
		/* caso richiesta solo su officeId */
		
		if( personId == null && officeId != null ) {
			
			Office office = Office.findById(officeId);
			if( checkUro(user.userRoleOffices, permission, office) ) {
				
				return true;
			}
			return false;
		}
		
		/* caso richiesta sia su personId che su officeId (rara, solo cambio sede persona) */

		if( personId != null && officeId != null ) { 
			
			Person person = Person.findById(personId);
			Office office = Office.findById(officeId);
			if( checkUro(user.userRoleOffices, permission, person.office) && checkUro(user.userRoleOffices, permission, office) ) {
				
				return true;
			}
			return false;
		}
		
		
		/* caso richiesta generica senza personId o officeId specificati */
		
		return checkUro(user.userRoleOffices, permission, null);
		
		
		
	
    }   
	
	private static boolean checkUro(List<UsersRolesOffices> uroList, Permission permission, Office office) {
		
		for(UsersRolesOffices uro : uroList) {

			if(office != null && !office.id.equals(uro.office.id)) {
				
				continue;
			}
			for(Permission p : uro.role.permissions) {

				if(p.description.equals(permission.description)) {

					return true;
				}
			}
		}
		return false;
	}
	
	static boolean permissionCheck() {
		String username = connected();
		if(username == null || username.isEmpty()){
			Logger.debug("Nessun utente connesso");
			return false;
		}
			
		Logger.trace("checking Admin permission for user %s", username);
		
		//TODO Rendere più specifici i controlli per gli account di Amministrazione
		/*
		if (getUserAllPermissions(username).size() > 1) {
			return true;
		}
		return false;
		*/
		return true;
    }   
	
//	private static User getUser(String username){
//		if (username == null || username.isEmpty()) {
//			Logger.trace("getUSer failed for username %s", username);
//			return null;
//		}
//		Logger.trace("Richiesta getUser(), username=%s", username);
//		
//
//		User user = User.find("byUsername", username).first();
//		Logger.trace("USer.find('byUsername'), username=%s, e' %s", username, user);
//		if (user == null){
//			Logger.info("Security.getUser(): USer con username = %s non trovata nel database", username);
//			return null;
//		}
//		return user;
//	}

	private static List<Permission> getUserAllPermissions(String username) {
		
		Optional<User> user = getUser(username);
		if (!user.isPresent()) {
			return Lists.newArrayList();
		}
		List<Permission> permissions = Cache.get(PERMISSION_CACHE_PREFIX + username, List.class);
		
		if (permissions == null) {
			user.get().refresh();
			permissions = user.get().getAllPermissions();
			Cache.set(PERMISSION_CACHE_PREFIX + username, permissions, CACHE_DURATION);
		}
		
		return permissions;
//		User user = getUser(username);
//		if (user == null) {
//
//			return null;
//		}
		//Set<UsersPermissionsOffices> permissions = Cache.get(PERMISSION_CACHE_PREFIX + username, Set.class);
		
//		List<Permission> permissions = Cache.get(PERMISSION_CACHE_PREFIX + username, List.class);
//		
//		if (permissions == null) {
//			user.refresh();
//			permissions = user.getAllPermissions();
//			Cache.set(PERMISSION_CACHE_PREFIX + username, permissions, CACHE_DURATION);
//		}
//		
//		return permissions;
	}
	
//	public static User getUser() {
//		return getUser(connected());
//	}
	
	public static List<Permission> getPersonAllPermissions() {
		return getUserAllPermissions(connected());
	}
	
	public static List<Office> getOfficeAllowed()
	{	
		if (!getUser().isPresent()) {
			return Lists.newArrayList();
		}
		return getUser().get().getOfficeAllowed();
//		User userLogged = getUser();
//		if(userLogged.person == null)
//			return Office.findAll();
//		if(userLogged.person != null)
//			return userLogged.person.getOfficeAllowed();
//		else
//			return null;
//		return getUser().getOfficeAllowed();
	}
	
	public static List<Office> getOfficeAllowed(String profile) {
		if (!getUser().isPresent()) {
			return Lists.newArrayList();
		}
		//User user = Security.getUser();
		List<Office> officeList = new ArrayList<Office>();
		
		for(UsersRolesOffices uro : getUser().get().userRoleOffices)  {
			
			for(Permission p : uro.role.permissions) {
				
				if(p.description.equals(profile)) {
					
					officeList.add(uro.office);
				}
			}
		}
		
		return officeList;
	}
	
	

}