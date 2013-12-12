/**
 * 
 */
package controllers;

import controllers.Secure.Security;
import it.cnr.iit.epas.AuthInfoBinder;
import it.cnr.iit.epas.JsonStampingBinder;
import models.exports.AuthInfo;
import play.Logger;
import play.data.binding.As;
import play.mvc.Controller;

/**
 * @author cristian
 *
 */
public class SecureJson extends Controller {

	/**
	 * Questo metodo deve essere chiamata passando i corretti header http
	 * Content-type: application/json
	 * Accept: application/json
	 * 
	 * @param body json nella forma {"username": "cristian.lucchesi", "password": "lapassword"}
	 * 
	 * Restituisce {"login" : "ok"} se il logout è andato a buon fine, 
	 * 	altrimenti {"login" : "ko"}

	 */
	public static void login(@As(binder=AuthInfoBinder.class) AuthInfo body) {
		Logger.trace("Chiamata SecureJson.login, authInfo=%s", body);

		String login = "ko";
		if (body != null && 
				Security.authenticate(body.getUsername(), body.getPassword())) {
			// Mark user as connected
			session.put("username", body.getUsername());
			login = "ok";
		} 
		Logger.debug("Login Json utente: %s, status=%s", body.getUsername(), login);

		renderJSON("{\"login\":\"" + login +"\"}");
	}

	/**
	 * Questo metodo deve essere chiamata passando i corretti header http
	 * Content-type: application/json
	 * Accept: application/json
	 * 
	 * Restituisce {"logout" : "ok"} se il logout è andato a buon fine, 
	 * 	altrimenti {"logout" : "ko"}
	 */
	public static void logout() {
		String logout = "ko";

		if(session.contains("username")) {
			session.clear();
			logout = "ok";
			Logger.debug("Logout Json utente: %s, status=%s", session.contains("username"), logout);
		}

		renderJSON("{\"logout\":\"" + logout +"\"}");        

	}
}
