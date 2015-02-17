package controllers;


import it.cnr.iit.epas.JsonStampingBinder;

import javax.inject.Inject;

import manager.StampingManager;
import models.Office;
import models.User;
import models.exports.StampingFromClient;
import play.data.binding.As;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;
import controllers.Resecure.BasicAuth;
import dao.OfficeDao;


@With( {Resecure.class, RequestInit.class} )
public class StampingsFromClient extends Controller{

	@Inject
	static SecurityRules rules;
	
	/**
	 * Aggiunge una timbratura ad una persona
	 *  
	 * @param body
	 */
	@BasicAuth
	public static String create(@As(binder=JsonStampingBinder.class) StampingFromClient body) {

		
		//rulesssssssssssssss
		
		if (body == null) {
			badRequest();	
		}
				
		if (StampingManager.createStamping(body)) {
			return "OK";
		}
		
		return "KO";
		
		 
	}
	
	//@BasicAuth
	public static void prova(Long officeId) {

		Office office = OfficeDao.getOfficeById(officeId);
		//Office office = Office.findById(officeId);
		
		User user = Security.getUser().get();
		
		rules.checkIfPermitted(office);
		
		renderText("Accettata timbratura da badge: " + user.username + " per l'office: " + office.name + "\n");
		 
	}
	
	
	
}
