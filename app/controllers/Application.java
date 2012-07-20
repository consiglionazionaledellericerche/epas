package controllers;

import it.cnr.iit.epas.ActionMenuItem;
import it.cnr.iit.epas.MainMenu;

import java.util.List;

import org.joda.time.LocalDate;

import models.Permission;
import models.Person;
import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Application extends Controller {
    
	@Before
	public static void populateCommonData() {
		List<Person> personList = Person.find("Select p from Person p order by p.surname").fetch();
		renderArgs.put("personList", personList);	
	}

    public static void indexAdmin() {
		Logger.debug("chiamato metodo indexAdmin dell'Application controller");
       	render();
    }
    
}