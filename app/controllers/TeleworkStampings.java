package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.TeleworkStampingDao;
import dao.history.HistoryValue;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import helpers.validators.StringIsTime;
import it.cnr.iit.epas.DateUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.PersonDayManager;
import manager.StampingManager;
import manager.TeleworkStampingManager;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import manager.services.telework.errors.Errors;
import manager.telework.service.TeleworkComunication;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.TeleworkStamping;
import models.dto.ReperibilityEvent;
import models.dto.TeleworkDto;
import models.dto.TeleworkPersonDayDto;
import models.enumerate.StampTypes;
import models.enumerate.TeleworkStampTypes;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.Play;
import play.data.validation.CheckWith;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With({Resecure.class})
public class TeleworkStampings extends Controller {

  static final String TELEWORK_CONF = "telework.stampings.active";

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
  static TeleworkStampingDao teleworkStampingDao;
  @Inject
  static TeleworkComunication comunication;

  /**
   * Renderizza il template per l'inserimento e la visualizzazione delle timbrature
   * per telelavoro nell'anno/mese passati come parametro.
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
    List<TeleworkStampTypes> beginEnd = TeleworkStampTypes.beginEndTelework();
    List<TeleworkStampTypes> meals = TeleworkStampTypes.beginEndMealInTelework();
    List<TeleworkStampTypes> interruptions = TeleworkStampTypes.beginEndInterruptionInTelework();
    IWrapperPerson wrperson = wrapperFactory.create(currentPerson);

    if (!wrperson.isActiveInMonth(new YearMonth(year, month))) {
      flash.error("Non esiste situazione mensile per il mese di %s %s",
          DateUtility.fromIntToStringMonth(month), year);

      YearMonth last = wrperson.getLastActiveMonth();
      Stampings.stampings(last.getYear(), last.getMonthOfYear());
    }
    PersonStampingRecap psDto = stampingsRecapFactory
        .create(wrperson.getValue(), year, month, true);
    if ("true".equals(Play.configuration.getProperty(TELEWORK_CONF))) {
      log.info("Chiedo la lista delle timbrature in telelavoro ad applicazione esterna.");
      list = manager.getMonthlyStampings(psDto);
      if (list.isEmpty()) {
        flash.error("Errore di comunicazione con l'applicazione telework-stamping. "
            + "L'applicazione potrebbe essere spenta o non raggiungibile."
            + "Riprovare più tardi");
      }
    } else {
      for (PersonStampingDayRecap day : psDto.daysRecap) {      

        TeleworkPersonDayDto dto = TeleworkPersonDayDto.builder()
            .personDay(day.personDay)
            .beginEnd(manager.getSpecificTeleworkStampings(day.personDay, beginEnd))
            .meal(manager.getSpecificTeleworkStampings(day.personDay, meals))
            .interruptions(manager.getSpecificTeleworkStampings(day.personDay, interruptions))
            .build();
        list.add(dto);
      }

    }

    render(list, year, month);
  }

  /**
   * Ritorna la situazione personale delle timbrature in telelavoro.
   * @param personId l'identificativo della persona
   * @param year l'anno 
   * @param month il mese
   */
  public static void personTeleworkStampings(Long personId, Integer year, Integer month) {
    if (year == null || month == null) {
      Stampings.personStamping(personId, LocalDate.now().getYear(), 
          LocalDate.now().getMonthOfYear());
    }
    Person person = personDao.getPersonById(personId);
    Preconditions.checkNotNull(person);

    rules.checkIfPermitted(person.office);

    IWrapperPerson wrPerson = wrapperFactory.create(person);

    if (!wrPerson.isActiveInMonth(new YearMonth(year, month))) {

      flash.error("Non esiste situazione mensile per il mese di %s",
          person.fullName(), DateUtility.fromIntToStringMonth(month));

      YearMonth last = wrapperFactory.create(person).getLastActiveMonth();
      personTeleworkStampings(personId, last.getYear(), last.getMonthOfYear());
    }
    List<TeleworkPersonDayDto> list = Lists.newArrayList();
    List<TeleworkStampTypes> beginEnd = TeleworkStampTypes.beginEndTelework();
    List<TeleworkStampTypes> meals = TeleworkStampTypes.beginEndMealInTelework();
    List<TeleworkStampTypes> interruptions = TeleworkStampTypes.beginEndInterruptionInTelework();
    PersonStampingRecap psDto = stampingsRecapFactory
        .create(wrPerson.getValue(), year, month, true);
    for (PersonStampingDayRecap day : psDto.daysRecap) {      

      TeleworkPersonDayDto dto = TeleworkPersonDayDto.builder()
          .personDay(day.personDay)
          .beginEnd(manager.getSpecificTeleworkStampings(day.personDay, beginEnd))
          .meal(manager.getSpecificTeleworkStampings(day.personDay, meals))
          .interruptions(manager.getSpecificTeleworkStampings(day.personDay, interruptions))
          .build();
      list.add(dto);      
    }
    render(year, month, list, person);
  }

  /**
   * Renderizza la modale per l'inserimento della timbratura in telelavoro.
   * @param personId l'identificativo della persona
   * @param date la data in cui inserire la timbratura
   */
  public static void insertStamping(Long personId, LocalDate date) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    Preconditions.checkState(!date.isAfter(LocalDate.now()));
    rules.checkIfPermitted(person);

