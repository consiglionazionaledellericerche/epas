package jobs;

import injection.StaticInject;

import java.util.List;

import javax.inject.Inject;

import manager.OfficeManager;

import models.Person;
import models.Role;
import models.UsersRolesOffices;

@StaticInject
public class FixEmployeesPermission {

  @Inject
  static OfficeManager officeManager;

  public static void doJob() {

    /* Procedura un pò esagerata per la riassociazione dei ruoli e permessi corretti
     *
     * Crea una copia di tutte le triple degli userRoleOffice per poi
     * cancellarli tutti dal db in modo da poter cancellare i permessi
     * e i ruoli.
     * Poi reimporta dal file yaml i permessi e i ruoli corretti e ricrea tutti gli
     * userRoleOffice in base alle informazioni precedentemente salvate
     *
     */

    // Sistema i permessi per tutte le persone
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
