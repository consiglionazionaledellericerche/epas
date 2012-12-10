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
		
		int year = params.get("year") != null ? params.get("year", Integer.class) : now.getYear(); 
		int month = params.get("month") != null ? params.get("month", Integer.class) : now.getMonthOfYear();
		ActionMenuItem action = params.get("action") != null && !params.get("action").equals("") ? ActionMenuItem.valueOf(params.get("action")) : ActionMenuItem.stampings;
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
		
		MainMenu mainMenu = new MainMenu(personId, year, month, action, persons);
		
		renderArgs.put("mainMenu", mainMenu);

	}
	
}
