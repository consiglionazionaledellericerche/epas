package controllers;

import com.google.common.base.Optional;

import com.mysema.query.SearchResults;

import dao.OfficeDao;
import dao.RoleDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

import helpers.Web;

import models.ConfGeneral;
import models.ConfYear;
import models.Configuration;
import models.Institute;
import models.Office;
import models.Role;
import models.UsersRolesOffices;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.Valid;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import javax.inject.Inject;

@With({Resecure.class, RequestInit.class})
public class Offices extends Controller {

  private static final Logger log = LoggerFactory.getLogger(Offices.class);
  @Inject
  static OfficeDao officeDao;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static RoleDao roleDao;
  @Inject
  static SecurityRules rules;

  public static void index() {
    flash.keep();
    list(null);
  }

  /**
   * il metodo che gestisce la lista degli istituti.
   *
   * @param name l'eventuale parametro su cui filtrare gli istituti
   */
  public static void list(String name) {

    //la lista di institutes su cui si ha tecnical admin in almeno un office

    SearchResults<?> results = officeDao.institutes(
        Optional.<String>fromNullable(name),
        Security.getUser().get(), roleDao.getRoleByName(Role.TECNICAL_ADMIN))
        .listResults();

    render(results, name);
  }

  /**
   * metodo che gestisce la visualizzazione dei dati di un istituto.
   *
   * @param id dell'istituto da visualizzare
   */
  public static void show(Long id) {
    final Office office = Office.findById(id);
    notFoundIfNull(office);
    render(office);
  }

  /**
   * metodo che gestisce la modifica di un office.
   *
   * @param id dell'istituto da modificare
   */
  public static void edit(Long id) {


    final Office office = Office.findById(id);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    IWrapperOffice wrOffice = wrapperFactory.create(office);

    render(office, wrOffice);
  }

  /**
   * metodo che visualizza le informazioni di un istituto.
   *
   * @param instituteId id dell'istituto da visualizzare
   */
  public static void blank(Long instituteId) {
    final Institute institute = Institute.findById(instituteId);
    notFoundIfNull(institute);

    render(institute);
  }

  /**
   * metodo che salva le informazioni per un office.
   *
   * @param office la sede da salvare
   */
  public static void save(@Valid Office office) {

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", office,
          validation.errorsMap());
      IWrapperOffice wrOffice = wrapperFactory.create(office);
      if (!office.isPersistent()) {
        render("@blank", office, wrOffice);
      } else {
        render("@edit", office, wrOffice);
      }
    } else {
      office.beginDate = new LocalDate(LocalDate.now().getYear() - 1, 12, 31);
      office.save();
      flash.success(Web.msgSaved(Office.class));
      Institutes.index();
    }
  }

  /**
   * metodo che cancella una sede.
   *
   * @param id della sede da cancellare
   */
  public static void delete(Long id) {

    final Office office = Office.findById(id);
    notFoundIfNull(office);

    // TODO: if( nessuna persona nella sede?? ) {}
    office.delete();
    flash.success(Web.msgDeleted(Office.class));
    Institutes.index();
  }

}
