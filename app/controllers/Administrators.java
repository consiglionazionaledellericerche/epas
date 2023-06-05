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

package controllers;

import common.security.SecurityRules;
import dao.OfficeDao;
import dao.PersonDao;
import dao.UserDao;
import helpers.Web;
import javax.inject.Inject;
import models.Institute;
import models.Office;
import models.UsersRolesOffices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione degli utenti amministratori.
 */
@With({Resecure.class})
public class Administrators extends Controller {

  private static final Logger log = LoggerFactory.getLogger(Institutes.class);

  @Inject
  static SecurityRules rules;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static PersonDao personDao;
  @Inject
  static UserDao userDao;

  /**
   * metodo che ritorna la form di inserimento amministratore per la sede passata per parametro.
   *
   * @param officeId l'id della sede a cui associare amministratore e ruolo.
   */
  public static void blank(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    // deve avere technicalAdmin sull'office, oppure super admin
    rules.checkIfPermitted(office);

    UsersRolesOffices uro = new UsersRolesOffices();
    uro.setOffice(office);

    render(uro);
  }

  /**
   * metodo che salva il ruolo per l'user_role_office.
   *
   * @param uro l'user_role_office da salvare
   */
  public static void save(@Valid UsersRolesOffices uro) {

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", uro,
          validation.errorsMap());
      flash.error(Web.msgHasErrors());
      Validation.addError("uro.role", "Esiste già quel ruolo assegnato alla persona");
      render("@blank", uro);
    } else {

      rules.checkIfPermitted(uro.getOffice());

      uro.save();
      flash.success(Web.msgSaved(Institute.class));
      Offices.edit(uro.getOffice().id);
    }
  }


  /**
   * metodo che cancella l'uro specificato.
   *
   * @param uroId l'id dell'user_role_office
   */
  public static void delete(Long uroId) {

    final UsersRolesOffices uro = UsersRolesOffices.findById(uroId);
    notFoundIfNull(uro);

    rules.checkIfPermitted(uro.getOffice());

    uro.delete();
    flash.success(Web.msgDeleted(UsersRolesOffices.class));
    Offices.edit(uro.getOffice().id);

  }

}
