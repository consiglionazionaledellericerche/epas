package controllers;

import org.joda.time.LocalDate;

import models.MonthRecap;
import models.Person;
import models.YearRecap;
import play.Logger;
import play.mvc.Controller;

public class SwitchTemplate extends Controller{

	private static void show(Long id){

		String s = params.get("view");

		if(s.equals("timbrature")){
			
	    	Stampings.show();
		}
		if(s.equals("assenzeAnnuali")){
			YearlyAbsences.show();
		}
		if(s.equals("assenzeMensili")){
			Absences.show();
		}
		
	}
	
	public static void show() {
    	show(Long.parseLong(session.get(Application.PERSON_ID_SESSION_KEY)));
    }
	
	
}
