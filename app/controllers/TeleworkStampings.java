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
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import common.security.SecurityRules;
import dao.PersonDao;
import dao.PersonDao.PersonLite;
import dao.PersonDayDao;
import dao.TeleworkValidationDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import helpers.validators.StringIsTime;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.PersonDayManager;
import manager.StampingManager;
import manager.TeleworkStampingManager;
import manager.configurations.EpasParam;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import manager.services.telework.errors.Errors;
import manager.telework.service.TeleworkComunication;
import models.Office;
import models.Person;
import models.PersonDay;
import models.TeleworkValidation;
import models.dto.NewTeleworkDto;
import models.dto.TeleworkDto;
import models.dto.TeleworkPersonDayDto;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.data.validation.CheckWith;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

/**
 * Controller per la gestione delle timbrature in telelavoro.
 * @author dario
 *
 */
@Slf4j
@With({Resecure.class})
public class TeleworkStampings extends Controller {

  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  static TeleworkStampingManager manager;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static PersonDayManager personDayManager;
  @Inject
  static PersonDao personDao;
  @Inject
  static SecurityRules rules;
  @Inject
  static StampingManager stampingManager;
  @Inject
  static TeleworkComunication comunication;
  @Inject
  static TeleworkValidationDao validationDao;

  /**
   * Renderizza il template per l'inserimento e la visualizzazione delle timbrature
   * per telelavoro nell'anno/mese passati come parametro.
   *
   * @param year l'anno
   * @param month il mese
   * @throws ExecutionException eccezione in esecuzione
   * @throws NoSuchFieldException eccezione di mancanza di parametro
   */
  public static void teleworkStampings(final Integer year, final Integer month) 
      throws NoSuchFieldException, ExecutionException {
    if (year == null || month == null) {
      Stampings.stampings(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
    }
    val currentPerson = Security.getUser().get().person;
    //Accesso da utente di sistema senza persona associata
    if (currentPerson == null) {
      Application.index();
    }
    List<TeleworkPersonDayDto> list = Lists.newArrayList();
    IWrapperPerson wrperson = wrapperFactory.create(currentPerson);

    if (!wrperson.isActiveInMonth(new YearMonth(year, month))) {
      flash.error("Non esiste situazione mensile per il mese di %s %s",
          DateUtility.fromIntToStringMonth(month), year);

      YearMonth last = wrperson.getLastActiveMonth();
      Stampings.stampings(last.getYear(), last.getMonthOfYear());
    }
    PersonStampingRecap psDto = stampingsRecapFactory
        .create(wrperson.getValue(), year, month, true, Optional.absent());
    
    log.debug("Chiedo la lista delle timbrature in telelavoro ad applicazione esterna.");
    list = manager.getMonthlyStampings(psDto);
    if (list.isEmpty()) {
      flash.error("Errore di comunicazione con l'applicazione telework-stamping. "
          + "L'applicazione potrebbe essere spenta o non raggiungibile."
          + "Riprovare più tardi");
    }
    boolean validated = false;
    //Recupero la lista dei mesi di telelavoro approvati
    List<TeleworkValidation> validationList = 
        validationDao.previousValidations(currentPerson, year, month);
    Optional<TeleworkValidation> valid = validationDao
        .byPersonYearAndMonth(currentPerson, year, month);
    if (valid.isPresent()) {
      validated = true;
    }

    render(list, year, month, validationList, validated);
  }

  /**
   * Ritorna la situazione personale delle timbrature in telelavoro.
   *
   * @param personId l'identificativo della persona
   * @param year l'anno 
   * @param month il mese
   * @throws ExecutionException eccezione in esecuzione
   * @throws NoSuchFieldException  eccezione di mancanza di parametro
   */
  public static void personTeleworkStampings(Long personId, Integer year, Integer month) 
      throws NoSuchFieldException, ExecutionException {
    if (year == null || month == null) {
      Stampings.personStamping(personId, LocalDate.now().getYear(), 
          LocalDate.now().getMonthOfYear());
    }
    Person person = personDao.getPersonById(personId);
    PersonLite p = null;
    if (person.personConfigurations.stream().noneMatch(pc -> 
        pc.epasParam.equals(EpasParam.TELEWORK_STAMPINGS) && pc.fieldValue.equals("true"))) {
      List<PersonDao.PersonLite> persons = (List<PersonLite>) renderArgs.get("navPersons");
      if (persons.isEmpty()) {
        flash.error("Non ci sono persone abilitate al telelavoro!!");
        Stampings.personStamping(personId, Integer.parseInt(session.get("yearSelected")), 
            Integer.parseInt(session.get("monthSelected")));
      }
      p = persons.get(0);
      
    }
    if (p != null) {
      person = personDao.getPersonById(p.id); 
    }
    
    Preconditions.checkNotNull(person);

    rules.checkIfPermitted(person.getOffice(new LocalDate(year, month, 1)).get());

    IWrapperPerson wrPerson = wrapperFactory.create(person);

    if (!wrPerson.isActiveInMonth(new YearMonth(year, month))) {

      flash.error("Non esiste situazione mensile per il mese di %s",
          person.fullName(), DateUtility.fromIntToStringMonth(month));

      YearMonth last = wrapperFactory.create(person).getLastActiveMonth();
      personTeleworkStampings(personId, last.getYear(), last.getMonthOfYear());
    }
    
    List<TeleworkPersonDayDto> list = Lists.newArrayList();
    Optional<Office> officeOwner = Security.getUser().get().person != null 
        ? Security.getUser().get().person.getCurrentOffice() : Optional.absent();
    PersonStampingRecap psDto = stampingsRecapFactory
        .create(wrPerson.getValue(), year, month, true, officeOwner);
    
    log.debug("Chiedo la lista delle timbrature in telelavoro ad applicazione esterna.");
    list = manager.getMonthlyStampings(psDto);
    if (list.isEmpty()) {
      flash.error("Errore di comunicazione con l'applicazione telework-stamping. "
          + "L'applicazione potrebbe essere spenta o non raggiungibile."
          + "Riprovare più tardi");
    }
    boolean validated = false;
    //Recupero la lista dei mesi di telelavoro approvati
    List<TeleworkValidation> validationList = 
        validationDao.previousValidations(person, year, month);
    Optional<TeleworkValidation> valid = validationDao
        .byPersonYearAndMonth(person, year, month);
    if (valid.isPresent()) {
      validated = true;
    }
    

    render(year, month, list, person, validated, validationList);
  }

  /**
   * Renderizza la modale per l'inserimento della timbratura in telelavoro.
   *
   * @param personId l'identificativo della persona
   * @param date la data in cui inserire la timbratura
   */
  public static void insertStamping(Long personId, LocalDate date) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    Preconditions.checkState(!date.isAfter(LocalDate.now()));
    rules.checkIfPermitted(person);

    TeleworkDto stamping = TeleworkDto.builder().build();
    render(person, date, stamping);
  }

