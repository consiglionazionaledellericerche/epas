package controllers;

import java.util.List;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;

import play.Logger;
import play.cache.Cache;
import play.db.jpa.JPA;
import play.db.jpa.GenericModel.JPAQuery;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import models.Permission;
import models.Person;

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

		Person person = 
			Person.find("SELECT p FROM Person p where username = ? and password = ?", 
					username, Hashing.md5().hashString(password,  Charsets.UTF_8).toString()).first();

		if(person != null){
			Cache.set(username, person, CACHE_DURATION);
			Cache.set(PERMISSION_CACHE_PREFIX + username, person.getAllPermissions(), CACHE_DURATION);
			Cache.set("personId", person.id, CACHE_DURATION);
			            
            flash.success("Welcome, " + person.name + ' ' + person.surname);
            Logger.info("person %s successfully logged in", person.username);
            Logger.trace("Permission list for %s %s: %s", person.name, person.surname, person.permissions);
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
		
		for (Permission permission : getPersonAllPermissions(username)) {
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
			if (getPersonAllPermissions(username).size() > 1) {
				return true;
			}
		return false;
    }   
	
	private static Person getPerson(String username){
		if (username == null || username.isEmpty()) {
			Logger.trace("getPerson failed for username %s", username);
			return null;
		}
		Logger.trace("Richiesta getPerson(), username=%s", username);
		

		Person person = Person.find("byUsername", username).first();
		Logger.trace("Person.find('byUsername'), username=%s, e' %s", username, person);
		if (person == null){
			Logger.info("Security.getPerson(): Person con username = %s non trovata nel database", username);
			return null;
		}
		return person;
	}
	
	private static Set<Permission> getPersonAllPermissions(String username) {
		Person person = getPerson(username);
		if (person == null) {
			return ImmutableSet.of();
		}
		Set<Permission> permissions = Cache.get(PERMISSION_CACHE_PREFIX + username, Set.class);
		if (permissions == null) {
			person.refresh();
			permissions = person.getAllPermissions();
			Cache.set(PERMISSION_CACHE_PREFIX + username, permissions, CACHE_DURATION);
		}
		return permissions;
	}
	
	public static Person getPerson() {
		return getPerson(connected());
	}
	
	public static Set<Permission> getPersonAllPermissions() {
		return getPersonAllPermissions(connected());
	}

}
