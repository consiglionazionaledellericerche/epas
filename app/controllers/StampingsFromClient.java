package controllers;


import it.cnr.iit.epas.JsonStampingBinder;

import javax.inject.Inject;

import manager.StampingManager;
import models.exports.StampingFromClient;
import play.data.binding.As;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;
import controllers.Resecure.BasicAuth;


@With( {Resecure.class, RequestInit.class} )
public class StampingsFromClient extends Controller{

	@Inject
	static SecurityRules rules;
	@Inject
	static StampingManager stampingManager;
	
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
				
		if (stampingManager.createStamping(body)) {
			return "OK";
		}
		
		return "KO";
		
		 
	}
	
	
}