  /**
   * Cancella la timbratura in telelavoro.
   *
   * @param teleworkStampingId l'identificativo della timbratura in telelavoro
   * @throws ExecutionException eccezione in esecuzione
   * @throws NoSuchFieldException eccezione di mancanza di parametro
   */
  public static void deleteTeleworkStamping(long teleworkStampingId, boolean confirmed) 
      throws NoSuchFieldException, ExecutionException {

    TeleworkDto stamping = null;
    if (!confirmed) {
      log.debug("Recupero la timbratura dall'applicazione esterna da cancellare");
      try {
        stamping = comunication.get(teleworkStampingId);
      } catch (NoSuchFieldException ex) {
        ex.printStackTrace();
      }
      confirmed = true;
      render(stamping, confirmed);
    }      

    log.debug("Comunico con il nuovo sistema per la cancellazione della "
        + "timbratura in telelavoro...");     

    int result = 0;
    try {
      result = comunication.delete(teleworkStampingId);
    } catch (NoSuchFieldException ex) {
      ex.printStackTrace();
    }

    if (result == Http.StatusCode.NO_RESPONSE) {
      flash.success("Orario eliminato correttamente");        
    } else {
      flash.error("Errore nell'eliminazione della timbratura su sistema esterno. Errore %s", 
          result);
    }
    teleworkStampings(Integer.parseInt(session.get("yearSelected")), 
        Integer.parseInt(session.get("monthSelected")));

    flash.success("Timbratura %s - %s eliminata correttamente", 
        stamping.formattedHour(), stamping.getStampType());
    teleworkStampings(Integer.parseInt(session.get("yearSelected")), 
        Integer.parseInt(session.get("monthSelected")));
  }

