package controllers;

import models.Person;

public class Security extends Secure.Security{
	
	static boolean authenticate(String username, String password) {
        Person person = Person.find("byUsername", username).first();
        return person != null && person.password.equals(password);
    }
	
	static boolean check(String profile) {
        Person person = Person.find("byUsername", connected()).first();
        if (person.permissions.get(0).description.equals("administrator")) {
            return true;
        }
        else {
            return false;
        }
    }    

}
