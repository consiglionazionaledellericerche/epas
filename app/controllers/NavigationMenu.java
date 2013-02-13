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
		int day = params.get("day") != null ? params.get("day", Integer.class).intValue() : 1;
		int year = params.get("year") != null ? params.get("year", Integer.class).intValue() : now.getYear(); 
		int month = params.get("month") != null ? params.get("month", Integer.class).intValue() : now.getMonthOfYear();
		ActionMenuItem action = params.get("action") != null && !params.get("action").equals("") ? ActionMenuItem.valueOf(params.get("action")) : ActionMenuItem.stampingsAdmin;
		Logger.debug("nella injectMenu la action è: %s", action);
		Long personId =  params.get("personId") != null ? params.get("personId", Long.class) : null;
	
		List<Person> persons = (List<Person>) Cache.get("persons");
		
		if (persons == null) {
			
			persons = new ArrayList<Person>();
			List<Person> genericPerson = Person.find("Select p from Person p order by p.surname").fetch();
			for(Person p : genericPerson){
				Contract c = Contract.find("Select c from Contract c where c.person = ? and ((c.beginContract != null and c.expireContract = null) or " +
						"(c.expireContract > ?) or (c.beginContract = null and c.expireContract = null)) order by c.beginContract desc limit 1", p, now).first();
				if(c != null && c.onCertificate == true)
					persons.add(p);
			}
//			persons = Person.find("Select distinct p from Person p, Contract c where c.person = p and c.onCertificate = ? " +
//					"order by p.surname", true).fetch();
			Cache.set("persons", persons, "5mn");
		}
		Logger.debug("nella injectMenu la action è: %s", action.getDescription());
		MainMenu mainMenu = null;
		if(!action.getDescription().equals("Presenza giornaliera"))
			mainMenu = new MainMenu(personId, year, month, action, persons);
		else
			mainMenu = new MainMenu(personId, year, month, day, action, persons);
		
		renderArgs.put("mainMenu", mainMenu);

	}
	
}
