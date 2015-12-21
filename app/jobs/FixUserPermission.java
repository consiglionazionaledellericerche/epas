package jobs;

import com.google.common.collect.Lists;

import injection.StaticInject;

import manager.OfficeManager;

import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.test.Fixtures;

import java.util.List;

import javax.inject.Inject;

@StaticInject
public class FixUserPermission {

  @Inject
  private static OfficeManager officeManager;

  public static void doJob() {

    final class Permesso {

      public Long user;
      public Long office;
      public String role;

      public Permesso(Long user, Long office, String role) {
        this.user = user;
        this.office = office;
        this.role = role;
      }
    }

/*		Procedura un pò esagerata per la riassociazione dei ruoli e permessi corretti
 * 
 *		Crea una copia di tutte le triple degli userRoleOffice per poi
 *		cancellarli tutti dal db in modo da poter cancellare i permessi
 *	 	e i ruoli.
 *		Poi reimporta dal file yaml i permessi e i ruoli corretti e ricrea tutti gli 
 *		userRoleOffice in base alle informazioni precedentemente salvate
 *	
 */

//		int evolution = (Integer)JPA.em().
//				createNativeQuery("SELECT max(id) from play_evolutions").getSingleResult();

    Role developer = Role.find("byName", Role.DEVELOPER).first();

    if (developer == null) {

      List<UsersRolesOffices> uros = UsersRolesOffices.findAll();
      List<Permesso> permessi = Lists.newArrayList();

      for (UsersRolesOffices uro : uros) {
        //Il ruolo superAdmin e' stato rinominato in Admin
        String ruolo = uro.role.name.equals("superAdmin") ? Role.ADMIN : uro.role.name;
        permessi.add(new Permesso(uro.user.id, uro.office.id, ruolo));
      }

      UsersRolesOffices.deleteAll();
      Role.deleteAll();

      JPA.em().clear();

      //Allinea tutte le sequenze del db
      Fixtures.executeSQL(Play.getFile("db/import/fix_sequences.sql"));

      Fixtures.loadModels("../db/import/rolesAndPermission.yml");

      for (Permesso p : permessi) {
        User user = User.findById(p.user);
        Office office = Office.findById(p.office);
        Role role = Role.find("byName", p.role).first();
        officeManager.setUro(user, office, role);
      }

      Logger.info("Ricreati %s permessi", uros.size());
    }

    //		Sistema i permessi per gli user admin e developer
    List<Office> offices = Office.findAll();

    for (Office o : offices) {
      officeManager.setSystemUserPermission(o);
    }

    //		Sistema i permessi per tutte le persone
    List<Person> persons = Person.findAll();
    Role employeeRole = Role.find("byName", Role.EMPLOYEE).first();
    for (Person p : persons) {

      boolean exist = false;
      //Cerco se esiste già e controllo che sia relativo all'office di appartentenza

      for (UsersRolesOffices uro : p.user.usersRolesOffices) {
        //Rimuovo ruolo role se non appartiene più all'office
        if (uro.role.name.equals(employeeRole.name)) {
          if (uro.office.codeId.equals(p.office.codeId)) {
            exist = true;
          } else {
            uro.delete();
          }
        }
      }

      if (!exist) {
        officeManager.setUro(p.user, p.office, employeeRole);
      }
    }
  }

}
