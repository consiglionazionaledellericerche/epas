package controllers;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import models.Office;
import models.Permission;
import models.Person;
import models.User;
import models.UsersRolesOffices;
import models.enumerate.Parameter;
import play.Logger;
import play.cache.Cache;
import play.mvc.Http;
import play.utils.Java;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;

import dao.ConfGeneralDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.UserDao;

public class Security extends Secure.Security {

	@Inject
	private static UserDao userDao;
	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static RoleDao roleDao;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static ConfGeneralDao confGeneralDao;

	/* Client rest */

	public final static String REST = "rest";

	/* lettore badge */

	public final static String STAMPINGS_CREATE = "stampingsCreate";

	/* Sviluppatore */

	public final static String DEVELOPER = "developer";

	/* Dipendente */

	public final static String EMPLOYEE = "employee";

	/* Amministratore Personale */

	public final static String VIEW_PERSON = "viewPerson";
	public final static String EDIT_PERSON = "editPerson"; //per adesso utilizzato anche per la nuova gestione dei ticket


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

	public final static String VIEW_SHIFT = "viewShift";
	public final static String MANAGE_SHIFT = "manageShift";
	
	public final static String VIEW_REPERIBILITY = "viewReperibility";
	public final static String MANAGE_REPERIBILITY = "manageReperibility";
	 
//	FIXME residuo dei vecchi residui, rimuoverlo e sostituirlo nei metodi che lo utilizzano
	public final static String INSERT_AND_UPDATE_ADMINISTRATOR = "insertAndUpdateAdministrator";

	public final static String PERMISSION_CACHE_PREFIX = "user-permission-office-";

	public final static String CACHE_DURATION = "30mn";

	/**
	 * @param username
	 * @param password
	 * @return true se è autenticato, false altrimenti.
	 */
	static boolean authenticate(String username, String password) {
		Logger.trace("Richiesta autenticazione di %s",username);

		User user = userDao.getUserByUsernameAndPassword(username, Optional.fromNullable(Hashing.md5().hashString(password,  Charsets.UTF_8).toString()));

		if(user != null){
			Cache.set(username, user, CACHE_DURATION);
			Cache.set("userId", user.id, CACHE_DURATION);

			Logger.info("user %s successfully logged in from ip %s", user.username,
					Http.Request.current().remoteAddress);
			
//			Logger.info("headers request %s", Http.Request.current().headers);
		
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

		//cache
		//User user = (User)Cache.get(username);
		//if(user!=null)
		//	return Optional.of(user);

		//db
		User user = userDao.getUserByUsernameAndPassword(username, Optional.<String>absent());
		//User user = User.find("byUsername", username).first();
		Logger.trace("User.find('byUsername'), username=%s, e' %s", username, user);
		if (user == null){
			Logger.info("Security.getUser(): USer con username = %s non trovata nel database", username);
			return Optional.<User>absent();
		}
		//Cache.set(username, user, CACHE_DURATION);
		return Optional.of(user);
	}

	static String connected() {
		if (request == null){
			return null;
		}
		if (request.user != null) {
			return request.user;
		} else {
			return Secure.Security.connected();
		}
	}

	public static Optional<User> getUser() {
		return getUser(connected());
	}

	static boolean check(String profile) {

		if (!getUser().isPresent()) {
			return false;
		}

		final User user = getUser().get();
		final Permission permission = roleDao.getPermissionByDescription(profile);
		//final Permission permission = Permission.find("byDescription", profile).first();
		if(permission == null) {

			Logger.debug("Il Permission per la check del profilo %s è null o vuoto", profile);
			return false;
		}

		Long officeId = params.get("officeId") != null ? Long.valueOf(params.get("officeId")) : null;
		Long personId = params.get("personId") != null ? Long.valueOf(params.get("personId")) : null;

		/* caso richiesta solo su personId */
		if( personId != null && officeId == null) {

			Person person = personDao.getPersonById(personId);
			//Person person = Person.findById(personId);
			if( checkUro(user.usersRolesOffices, permission, person.office) ) {

				return true;
			}
			return false;
		}

		/* caso richiesta solo su officeId */

		if( personId == null && officeId != null ) {

			Office office = officeDao.getOfficeById(officeId);
			//Office office = Office.findById(officeId);
			if( checkUro(user.usersRolesOffices, permission, office) ) {

				return true;
			}
			return false;
		}

		/* caso richiesta sia su personId che su officeId (rara, solo cambio sede persona) */

		if( personId != null && officeId != null ) { 

			Person person = personDao.getPersonById(personId);
			//Person person = Person.findById(personId);
			Office office = officeDao.getOfficeById(officeId);
			//Office office = Office.findById(officeId);
			if( checkUro(user.usersRolesOffices, permission, person.office) && checkUro(user.usersRolesOffices, permission, office) ) {

				return true;
			}
			return false;
		}


		/* caso richiesta generica senza personId o officeId specificati */

		return checkUro(user.usersRolesOffices, permission, null);
	}   

	private static boolean checkUro(List<UsersRolesOffices> uroList, Permission permission, Office office) {

		for(UsersRolesOffices uro : uroList) {
			if(office != null && !office.id.equals(uro.office.id)) {

				continue;
			}
			List<Permission> permissionList = Lists.newArrayList(uro.role.permissions);
			for(Permission p : permissionList) {

				if(p.description.equals(permission.description)) {
					return true;
				}
			}
		}
		return false;
	}

	static boolean permissionCheck() {
		final String username = connected();
		if (Strings.isNullOrEmpty(username)) {
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

	private static List<Permission> getUserAllPermissions(String username) {

		final Optional<User> user = getUser(username);
		if (!user.isPresent()) {
			return Lists.newArrayList();
		}
		@SuppressWarnings("unchecked")
		List<Permission> permissions = 
			Cache.get(PERMISSION_CACHE_PREFIX +	username, List.class);

		if (permissions == null) {
			user.get().refresh();
			//permissions = user.get().getAllPermissions();
			permissions = userDao.getAllPermissions(user.get());
			Cache.set(PERMISSION_CACHE_PREFIX + username, permissions, CACHE_DURATION);
		}

		return permissions;
	}

	public static List<Permission> getPersonAllPermissions() {
		return getUserAllPermissions(connected());
	}

	public static List<Office> getOfficeAllowed(String profile) {
		if (!getUser().isPresent()) {
			return Lists.newArrayList();
		}
		final List<Office> officeList = new ArrayList<Office>();

		for(UsersRolesOffices uro : getUser().get().usersRolesOffices)  {
			for(Permission p : uro.role.permissions) {
				if(p.description.equals(profile)) {
					officeList.add(uro.office);
				}
			}
		}
		return officeList;
	}

	static Object invoke(String m, Object... args) throws Throwable {

		try {
			return Java.invokeChildOrStatic(Security.class, m, args);       
		} catch(InvocationTargetException e) {
			throw e.getTargetException();
		}
	}
	
	public static boolean checkForWebstamping(){
		String remoteAddress = Http.Request.current().remoteAddress;
		return !confGeneralDao.containsValue(
				Parameter.ADDRESSES_ALLOWED.description, remoteAddress).isEmpty();
	}
}