    TeleworkStamping stamping = new TeleworkStamping();
    render(person, date, stamping);
  }

  /**
   * Cancella la timbratura in telelavoro.
   * @param teleworkStampingId l'identificativo della timbratura in telelavoro
   * @throws ExecutionException eccezione in esecuzione
   * @throws NoSuchFieldException eccezione di mancanza di parametro
   */
  public static void deleteTeleworkStamping(long teleworkStampingId, boolean confirmed) 
      throws NoSuchFieldException, ExecutionException {
    TeleworkStamping stamping = null;
    if ("true".equals(Play.configuration.getProperty(TELEWORK_CONF))) {
      
      TeleworkDto dto = null;
      if (!confirmed) {
        log.info("Recupero la timbratura dall'applicazione esterna da cancellare");
        try {
          dto = comunication.get(teleworkStampingId);
        } catch (NoSuchFieldException ex) {
          ex.printStackTrace();
        }
        stamping = manager.mapper(dto);
        confirmed = true;
        render(stamping, confirmed);
      }      
      
      log.info("Comunico con il nuovo sistema per la cancellazione della "
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

    } else {
      stamping = teleworkStampingDao.getStampingById(teleworkStampingId);
      notFoundIfNull(stamping);
      if (!confirmed) {
        confirmed = true;
        render(stamping, confirmed);
      }
      stamping.delete();
    }

    flash.success("Timbratura %s - %s eliminata correttamente", 
        stamping.formattedHour(), stamping.stampType.getDescription());
    teleworkStampings(Integer.parseInt(session.get("yearSelected")), 
        Integer.parseInt(session.get("monthSelected")));
  }

  /**
   * Persiste la timbratura in telelavoro.
   * @param personId l'identificativo della persona
   * @param date la data 
   * @param stamping la timbratura da salvare
   * @param time l'orario della timbratura
   * @throws ExecutionException eccezione in esecuzione
   * @throws NoSuchFieldException eccezione di mancanza di parametro
   */
  public static void save(Long personId, @Required LocalDate date, 
      @Required TeleworkStamping stamping, @Required @CheckWith(StringIsTime.class) String time) 
          throws NoSuchFieldException, ExecutionException {
    Preconditions.checkState(!date.isAfter(LocalDate.now()));

    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);

    PersonDay pd = personDayManager.getOrCreateAndPersistPersonDay(person, date);
    stamping.date = stampingManager.deparseStampingDateTime(date, time);
    Optional<Errors> check = manager.checkTeleworkStamping(stamping, pd);
    if (check.isPresent()) {
      Validation.addError("stamping.stampType", check.get().advice);
      if (Validation.hasErrors()) {
        response.status = 400;
        render("@insertStamping", stamping, person, date, time);
      }
    }
    if ("true".equals(Play.configuration.getProperty(TELEWORK_CONF))) {
      log.info("Comunico con il nuovo sistema per la memorizzazione delle ore in telelavoro...");
      log.info("Salvo la timbratura {}", stamping.toString());
      stamping.personDay = pd;
      int result = manager.save(stamping);
      if (result == Http.StatusCode.CREATED) {
        flash.success("Orario inserito correttamente");        
      } else {
        flash.error("Errore nel salvataggio della timbratura su sistema esterno. Errore %s", 
            result);
      }
      teleworkStampings(date.getYear(), date.getMonthOfYear());

    } else {
      boolean newInsert = !stamping.isPersistent();

      // Se si tratta di un update ha già tutti i riferimenti al personday
      if (newInsert) {
        final PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(person, date);
        stamping.personDay = personDay;
        // non è usato il costruttore con la add, quindi aggiungiamo qui a mano:
        personDay.teleworkStampings.add(stamping);
      }
      stamping.save();
      flash.success("Orario inserito correttamente");
      teleworkStampings(date.getYear(), date.getMonthOfYear());
    }    

  }

  /**
   * Modale di modifica della timbratura (Da terminare...)
   * @param teleworkStampingId l'identificativo della timbratura da modificare
   */
  public static void editTeleworkStamping(long teleworkStampingId) {
    TeleworkStamping stamping = null;
    if ("true".equals(Play.configuration.getProperty(TELEWORK_CONF))) {
      log.info("Comunico con il nuovo sistema per la memorizzazione delle ore in telelavoro...");
      try {
        stamping = manager.get(teleworkStampingId);
      } catch (ExecutionException ex) {
        // TODO Auto-generated catch block
        ex.printStackTrace();
      } 


    } else {
      stamping = teleworkStampingDao.getStampingById(teleworkStampingId);
    }

    render(stamping);
  }

  /**
   * Metodo per eventuale calendario.
   * @param date la data a cui mostrare la situazione
   */
  public static void show(LocalDate date) {
    final LocalDate currentDate = Optional.fromNullable(date).or(LocalDate.now());
    //rules.checkIfPermitted(reperibilitySelected);

    render(currentDate);
  }

  public static void events(LocalDate start, LocalDate end) {
    List<ReperibilityEvent> events = new ArrayList<>();
  }
}
