package controllers;

import models.Person;
import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;

public class Application extends Controller {
    
	public static final String USERNAME_SESSION_KEY = "username";
	public static final String PERSON_ID_SESSION_KEY = "person_id";
		
    @Before
    static void addPerson() {
        Person person = connected();
        if(person != null) {
            renderArgs.put("person", person);
        }
    }
    
    static Person connected() {
        if(renderArgs.get(USERNAME_SESSION_KEY) != null) {
            return renderArgs.get(USERNAME_SESSION_KEY, Person.class);
        }
        String username = session.get(USERNAME_SESSION_KEY);

        if(username != null) {
            return Person.find("byUsername", username).first();
        } 
        return null;
    }
    
	public static void index() {
        if(connected() != null) {
        	Person person = connected();
        	if(person.permissions.isEmpty())
        		Stampings.show();
        	else
        		Stampings.showAdmin();
        }
        render();
    } 
    
    public static void login(String username, String password) {
        Person person = Person.find("SELECT p FROM Person p where username = ? and password = md5(?)", username, password).first();
        if(person != null) {
            session.put(USERNAME_SESSION_KEY, person.username);
            session.put(PERSON_ID_SESSION_KEY, person.id);
            
            flash.success("Welcome, " + person.name + person.surname);
            Logger.info("person %s successfully logged in", person.username);
            Logger.debug("%s: person.id = %d", person.username, person.id);
            
            Stampings.show();
        }
        // Oops
        flash.put("username", username);
        flash.error("Login failed");
        index();
    }
    
    public static void logout() {
        session.clear();
        index();
    }
    
}