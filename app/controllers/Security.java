package controllers;


import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;

import manager.ConfGeneralManager;
import models.Office;
import models.User;
import models.enumerate.Parameter;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.mvc.Http;
import play.utils.Java;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.hash.Hashing;

import dao.UserDao;

public class Security extends Secure.Security {

	@Inject
	private static UserDao userDao;
	@Inject
	private static ConfGeneralManager confGeneralManager;

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

	//FIXME residuo dei vecchi residui, rimuoverlo e sostituirlo nei metodi che lo utilizzano
	public final static String INSERT_AND_UPDATE_ADMINISTRATOR = "insertAndUpdateAdministrator";

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

	static Object invoke(String m, Object... args) throws Throwable {

		try {
			return Java.invokeChildOrStatic(Security.class, m, args);       
		} catch(InvocationTargetException e) {
			throw e.getTargetException();
		}
	}
	
	public static boolean checkForWebstamping(){
		if("true".equals(Play.configuration.getProperty(Clocks.SKIP_IP_CHECK))){
			return true;
		}
		String remoteAddress = Http.Request.current().remoteAddress;
		return !confGeneralManager.containsValue(
				Parameter.ADDRESSES_ALLOWED.description, remoteAddress).isEmpty();
	}
}
