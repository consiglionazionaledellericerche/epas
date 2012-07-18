package controllers;

import java.util.List;

import models.Permission;
import models.Person;
import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class Application extends Controller {
    
	@Before
	public static void populateCommonData() {
		List<Person> personList = Person.find("Select p from Person p order by p.surname").fetch();
		renderArgs.put("personList", personList);	
	}
	
	public static void index() {		
		Logger.debug("chiamato metodo index dell'Application controller");
		
        if(Security.getPersonAllPermissions().isEmpty()) 
        	Stampings.show();
        else
        	Stampings.showAdmin();
        
        render();
    } 
    
    
}