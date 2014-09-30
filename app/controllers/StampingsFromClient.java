package controllers;

import controllers.Resecure.BasicAuth;
import it.cnr.iit.epas.JsonStampingBinder;
import models.Person;
import models.exports.StampingFromClient;
import play.Logger;
import play.data.binding.As;
import play.mvc.Controller;
import play.mvc.With;

//@With(Resecure.class)
public class StampingsFromClient extends Controller{

	/**
	 * Aggiunge una timbratura ad una persona
	 *  
	 * @param body
	 */
	//@BasicAuth
	public static String create(@As(binder=JsonStampingBinder.class) StampingFromClient body) {

		Logger.debug("create: Received stampingFromClient %s", body);
		
		if (body == null) {
			badRequest();	
		}
		
		if (Person.createStamping(body)) {
			return "OK";
		}
		
		
		return "KO";
	}
}