  /**
   * Persiste la timbratura in telelavoro.
   *
   * @param personId l'identificativo della persona
   * @param date la data 
   * @param stamping la timbratura da salvare
   * @param time l'orario della timbratura
   * @throws ExecutionException eccezione in esecuzione
   * @throws NoSuchFieldException eccezione di mancanza di parametro
   */
  public static void save(Long personId, @Required LocalDate date, 
      @Required @Valid TeleworkDto stamping, @Required @CheckWith(StringIsTime.class) String time) 
          throws NoSuchFieldException, ExecutionException {
    Preconditions.checkState(!date.isAfter(LocalDate.now()));

    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    if (Validation.hasErrors()) {
      response.status = 400;
      render("@insertStamping", stamping, person, date, time);
    }
    PersonDay pd = personDayManager.getOrCreateAndPersistPersonDay(person, date);
    stamping.setDate(stampingManager.deparseStampingDateTimeAsJavaTime(date, time));
    Optional<Errors> check = manager.checkTeleworkStamping(stamping, pd);
    if (check.isPresent()) {
      Validation.addError("stamping.stampType", check.get().advice);
      if (Validation.hasErrors()) {
        response.status = 400;
        render("@insertStamping", stamping, person, date, time);
      }
    }
    log.debug("Comunico con il nuovo sistema per la memorizzazione delle ore in telelavoro...");
    stamping.setPersonDayId(pd.getId());
    int result = manager.save(stamping);
    if (result == Http.StatusCode.CREATED) {
      log.info("Creata la timbratura {} nel sistema esterno", stamping.toString());      
      flash.success("Orario inserito correttamente");        
    } else {
      flash.error("Errore nel salvataggio della timbratura su sistema esterno. Errore %s", 
          result);
    }
    teleworkStampings(date.getYear(), date.getMonthOfYear());    

  }

  /**
   * Modale di modifica della timbratura (Da terminare...).
   *
   * @param teleworkStampingId l'identificativo della timbratura da modificare
   */
  public static void editTeleworkStamping(long teleworkStampingId) {
    TeleworkDto stamping = null;
    log.debug("Comunico con il nuovo sistema per la memorizzazione delle ore in telelavoro...");
    try {
      stamping = manager.get(teleworkStampingId);
    } catch (ExecutionException ex) {
      log.error("Problema durante la ricezione della timbratura per telelavoro {}",
          teleworkStampingId, ex);
    }
    
    render(stamping);
  }
  
  /**
   * Genera il report mensile di telelavoro.
   * 
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @throws NoSuchFieldException eccezione di mancanza di parametro
   * @throws ExecutionException eccezione in esecuzione
   */
  public static void generateReport(int year, int month) 
      throws NoSuchFieldException, ExecutionException {
    
    List<NewTeleworkDto> list = Lists.newArrayList();
    val currentPerson = Security.getUser().get().person;
    IWrapperPerson wrperson = wrapperFactory.create(currentPerson);

    if (!wrperson.isActiveInMonth(new YearMonth(year, month))) {
      flash.error("Non esiste situazione mensile per il mese di %s %s",
          DateUtility.fromIntToStringMonth(month), year);

      YearMonth last = wrperson.getLastActiveMonth();
      Stampings.stampings(last.getYear(), last.getMonthOfYear());
    }
    PersonStampingRecap psDto = stampingsRecapFactory
        .create(wrperson.getValue(), year, month, true, Optional.absent());
    LocalDate date = LocalDate.now();
    log.debug("Chiedo la lista delle timbrature in telelavoro ad applicazione esterna.");
    list = manager.stampingsForReport(psDto);
    render(list, currentPerson, year, month, date);
  }
  
}
