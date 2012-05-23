package controllers;

import java.util.List;

import models.MonthRecap;
import models.Person;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
@Check("administrator")
public class Persons extends Controller{

	@Check("administrator")
	public static void insert(){
		
	}
	

	public static void update(){
		
		List<Person> personList = Person.find("Select per from Person per order by per.surname").fetch();
		//Logger.warn("la lista di persone è: "+personList.toString());
		render(personList);
	}
	
	
}
