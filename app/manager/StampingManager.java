package manager;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.inject.Inject;
import controllers.Security;
import controllers.Stampings;

import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;

import helpers.Web;

import java.util.ArrayList;
import java.util.List;

import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingDayRecapFactory;
import models.Person;
import models.PersonDay;
import models.Role;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.enumerate.StampTypes;
import models.exports.StampingFromClient;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

@Slf4j
public class StampingManager {

  private final PersonDayDao personDayDao;
  private final PersonDao personDao;
  private final PersonDayManager personDayManager;
  private final PersonStampingDayRecapFactory stampingDayRecapFactory;
  private final ConsistencyManager consistencyManager;
  private final StampingDao stampingDao;
  private final NotificationManager notificationManager;

  /**
   * @param personDayDao il dao per cercare i personday
   * @param personDao il dao per cercare le persone
   * @param personDayManager il manager per lavorare sui personday
   * @param stampingDayRecapFactory il factory per lavorare sugli stampingDayRecap
   * @param consistencyManager il costruttore dell'injector.
   */
  @Inject
  public StampingManager(PersonDayDao personDayDao,
      PersonDao personDao,
      PersonDayManager personDayManager,
      PersonStampingDayRecapFactory stampingDayRecapFactory,
      ConsistencyManager consistencyManager, StampingDao stampingDao,
      NotificationManager notificationManager) {

    this.personDayDao = personDayDao;
    this.personDao = personDao;
    this.personDayManager = personDayManager;
    this.stampingDayRecapFactory = stampingDayRecapFactory;
    this.consistencyManager = consistencyManager;
    this.stampingDao = stampingDao;
    this.notificationManager = notificationManager;
  }


