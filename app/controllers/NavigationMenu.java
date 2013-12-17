/**
 * 
 */
package controllers;

import java.util.ArrayList;
import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import it.cnr.iit.epas.MainMenu;
import models.Contract;
import models.Person;

import org.joda.time.LocalDate;

import play.Logger;
import play.cache.Cache;
import play.mvc.After;
import play.mvc.Before;
import play.mvc.Controller;

/**
 * @author cristian
 *
 */
public class NavigationMenu extends Controller {

	@Before
	public static void injectMenu() { 
		LocalDate now = new LocalDate();
		Integer month;
		Integer year;
		Integer day;
		
		String method = session.get("methodSelected");
		
		if(session.get("monthSelected")==null)
			month = now.getMonthOfYear();
		else
			month = Integer.parseInt(session.get("monthSelected"));
		
		if(session.get("yearSelected")==null)
			year = now.getYear();
		else
			year = Integer.parseInt(session.get("yearSelected"));
		
		if(session.get("daySelected")==null)
			day = now.getDayOfMonth();
		else
			day = Integer.parseInt(session.get("daySelected"));
		
		
		
		/*
		Integer day = params.get("day") != null ? Integer.valueOf(params.get("day")) : null;
		int year 	= params.get("year") != null ? Integer.valueOf(params.get("year")) : now.getYear(); 
		int month 	= params.get("month") != null  ? Integer.valueOf(params.get("month")) : now.getMonthOfYear();
		if(month == 0)
			month = now.getMonthOfYear();
		*/
		Long personId;
		if( flash.get("personId") != null )
			personId = Long.parseLong(flash.get("personId"));
		else
			personId = Security.getPerson().id;


	
		
		ActionMenuItem action;
		if(method != null && !method.equals("")) 
			action = ActionMenuItem.valueOf(method);
		else
			action = ActionMenuItem.stampingsAdmin;
		
		//lista delle persone 
		List<Person> persons = new ArrayList<Person>();
		persons = Person.getActivePersonsInMonth(month, year);

		
		
		MainMenu mainMenu = null;
		if(action.getDescription().equals("Riepilogo mensile"))
			mainMenu = new MainMenu(year, month, action);
		if(action.getDescription().equals("Presenza giornaliera")){
			day = flash.get("day") != null ? Integer.valueOf(flash.get("day")) : now.getDayOfMonth();
			mainMenu = new MainMenu(personId, year, month, day, action, persons);			
		}
		
		else{
			mainMenu = new MainMenu(personId, year, month, action, persons);
			
		}		
		
		renderArgs.put("mainMenu", mainMenu);

	}
	
}
