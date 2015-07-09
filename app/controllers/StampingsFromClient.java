package controllers;


import it.cnr.iit.epas.JsonAbsenceBinder;
import it.cnr.iit.epas.JsonStampingBinder;

import javax.inject.Inject;

import com.google.common.base.Optional;

import manager.AbsenceManager;
import manager.StampingManager;
import manager.cache.AbsenceTypeManager;
import models.AbsenceType;
import models.Person;
import models.exports.AbsenceFromClient;
import models.exports.StampingFromClient;
import play.data.binding.As;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;
import controllers.Resecure.BasicAuth;
import dao.PersonDao;


@With( {Resecure.class, RequestInit.class} )
public class StampingsFromClient extends Controller{

	@Inject
	static SecurityRules rules;
	@Inject
	static StampingManager stampingManager;
	@Inject
	static AbsenceTypeManager absenceTypeManager;
	@Inject
	static AbsenceManager absenceManager;
	@Inject
	static PersonDao personDao;
	
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
	
	@BasicAuth
	public static void absence(AbsenceFromClient body) {
		
		if (body == null) {
			badRequest();
		}
		
//		AbsenceType abt = absenceTypeManager.getAbsenceType(body.code);
//		absenceManager.insertAbsence(body.person, body.date, Optional.fromNullable(body.date), 
//				abt, Optional.<Blob>absent(), Optional.<String>absent(), false);
		
		
		renderText("ok");
		
	}
	
	
}
