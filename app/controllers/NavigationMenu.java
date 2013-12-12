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
		Integer day = flash.get("day") != null ? Integer.valueOf(flash.get("day")) : null;
		int year = flash.get("year") != null ? Integer.valueOf(flash.get("year")) : now.getYear(); 
		int month = flash.get("month") != null  ? Integer.valueOf(flash.get("month")) : now.getMonthOfYear();
		if(month == 0)
			month = now.getMonthOfYear();
		
		Long personId =  flash.get("personId") != null ? Long.parseLong(flash.get("personId")) : null;
		if(personId == null)
			personId = Security.getPerson().id;
		//Person person = Person.findById(personId);
		Logger.debug("Action= %s",flash.get("method"));
		Logger.debug("PersonId= %d", personId);
		
		ActionMenuItem action = flash.get("method") != null && !flash.get("method").equals("") && personId != null ? ActionMenuItem.valueOf(flash.get("method")) : ActionMenuItem.stampingsAdmin;
		
		//Logger.debug("Appena assegnata la action nel metodo injectMenu della classe NavigationMenu. La action è: %s", action.getDescription());
		
	
		List<Person> persons = (List<Person>) Cache.get("persons");
		
		if (persons == null) {
			
			persons = new ArrayList<Person>();
			List<Person> genericPerson = Person.find("Select p from Person p where p.name <> ? and p.name <> ? order by p.surname", "Admin", "epas").fetch();
			for(Person p : genericPerson){
				
				Contract c = Contract.find("Select c from Contract c where c.person = ? and ((c.beginContract != null and c.expireContract = null) or " +
						"(c.expireContract > ?) or (c.beginContract = null and c.expireContract = null)) order by c.beginContract desc limit 1", p, new LocalDate(year,month,1)).first();
				
				if(c != null && c.onCertificate == true){
					persons.add(p);
					
				}
				
			}
			Cache.set("persons", persons, "5mn");
		}
		//Logger.debug("nella injectMenu la action è: %s", action.getDescription());
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
