package controllers;

import java.util.List;
import java.util.Set;

import models.Office;
import models.Permission;
import models.Person;
import models.RemoteOffice;
import models.User;
import play.Logger;
import play.cache.Cache;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;

public class Security extends Secure.Security {
	
	public final static String VIEW_PERSON_LIST = "viewPersonList";
	public final static String INSERT_AND_UPDATE_PERSON = "insertAndUpdatePerson";
	public final static String DELETE_PERSON = "deletePerson";
	public final static String INSERT_AND_UPDATE_STAMPING = "insertAndUpdateStamping";
	public final static String INSERT_AND_UPDATE_PASSWORD = "insertAndUpdatePassword";
	public final static String INSERT_AND_UPDATE_WORKINGTIME = "insertAndUpdateWorkingTime";
	public final static String INSERT_AND_UPDATE_ABSENCE = "insertAndUpdateAbsence";
	public final static String INSERT_AND_UPDATE_CONFIGURATION = "insertAndUpdateConfiguration";
	public final static String INSERT_AND_UPDATE_ADMINISTRATOR = "insertAndUpdateAdministrator";
	public final static String INSERT_AND_UPDATE_COMPETENCES = "insertAndUpdateCompetences";
	public final static String INSERT_AND_UPDATE_VACATIONS = "insertAndUpdateVacations";
	public final static String VIEW_PERSONAL_SITUATION ="viewPersonalSituation";
	public final static String UPLOAD_SITUATION = "uploadSituation";
	public final static String INSERT_AND_UPDATE_OFFICES = "insertAndUpdateOffices";
	public final static String DEVELOPER = "developer";
	
	public final static String PERMISSION_CACHE_PREFIX = "permission.";
		
	public final static String CACHE_DURATION = "30mn";
	

	static boolean authenticate(String username, String password) {
	    Logger.trace("Richiesta autenticazione di %s",username);

		User user = 
			User.find("SELECT u FROM User u where username = ? and password = ?", 
					username, Hashing.md5().hashString(password,  Charsets.UTF_8).toString()).first();

		if(user != null){
			Cache.set(username, user, CACHE_DURATION);
			Cache.set(PERMISSION_CACHE_PREFIX + username, user.getAllPermissions(), CACHE_DURATION);
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
	
	
	static boolean check(String profile) {
		String username = connected();
		if(username == null || username.isEmpty()){
			Logger.debug("Lo username per la check del profilo %s è null o vuoto", profile);
			return false;
		}
			
		Logger.trace("checking permission %s for user %s", profile, username);
		
		for (Permission permission : getUserAllPermissions(username)) {
			if (permission.description.equals(profile)) {
				return true;
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
		if (getUserAllPermissions(username).size() > 1) {
			return true;
		}
		return false;
    }   
	
	private static User getUser(String username){
		if (username == null || username.isEmpty()) {
			Logger.trace("getUSer failed for username %s", username);
			return null;
		}
		Logger.trace("Richiesta getUser(), username=%s", username);
		

		User user = User.find("byUsername", username).first();
		Logger.trace("USer.find('byUsername'), username=%s, e' %s", username, user);
		if (user == null){
			Logger.info("Security.getUser(): USer con username = %s non trovata nel database", username);
			return null;
		}
		return user;
	}

	private static Set<Permission> getUserAllPermissions(String username) {
		User user = getUser(username);
		if (user == null) {
			return ImmutableSet.of();
		}
		Set<Permission> permissions = Cache.get(PERMISSION_CACHE_PREFIX + username, Set.class);
		if (permissions == null) {
			user.refresh();
			permissions = user.getAllPermissions();
			Cache.set(PERMISSION_CACHE_PREFIX + username, permissions, CACHE_DURATION);
		}
		return permissions;
	}
	
	public static User getUser() {
		return getUser(connected());
	}
	
	public static Set<Permission> getPersonAllPermissions() {
		return getUserAllPermissions(connected());
	}
	
	public static List<Office> getOfficeAllowed()
	{
		User userLogged = getUser();
		if(userLogged.username.equals("admin"))
			return Office.findAll();
		if(userLogged.person!=null)
			return userLogged.person.getOfficeAllowed();
		else
			return null;
	}
	
	/**
	 * 
	 * @param user
	 * @param personId
	 * @return true se l'user possiede i diritti per visualizzarla in termini di office false altrimenti 
	 */
	public static boolean canUserSeePerson(User user, Long personId){
		Person person = Person.findById(personId);
		if(person == null)
			return false;
		
		//amministratore
		if(user.isAdmin())
			return true;
		
		if(user.person==null)	//questo evento non dovrebbe verificarsi
			return false;
		
		if(person.office.id == user.person.office.id)
			return true;
		
		for(RemoteOffice remote : user.person.office.remoteOffices){
			if(remote.id == person.office.id)
				return true;
		}
		return false;
	}
	
	/**
	 * Ritorna la persona identificata da personId se l'user loggato è effettivamente tale persona.
	 * @param personId
	 * @return
	 */
	public static Person getSelfPerson(Long personId) {
		if(personId == null)
			return null;
		User user = getUser();
		if(user == null)
			return null;
		if(user.person == null)
			return null;
		if(user.person.id.longValue() != personId.longValue())
			return null;
		return user.person;
				
	}

}
