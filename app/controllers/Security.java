package controllers;

import play.Logger;
import play.db.jpa.GenericModel.JPAQuery;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import models.Permission;
import models.Person;

public class Security extends Secure.Security {
	
	public final static String VIEW_PERSON_LIST = "viewPersonList";
	public final static String INSERT_AND_UPDATE_PERSON = "insertAndUpdatePerson";
	public final static String DELETE_PERSON = "deletePerson";

		
	static boolean authenticate(String username, String password) {
        Person person = Person.find("byUsername", username).first();
        return person != null && person.password.equals(password);
    }
	
	static boolean check(String profile) {
		String username = connected();
		Logger.trace("checking permission %s for user %s", profile, username);
		
        Person person = Person.find("byUsername", connected()).first();
        
        Permission permission = Permission.find("byDescription", profile).first();
        if (permission == null) {
        	throw new IllegalArgumentException(
        		String.format("permission %s doesn't exist in database", profile));
        }
        
        boolean authorized = person.getAllPermissions().contains(permission);
        if (!authorized) {
        	Logger.debug("User %s not authorized for %s", username, profile);
        }
        
        return authorized;
    }    

}
