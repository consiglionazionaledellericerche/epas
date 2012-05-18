package controllers;

import java.util.List;

import models.Person;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
@Check("administrator")
public class Persons extends Controller{

	@Check("administrator")
	public void insert(){
		
	}
	

	public void update(){
		List<Person> personList = Person.findAll();
		render(personList);
	}
	
	
}
