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

import com.google.common.base.Optional;
import com.google.gdata.util.common.base.Preconditions;
import com.querydsl.core.QueryResults;
import common.security.SecurityRules;
import dao.OfficeDao;
import dao.RoleDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import helpers.Web;
import javax.inject.Inject;
import manager.PeriodManager;
import manager.configurations.ConfigurationManager;
import models.Institute;
import models.Office;
import models.Role;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione degli uffici.
 */
@With({Resecure.class})
public class Offices extends Controller {

  private static final Logger log = LoggerFactory.getLogger(Offices.class);
  @Inject
  static OfficeDao officeDao;
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static RoleDao roleDao;
  @Inject
  static SecurityRules rules;
  @Inject
  static PeriodManager periodManager;

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

    //la lista di institutes su cui si ha technical admin in almeno un office

    QueryResults<?> results = officeDao.institutes(
        Optional.<String>fromNullable(name), Optional.absent(), Optional.absent(),
        Security.getUser().get(), roleDao.getRoleByName(Role.TECHNICAL_ADMIN))
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

    Office office = new Office();
    office.institute = institute;
    render(office);
  }

  /**
   * metodo che salva le informazioni per un office.
   *
   * @param office la sede da salvare
   */
  public static void save(@Valid Office office) {

    Preconditions.checkNotNull(office.institute);

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
      if (office.beginDate == null) {
        office.beginDate = new LocalDate(LocalDate.now().getYear() - 1, 12, 31);
      }
      office.save();

      // Configurazione iniziale di default ...
      configurationManager.updateConfigurations(office);

      periodManager.updatePropertiesInPeriodOwner(office);
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