  /**
   * @param pd il personday
   * @param stamping la timbratura
   * @return true se esiste una timbratura nel personday uguale a quella passata.
   */
  private static boolean checkDuplicateStamping(
      PersonDay pd, StampingFromClient stamping) {

    for (Stamping stamp : pd.stampings) {

      if (stamp.date.isEqual(stamping.dateTime)) {
        return true;
      }
      if (stamp.date.isEqual(stamping.dateTime.minusMinutes(1))
          && (stamp.isIn() && stamping.inOut == 0
          || stamp.isOut() && stamping.inOut == 1)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Crea il tempo. Il campo time è già stato validato HH:MM o HHMM
   */
  public LocalDateTime deparseStampingDateTime(LocalDate date, String time) {

    time = time.replaceAll(":", "");
    Integer hour = Integer.parseInt(time.substring(0, 2));
    Integer minute = Integer.parseInt(time.substring(2, 4));
    return new LocalDateTime(date.getYear(), date.getMonthOfYear(),
        date.getDayOfMonth(), hour, minute);

  }

  /**
   * Calcola il numero massimo di coppie ingresso/uscita in un giorno specifico per tutte le persone
   * presenti nella lista di persone attive a quella data.
   *
   * @param date data
   * @param activePersonsInDay lista delle persone da verificare
   * @return numero di coppie
   */
  public final int maxNumberOfStampingsInMonth(final LocalDate date,
      final List<Person> activePersonsInDay) {

    int max = 0;

    for (Person person : activePersonsInDay) {
      Optional<PersonDay> pd = personDayDao.getPersonDay(person, date);

      if (pd.isPresent()) {
        PersonDay personDay = pd.get();
        if (max < personDayManager.numberOfInOutInPersonDay(personDay)) {
          max = personDayManager.numberOfInOutInPersonDay(personDay);
        }
      }
    }

    if (max < 2) {
      max = 2;
    }
    return max;
  }

  /**
   * Metodo che salva la timbratura.
   * @param stamping la timbratura da persistere
   * @param date la data della timbratura
   * @param time l'orario della timbratura
   * @param person la persona a cui associare la timbratura
   * @return la stringa contenente un messaggio di errore se il salvataggio non è andato a
   *     buon fine, stringa vuota altrimenti.
   */
  public String persistStamping(Stamping stamping, LocalDate date, String time, 
      Person person, User currentUser, boolean newInsert) {
    String result = "";
    
    val alreadyPresentStamping = stampingDao.getStamping(stamping.date, person, stamping.way);
    //Se la timbratura allo stesso orario e con lo stesso verso non è già presente o è una modifica
    //alla timbratura esistente allora creo/modifico la timbratura.
    if (!alreadyPresentStamping.isPresent() 
        || alreadyPresentStamping.get().id.equals(stamping.id)) {

      if (!currentUser.isSystemUser()) {
        if (currentUser.hasRoles(Role.PERSONNEL_ADMIN)) {
          stamping.markedByEmployee = false;
          stamping.markedByAdmin = true;
        } else {
          stamping.markedByEmployee = true;
          stamping.markedByAdmin = false;
        }
      }
      stamping.save();

      consistencyManager
      .updatePersonSituation(stamping.personDay.person.id, stamping.personDay.date);
      
      notificationManager
      .notificationStampingPolicy(currentUser, stamping, newInsert, !newInsert, false);
    } else {
      if ((stamping.stampType != null 
          && !stamping.stampType.equals(alreadyPresentStamping.get().stampType)) 
          || (stamping.stampType == null && alreadyPresentStamping.get().stampType != null)) {
        result = "Timbratura già presente ma con causale diversa, "
            + "modificare la timbratura presente.";  
      } else {
        result = "Timbratura ignorata perché già presente.";
      }
    }
    
    return result;
  }
  
  
  /**
   * Stamping dal formato del client al formato ePAS.
   */
  public boolean createStampingFromClient(StampingFromClient stampingFromClient,
      boolean recompute) {

    // Check della richiesta
    Verify.verifyNotNull(stampingFromClient);

    final Person person = stampingFromClient.person;
    // Recuperare il personDay
    PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(
        stampingFromClient.person, stampingFromClient.dateTime.toLocalDate());

    // Check stamping duplicata
    if (checkDuplicateStamping(personDay, stampingFromClient)) {
      log.info("Timbratura delle {} già presente per {} (matricola = {}) ",
          stampingFromClient.dateTime, person, person.number);
      return false;
    }

    //Creazione stamping e inserimento
    Stamping stamping = new Stamping(personDay, stampingFromClient.dateTime);
    stamping.date = stampingFromClient.dateTime;
    stamping.markedByAdmin = stampingFromClient.markedByAdmin;
    stamping.way = stampingFromClient.inOut == 0 ? WayType.in : WayType.out;
    stamping.stampType = stampingFromClient.stampType;
    stamping.stampingZone = 
        (stampingFromClient.zona != null && !stampingFromClient.zona.equals("")) 
        ? stampingFromClient.zona : null;
    stamping.note = stampingFromClient.note;
    
    stamping.save();
    
    log.info("Inserita timbratura {} per {} (matricola = {}) ",
        stamping.getLabel(), person, person.number);

    // Ricalcolo
    if (recompute) {
      consistencyManager.updatePersonSituation(person.id, personDay.date);
    }

    return true;
  }

  /**
   * La lista dei PersonStampingDayRecap renderizzata da presenza giornaliera.
   */
  public List<PersonStampingDayRecap> populatePersonStampingDayRecapList(
      List<Person> activePersonsInDay,
      LocalDate dayPresence, int numberOfInOut) {

    List<PersonStampingDayRecap> daysRecap = new ArrayList<>();
    for (Person person : activePersonsInDay) {

      person = personDao.getPersonById(person.id);
      Optional<PersonDay> pd = personDayDao.getPersonDay(person, dayPresence);

      if (pd.isPresent()) {
        personDayManager.setValidPairStampings(pd.get().stampings);
        daysRecap.add(stampingDayRecapFactory
            .create(pd.get(), numberOfInOut, true, Optional.absent()));
      }
    }
    return daysRecap;
  }

  /**
   * @param stamping la timbratura da controllare
   * @param user l'utente che vuole inserire la timbratura
   * @param employee la persona per cui si vuole inserire la timbratura
   * @return true se lo stampType relativo alla timbratura da inserire è tra quelli previsti per la
   *     timbratura fuori sede, false altrimenti.
   */
  public boolean checkStampType(Stamping stamping, User user, Person employee) {
    return user.person.id.equals(employee.id)
        && stamping.stampType == StampTypes.LAVORO_FUORI_SEDE;
  }

  /**
   * Associa la persona alla timbratura ricevuta via REST.
   *
   * @param stamping DTO costruito dal Json
   * @return un Optional contenente la person o absent
   */
  public Optional<Person> linkToPerson(StampingFromClient stamping) {

    Optional<User> user = Security.getUser();
    if (!user.isPresent()) {
      log.error("Impossibile recuperare l'utente che ha inviato la timbratura: {}", stamping);
      return Optional.absent();
    }
    if (user.get().badgeReader == null) {
      log.error("L'utente {} utilizzato per l'invio della timbratura"
          + " non ha una istanza badgeReader valida associata.", user.get().username);
      return Optional.absent();
    }
    final Optional<Person> person = Optional.fromNullable(personDao
        .getPersonByBadgeNumber(stamping.numeroBadge, user.get().badgeReader));

    if (person.isPresent()) {
      stamping.person = person.get();      
    } else {
      log.warn("Non e' stato possibile recuperare la persona a cui si riferisce la timbratura,"
          + " matricolaFirma={}. Controllare il database.", stamping.numeroBadge);
    }
   
    return person;
  }

}
