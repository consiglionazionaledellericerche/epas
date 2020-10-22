package controllers;

import com.google.common.base.Optional;
import com.querydsl.core.QueryResults;
import dao.OfficeDao;
import dao.RoleDao;
import helpers.Web;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Institute;
import models.Role;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

@Slf4j
@With({Resecure.class})
public class Institutes extends Controller {

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static RoleDao roleDao;

  public static void index() {
    flash.keep();
    list(null, null, null);
  }

  /**
   * Lista degli istituti.
   *
   * @param instituteName filtro nome istituto
   * @param officeName filtro sede
   */
  public static void list(String instituteName, String officeName, String codes) {

    QueryResults<?> results = officeDao.institutes(Optional.<String>fromNullable(instituteName),
        Optional.<String>fromNullable(officeName), Optional.<String>fromNullable(codes),
        Security.getUser().get(), roleDao.getRoleByName(Role.TECHNICAL_ADMIN))
        .listResults();

    render(results);
  }

  /**
   * Ritorna la form di editing dell'istituto con id passato come parametro.
   * @param id l'identificativo dell'istituto da editare
   */
  public static void edit(Long id) {

    final Institute institute = Institute.findById(id);
    notFoundIfNull(institute);
    render(institute);
  }

  public static void blank() {
    final Institute institute = new Institute();
    render("@edit", institute);
  }

  /**
   * Permette il salvataggio dell'istituto in oggetto al metodo.
   * @param institute l'oggetto da persistere
   */
  public static void save(@Valid Institute institute) {

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", institute,
          validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@edit", institute);
    } else {
      institute.save();
      flash.success(Web.msgSaved(Institute.class));
      index();
    }
  }

  /**
   * Cancella l'oggetto institute con id passato come parametro.
   * @param id l'identificativo dell'istituto da cancellare
   */
  public static void delete(Long id) {
    final Institute institute = Institute.findById(id);
    notFoundIfNull(institute);

    if (institute.seats.isEmpty()) {
      institute.delete();
      flash.success(Web.msgDeleted(Institute.class));
      index();
    }
    flash.error(Web.msgHasErrors());
    index();
  }

}
