/**
 * 
 */
package controllers;

import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import it.cnr.iit.epas.MainMenu;

import models.Person;

import org.joda.time.LocalDate;

import play.Logger;
import play.cache.Cache;
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
		
		int year = params.get("year") != null ? params.get("year", Integer.class) : now.getYear(); 
		int month = params.get("month") != null ? params.get("month", Integer.class) : now.getMonthOfYear();
		ActionMenuItem action = params.get("action") != null && !params.get("action").equals("") ? ActionMenuItem.valueOf(params.get("action")) : ActionMenuItem.stampings;
		Long personId =  params.get("personId") != null ? params.get("personId", Long.class) : null;
		
		List<Person> persons = (List<Person>) Cache.get("persons");
		
		if (persons == null) {
			persons = Person.find("Select p from Person p order by p.surname").fetch();
			Cache.set("persons", persons, "5mn");
		}
		
		MainMenu mainMenu = new MainMenu(personId, year, month, action, persons);
		
		renderArgs.put("mainMenu", mainMenu);

	}
	
}
