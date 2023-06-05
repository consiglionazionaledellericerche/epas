/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package jobs;

import common.injection.StaticInject;
import java.util.List;
import javax.inject.Inject;
import manager.OfficeManager;
import models.Person;
import models.Role;
import models.UsersRolesOffices;

/**
 * Classe che fixa eventuali problemi sui permessi degli impiegati.
 *
 * @author dario
 *
 */
@StaticInject
public class FixEmployeesPermission {

  @Inject
  static OfficeManager officeManager;

  /**
   * Esecuzione del job.
   */
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

      for (UsersRolesOffices uro : p.getUser().getUsersRolesOffices()) {
        //Rimuovo ruolo role se non appartiene più all'office
        if (uro.getRole().getName().equals(employeeRole.getName())) {
          if (uro.getOffice().getCodeId().equals(p.getOffice().getCodeId())) {
            exist = true;
          } else {
            uro.delete();
          }
        }
      }

      if (!exist) {
        officeManager.setUro(p.getUser(), p.getOffice(), employeeRole);
      }
    }
  }

}
