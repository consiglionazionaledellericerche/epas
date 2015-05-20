package jobs;

import java.util.List;

import javax.inject.Inject;

import manager.OfficeManager;
import models.Office;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart(async=true)
public class FixUserPermission extends Job{

	@Inject
	private static OfficeManager officeManager;

	public void doJob(){

		//		Sistema i permessi per lo user admin e developer
		List<Office> offices = Office.findAll();
		for(Office o : offices){
			officeManager.setSystemUserPermission(o);
		}

		//		Sistema i permessi per tutte le persone
		List<Person> persons = Person.findAll();
		Role employeeRole = Role.find("byName",  Role.EMPLOYEE).first();
		for(Person p : persons){

			boolean exist = false;
			//Cerco se esiste già e controllo che sia relativo all'office di appartentenza

			for(UsersRolesOffices uro : p.user.usersRolesOffices ) {
				//Rimuovo ruolo role se non appartiene più all'office
				if(uro.role.name.equals(employeeRole.name)){
					if(uro.office.code.equals(p.office.code)) {
						exist = true;
					}
					else {
						uro.delete();
					}
				}
			}

			if(!exist) {
				officeManager.setUro(p.user, p.office, employeeRole);
			}
		}
	}


}
