package controllers;

import com.google.common.base.Optional;

import com.mysema.query.SearchResults;

import dao.OfficeDao;
import dao.RoleDao;

import helpers.Web;

import models.Institute;
import models.Role;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import javax.inject.Inject;

@With({Resecure.class, RequestInit.class})
public class Institutes extends Controller {

  private static final Logger log = LoggerFactory.getLogger(Institutes.class);

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static RoleDao roleDao;

  public static void index() {
    flash.keep();
    list(null);
  }

  public static void list(String name) {

    SearchResults<?> results = officeDao.institutes(Optional.<String>fromNullable(name),
            Security.getUser().get(), roleDao.getRoleByName(Role.TECNICAL_ADMIN))
            .listResults();

    render(results, name);
  }

  public static void edit(Long id) {

    final Institute institute = Institute.findById(id);
    notFoundIfNull(institute);
    render(institute);
  }

  public static void blank() {
    final Institute institute = new Institute();
    render("@edit", institute);
  }

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
