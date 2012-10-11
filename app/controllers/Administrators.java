package controllers;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import models.ContactData;
import models.Contract;
import models.Location;
import models.Permission;
import models.Person;
import models.PersonTags;
import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Administrators extends Controller {
	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void list(){
		List<Person> administratorList = new ArrayList<Person>();
		List<Person> personList = Person.findAll();
		for(Person p : personList){
			if(p.permissions.size() > 0){
				administratorList.add(p);
			}
			
		}
				
		render(administratorList);
	}

	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void save(@Valid @Required Person person, @Valid List<Permission> permissionList) {
	
		if(validation.hasErrors()) {
			if(request.isAjax()) error("Invalid value");
			render("@list");
		}
		
		person.permissions = permissionList;
		person.save();
		flash.success(
				String.format("Aggiornati permessi per %s %s con successo", person.name, person.surname));
		Application.indexAdmin();
		
	}
	
	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void discard(){
		list();
	}
	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void insertAdmin(Long id){
		if(id == null) {
			render();
		}
		Person person = Person.findById(id);
		if(person == null){
			person = new Person();
		}
		List<Permission> permissionList = person.permissions;
		if(permissionList == null){
			permissionList = new ArrayList<Permission>();
		}
		render(person, permissionList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void edit(Long adminId){
		/**
		 * TODO: completare il metodo
		 */
	}
	
}
