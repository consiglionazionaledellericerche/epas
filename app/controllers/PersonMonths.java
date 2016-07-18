package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;

import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonMonthRecapDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperContractMonthRecap;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import manager.PersonMonthsManager;

import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.PersonMonthRecap;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.data.validation.Required;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;

@With({Resecure.class})
public class PersonMonths extends Controller {

  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static PersonMonthRecapDao personMonthRecapDao;
  @Inject
  private static PersonMonthsManager personMonthsManager;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  static WrapperModelFunctionFactory wrapperFunctionFactory;

  /**
   * metodo che renderizza la visualizzazione del riepilogo orario.
   *
   * @param year l'anno
   */
  public static void hourRecap(int year) {

    Optional<User> user = Security.getUser();
    if (!user.isPresent() || user.get().person == null) {
      flash.error("Accesso negato.");
      renderTemplate("Application/indexAdmin.html");
    }

    if (year > new LocalDate().getYear()) {
      flash.error("Impossibile richiedere riepilogo anno futuro.");
      renderTemplate("Application/indexAdmin.html");
    }

    Person person = user.get().person;

    Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();

    Preconditions.checkState(contract.isPresent());

    List<IWrapperContractMonthRecap> recaps = Lists.newArrayList();
    YearMonth actual = new YearMonth(year, 1);
    YearMonth last = new YearMonth(year, 12);
    IWrapperContract con = wrapperFactory.create(contract.get());
    while (!actual.isAfter(last)) {
      Optional<ContractMonthRecap> recap = con.getContractMonthRecap(actual);
      if (recap.isPresent()) {
        recaps.add(wrapperFactory.create(recap.get()));
      }
      actual = actual.plusMonths(1);
    }

    render(recaps, year);
  }

  /**
   * Le ore di formazione del dipendente nell'anno.
   *
   * @param year year
   */
  public static void trainingHours(int year) {

    Person person = Security.getUser().get().person;

    List<PersonMonthRecap> personMonthRecapList = personMonthRecapDao
        .getPersonMonthRecapInYearOrWithMoreDetails(person, year,
            Optional.<Integer>absent(), Optional.<Boolean>absent());

    LocalDate today = new LocalDate();

    render(person, year, personMonthRecapList, today);

  }

  /**
   * CRUD Inserimento le ore di formazione.
   *
   * @param month month
   * @param year  year
   */
  public static void insertTrainingHours(Integer month, Integer year) {

    Person person = Security.getUser().get().person;

    render(person, month, year);
  }

  /**
   * Salva l'ora di formazione.
   *
   * @param begin inizio
   * @param end   fine
   * @param value quantità
   * @param month mese
   * @param year  anno
   */
  public static void saveTrainingHours(@Valid @Min(0) Integer begin, @Min(0) @Valid Integer end,
      @Required @Valid @Min(0) Integer value, Integer month, Integer year, 
      Long personMonthSituationId) {

    Person person = Security.getUser().get().person;

    if (personMonthSituationId != null) {
      PersonMonthRecap pm = personMonthRecapDao.getPersonMonthRecapById(personMonthSituationId);

      Verify.verify(pm.isEditable());
      checkErrorsInUpdate(value, pm);
      
      if (validation.hasErrors()) {
        response.status = 400;
        render("@insertTrainingHours",
            person, month, year, begin, end, value, personMonthSituationId);
      }
      pm.trainingHours = value;
      pm.save();
      flash.success("Ore di formazione aggiornate.", value);

      PersonMonths.trainingHours(year);

    }
    checkErrors(begin, end, year, month, value);
    if (validation.hasErrors()) {
      response.status = 400;
      render("@insertTrainingHours", person, month, year, begin, end, value);
    }
    
    if (!validation.hasErrors()) {
      if (!personMonthsManager.checkIfAlreadySend(person, year, month).getResult()) {
        flash.error("Le ore di formazione per il mese selezionato sono già state approvate.");
        trainingHours(year);
      }
    }

    personMonthsManager.saveTrainingHours(person, year, month, begin, end, false, value);
    flash.success("Salvate %d ore di formazione ", value);

    PersonMonths.trainingHours(year);
  }

  /**
   * Modifica delle ore di formazione.
   *
   * @param personMonthSituationId id
   */
  public static void modifyTrainingHours(Long personMonthSituationId) {

    PersonMonthRecap pm = personMonthRecapDao.getPersonMonthRecapById(personMonthSituationId);

    int year = pm.year;
    int month = pm.month;
    Person person = pm.person;

    int begin = pm.fromDate.getDayOfMonth();
    int end = pm.toDate.getDayOfMonth();
    int value = pm.trainingHours;

    LocalDate dateFrom = new LocalDate(year, month, begin);
    LocalDate dateTo = new LocalDate(year, month, end);

    render("@insertTrainingHours",
        dateFrom, dateTo, person, month, year, personMonthSituationId, begin, end, value);
  }


  /**
   * metodo che renderizza la form di conferma cancellazione di ore di formazione.
   *
   * @param personId           l'id della persona
   * @param personMonthRecapId l'id del personMonthRecap
   */
  public static void deleteTrainingHours(Long personId, Long personMonthRecapId) {
    PersonMonthRecap pm = personMonthRecapDao.getPersonMonthRecapById(personMonthRecapId);
    if (pm == null) {
      flash.error("Ore di formazioni inesistenti. Operazione annullata.");
      PersonMonths.trainingHours(LocalDate.now().getYear());

    }
    render(pm);
  }


