package controllers;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

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
		/**
		 * TODO: cambiare i permessi in relazione al fatto che l'utente loggato sia effettivamente attivo nella data in cui visita
		 * la pagina di lista amministratori
		 */
		List<Person> administratorList = new ArrayList<Person>();
		List<Person> personList = Person.find("Select p from Person p where p.name <> ? order by p.surname", "Admin").fetch();
		for(Person p : personList){
			if(p.permissions.size() > 0){
				administratorList.add(p);
			}

		}

		render(administratorList);
	}

	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void save(@Valid @Required Long personId) {

		if(validation.hasErrors()) {
			if(request.isAjax()) error("Invalid value");
			render("@list");
		}

		List<Permission> permissionList = Permission.findAll();
		Person person = Person.em().getReference(Person.class, personId);
		String viewPersonList = params.get("viewPersonList");
		String insertAndUpdatePerson = params.get("insertAndUpdatePerson");
		String deletePerson = params.get("deletePerson");
		String insertAndUpdateStamping = params.get("insertAndUpdateStamping");
		String insertAndUpdatePassword = params.get("insertAndUpdatePassword");
		String insertAndUpdateWorkingTime = params.get("insertAndUpdateWorkingTime");
		String insertAndUpdateAbsence = params.get("insertAndUpdateAbsence");
		String insertAndUpdateConfiguration = params.get("insertAndUpdateConfiguration");
		String insertAndUpdateAdministrator = params.get("insertAndUpdateAdministrator");
		String insertAndUpdateOffices = params.get("insertAndUpdateOffices");
		String insertAndUpdateCompetences = params.get("insertAndUpdateCompetences");
		String insertAndUpdateVacations = params.get("insertAndUpdateVacations");
		String uploadSituation = params.get("uploadSituation");
		
		/*
		if(person.permissions.size() > 0){
			person.permissions.clear();
			person.save();
		}
		
		if(insertAndUpdateOffices.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("insertAndUpdateOffices"))
					person.permissions.add(p);
			}
		}
		
		if(viewPersonList.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("viewPersonList"))
					person.permissions.add(p);
			}

		}
		if(insertAndUpdatePerson.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("insertAndUpdatePerson"))
					person.permissions.add(p);
			}

		}
		if(deletePerson.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("deletePerson"))
					person.permissions.add(p);
			}

		}	
		if(insertAndUpdateStamping.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("insertAndUpdateStamping"))
					person.permissions.add(p);
			}

		}
		if(insertAndUpdatePassword.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("insertAndUpdatePassword"))
					person.permissions.add(p);
			}

		}
		if(insertAndUpdateWorkingTime.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("insertAndUpdateWorkingTime"))
					person.permissions.add(p);
			}

		}
		if(insertAndUpdateAbsence.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("insertAndUpdateAbsence"))
					person.permissions.add(p);
			}

		}
		if(insertAndUpdateConfiguration.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("insertAndUpdateConfiguration"))
					person.permissions.add(p);
			}

		}
		if(insertAndUpdateAdministrator.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("insertAndUpdateAdministrator"))
					person.permissions.add(p);
			}

		}
		if(insertAndUpdateCompetences.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("insertAndUpdateCompetences"))
					person.permissions.add(p);
			}
		}
		if(insertAndUpdateVacations.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("insertAndUpdateVacations"))
					person.permissions.add(p);
			}
		}
		if(uploadSituation.equals("true")){
			for(Permission p : permissionList){
				if(p.description.equals("uploadSituation"))
					person.permissions.add(p);
			}
		}
		
		person.save();
		*/
		flash.success(String.format("Aggiornati permessi per %s %s con successo", person.name, person.surname));
		Application.indexAdmin();

	}


	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void discard(){
		Administrators.list();
	}

	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void insertAdmin(Long id){
		List<Person> personList = Person.findAll();
		if(id == null) {
			render(personList);
		}

		Person person = Person.findById(id);
		if(person == null){
			person = new Person();
		}
		List<Permission> permissionList = person.permissions;
		if(permissionList == null){
			permissionList = new ArrayList<Permission>();
		}
		render(person, permissionList, personList);
	}

	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void edit(Long adminId){
		if(adminId != null){
			Person person = Person.findById(adminId);
			render(person);
		}
	}
	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void update(){
		long personId = params.get("personId", Long.class);
		Person person = Person.findById(personId);
		String viewPersonList = params.get("viewPersonList");
		String insertAndUpdatePerson = params.get("insertAndUpdatePerson");
		String deletePerson = params.get("deletePerson");
		String insertAndUpdateStamping = params.get("insertAndUpdateStamping");
		String insertAndUpdatePassword = params.get("insertAndUpdatePassword");
		String insertAndUpdateWorkingTime = params.get("insertAndUpdateWorkingTime");
		String insertAndUpdateAbsence = params.get("insertAndUpdateAbsence");
		String insertAndUpdateConfiguration = params.get("insertAndUpdateConfiguration");
		String insertAndUpdateAdministrator = params.get("insertAndUpdateAdministrator");
		String insertAndUpdateOffices = params.get("insertAndUpdateOffices");
		String insertAndUpdateVacations = params.get("insertAndUpdateVacations");
		String insertAndUpdateCompetences = params.get("insertAndUpdateCompetences");
		String uploadSituation = params.get("uploadSituation");
		if(viewPersonList.equals("true") && !person.isViewPersonAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "viewPersonList").first();
			person.permissions.add(p);
		}
		else if(viewPersonList.equals("false") && person.isViewPersonAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "viewPersonList").first();
			person.permissions.remove(p);
		}
		
		if(insertAndUpdatePerson.equals("true") && !person.isInsertAndUpdatePersonAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdatePerson").first();
			person.permissions.add(p);
		}
		else if(insertAndUpdatePerson.equals("false") && person.isInsertAndUpdatePersonAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdatePerson").first();
			person.permissions.remove(p);
		}
		
		if(deletePerson.equals("true") && !person.isDeletePersonAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "deletePerson").first();
			person.permissions.add(p);
		}
		else if(deletePerson.equals("false") && person.isDeletePersonAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "deletePerson").first();
			person.permissions.remove(p);
		}
		
		if(insertAndUpdateStamping.equals("true") && !person.isInsertAndUpdateStampingAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateStamping").first();
			person.permissions.add(p);
		}
		else if(insertAndUpdateStamping.equals("false") && person.isInsertAndUpdateStampingAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateStamping").first();
			person.permissions.remove(p);
		}
		
		if(insertAndUpdatePassword.equals("true") && !person.isInsertAndUpdatePasswordAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdatePassword").first();
			person.permissions.add(p);
		}	
		else if(insertAndUpdatePassword.equals("false") && person.isInsertAndUpdatePasswordAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdatePassword").first();
			person.permissions.remove(p);
		}
		
		if(insertAndUpdateWorkingTime.equals("true") && !person.isInsertAndUpdateWorkinTimeAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateWorkingTime").first();
			person.permissions.add(p);
		}	
		else if(insertAndUpdateWorkingTime.equals("false") && person.isInsertAndUpdateWorkinTimeAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateWorkingTime").first();
			person.permissions.remove(p);
		}
		
		if(insertAndUpdateAbsence.equals("true") && !person.isInsertAndUpdateAbsenceAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateAbsence").first();
			person.permissions.add(p);
		}
		else if(insertAndUpdateAbsence.equals("false") && person.isInsertAndUpdateAbsenceAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateAbsence").first();
			person.permissions.remove(p);
		}
		
		if(insertAndUpdateConfiguration.equals("true") && !person.isInsertAndUpdateConfigurationAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateConfiguration").first();
			person.permissions.add(p);
		}
		else if(insertAndUpdateConfiguration.equals("false") && person.isInsertAndUpdateConfigurationAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateConfiguration").first();
			person.permissions.remove(p);
		}
		
		if(insertAndUpdateAdministrator.equals("true") && !person.isInsertAndUpdateAdministratorAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateAdministrator").first();
			person.permissions.add(p);
		}
		else if(insertAndUpdateAdministrator.equals("false") && person.isInsertAndUpdateAdministratorAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateAdministrator").first();
			person.permissions.remove(p);
		}
		
		if(insertAndUpdateOffices.equals("true") && !person.isInsertAndUpdateOfficesAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateOffices").first();
			person.permissions.add(p);
		}
		else if(insertAndUpdateOffices.equals("false") && person.isInsertAndUpdateOfficesAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateOffices").first();
			person.permissions.remove(p);
		}
		
		if(insertAndUpdateCompetences.equals("true") && !person.isInsertAndUpdateCompetenceAndOvertimeAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateCompetences").first();
			person.permissions.add(p);
		}
		else if(insertAndUpdateCompetences.equals("false") && person.isInsertAndUpdateCompetenceAndOvertimeAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateCompetences").first();
			person.permissions.remove(p);
		}
		
		if(insertAndUpdateVacations.equals("true") && !person.isInsertAndUpdateVacationsAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateVacations").first();
			person.permissions.add(p);
		}
		else if(insertAndUpdateVacations.equals("false") && person.isInsertAndUpdateVacationsAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "insertAndUpdateVacations").first();
			person.permissions.remove(p);
		}
		if(uploadSituation.equals("true") && !person.isUploadSituationAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "uploadSituation").first();
			person.permissions.add(p);
		}
		else if(uploadSituation.equals("false") && person.isUploadSituationAvailable()){
			Permission p = Permission.find("Select p from Permission p where p.description = ? ", "uploadSituation").first();
			person.permissions.remove(p);
		}
		person.save();
		flash.success(String.format("Aggiornati con successo i permessi per %s %s", person.name, person.surname));
		Application.indexAdmin();
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void delete(Long adminId){
		Person person = Person.findById(adminId);
		person.permissions.clear();
		person.save();
		flash.success(String.format("Eliminati i permessi per l'utente %s %s", person.name, person.surname));
		Application.indexAdmin();
	}

}
