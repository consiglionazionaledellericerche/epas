package controllers;

import dao.OfficeDao;
import dao.PersonDao;
import dao.UserDao;

import helpers.Web;

import models.Institute;
import models.Office;
import models.UsersRolesOffices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import javax.inject.Inject;

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
    uro.office = office;

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

      render("@insertNewAdministrator", uro);
    } else {

      rules.checkIfPermitted(uro.office);

      uro.save();
      flash.success(Web.msgSaved(Institute.class));
      Offices.edit(uro.office.id);
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

    rules.checkIfPermitted(uro.office);

    uro.delete();
    flash.success(Web.msgDeleted(UsersRolesOffices.class));
    Offices.edit(uro.office.id);

  }

}