  /**
   * metodo che cancella dal db le ore di formazione specificate.
   *
   * @param personMonthRecapId l'id delle ore di formazione da cancellare
   */
  public static void deleteTrainingHoursConfirmed(Long personMonthRecapId) {

    PersonMonthRecap pm = personMonthRecapDao.getPersonMonthRecapById(personMonthRecapId);
    if (pm == null) {
      flash.error("Ore di formazioni inesistenti. Operazione annullata.");
      Stampings.stampings(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
    }

    Person person = Security.getUser().get().person;
    if (person == null || !person.id.equals(pm.person.id)) {
      flash.error("Accesso negato.");
      renderTemplate("Application/indexAdmin.html");
    }

    pm.delete();
    flash.error("Ore di formazione eliminate con successo.");
    PersonMonths.trainingHours(pm.year);
  }

  /**
   * visualizza il tabellone riepilogativo delle ore di formazione in un determinato mese e anno 
   * per l'ufficio con id officeId.
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @param officeId l'id dell'ufficio di riferimento
   */
  public static void visualizePeopleTrainingHours(int year, int month, Long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    Set<Office> offices = Sets.newHashSet();
    offices.add(office);
    List<Person> personList = personDao.getActivePersonInMonth(offices, new YearMonth(year, month));

    Map<Person, List<PersonMonthRecap>> map = personMonthsManager
        .createMap(personList, year, month);

    render(map, year, month, office);
  }
  
  /**
   * visualizza la form di inserimento per l'amministratore delle ore di formazione.
   * @param officeId l'id dell'ufficio di cui si vuole inserire le ore di formazione
   * @param month il mese di riferimento
   * @param year l'anno di riferimento
   */
  public static void insertPeopleTrainingHours(Long officeId, int month, int year) {
    Office office;
    if (officeId == null) {
      office = officeDao.getOfficeById(Long.parseLong(session.get("officeSelected")));
    } else {
      office = officeDao.getOfficeById(officeId);
    }
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    List<Person> simplePersonList = personDao.listFetched(Optional.<String>absent(),
        ImmutableSet.of(office), false, null, null, false).list();

    render(month, year, simplePersonList);
  }
  
  /**
   * salvataggio delle ore di formazione inserite dall'amministratore.
   * @param begin il giorno di inizio della formazione
   * @param end il giorno di fine della formazione
   * @param value la quantità di ore di formazione
   * @param month il mese in cui si è fatta formazione
   * @param year l'anno in cui si è fatta la formazione
   * @param person la persona che ha fatto la formazione
   */
  public static void save(@Valid @Min(0) Integer begin, @Min(0) @Valid Integer end,
      @Required @Valid @Min(0) Integer value, Integer month, Integer year, Person person) {
    checkErrors(begin, end, year, month, value);
    if (validation.hasErrors()) {
      response.status = 400;
      render("@insertPeopleTrainingHours",
          person, month, year, begin, end, value);
    }
    //Verify.verify(pm.isEditable());
    personMonthsManager.saveTrainingHours(person, year, month, begin, end, false, value);
    flash.success("Salvate %d ore di formazione per %s", value, person.fullName());
    PersonMonths.visualizePeopleTrainingHours(year, month, person.office.id);
  }
  
  /**
   * metodo privato che aggiunge al validation eventuali errori riscontrati nel passaggio
   * dei parametri.
   * @param begin il giorno di inizio della formazione
   * @param end il giorno di fine della formazione
   * @param year l'anno di formazione
   * @param month il mese di formazione
   * @param value la quantità di ore di formazione
   */
  private static void checkErrors(Integer begin, Integer end, Integer year, 
      Integer month, Integer value) {
    if (!validation.hasErrors()) {
      if (begin == null) {
        validation.addError("begin", "Richiesto");
      }
      if (end == null) {
        validation.addError("end", "Richiesto");
      }    
      int endMonth = new LocalDate(year, month, 1).dayOfMonth().withMaximumValue().getDayOfMonth();
      if (begin > endMonth) {
        validation.addError("begin",
            "deve appartenere al mese selezionato");
      }
      if (end > endMonth) {
        validation.addError("end",
            "deve appartenere al mese selezionato");
      }
      if (begin > end) {
        validation.addError("begin",
            "inizio intervallo  non valido");
      }
    
      if (value > 24 * (end - begin + 1) && end - begin >= 0) {
        validation.addError("value",
            "valore troppo alto");
      }
    }
  }
  
  /**
   * aggiunge al validation l'eventuale errore relativo al quantitativo orario che può superare
   * le ore possibili prendibili per quel giorno.
   * @param value il quantitativo di ore di formazione
   * @param pm il personMonthRecap da modificare con le ore passate come parametro
   */
  private static void checkErrorsInUpdate(Integer value, PersonMonthRecap pm) {
    if (!validation.hasErrors()) {
      if (value > 24 * (pm.toDate.getDayOfMonth() - pm.fromDate.getDayOfMonth() + 1)) {
        validation.addError("value",
            "valore troppo alto");
      }
    }
  }

}
