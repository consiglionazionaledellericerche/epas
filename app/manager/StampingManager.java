package manager;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;

import manager.recaps.personStamping.PersonStampingDayRecap;
import manager.recaps.personStamping.PersonStampingDayRecapFactory;

import models.Contract;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.exports.StampingFromClient;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class StampingManager {

  private final Logger log = LoggerFactory.getLogger(StampingManager.class);
  private final PersonDayDao personDayDao;
  private final PersonDao personDao;
  private final PersonDayManager personDayManager;
  private final PersonStampingDayRecapFactory stampingDayRecapFactory;
  private final ConsistencyManager consistencyManager;
  
  /**
   * 
   * @param stampingDao il dao per cercare le stampings
   * @param personDayDao il dao per cercare i personday
   * @param personDao il dao per cercare le persone
   * @param personDayManager il manager per lavorare sui personday
   * @param stampingDayRecapFactory il factory per lavorare sugli stampingDayRecap
   * @param consistencyManager
   * il costruttore dell'injector.
   */
  @Inject  
  public StampingManager(StampingDao stampingDao,
                         PersonDayDao personDayDao,
                         PersonDao personDao,
                         PersonDayManager personDayManager,
                         PersonStampingDayRecapFactory stampingDayRecapFactory,
                         ConsistencyManager consistencyManager) {

    this.personDayDao = personDayDao;
    this.personDao = personDao;
    this.personDayManager = personDayManager;
    this.stampingDayRecapFactory = stampingDayRecapFactory;
    this.consistencyManager = consistencyManager;
  }


  /**
   * @param pd il personday
   * @param stamping la timbratura
   * @return true se esiste una timbratura nel personday uguale a quella passata.
   */
  private static boolean checkDuplicateStamping(
          PersonDay pd, StampingFromClient stamping) {

    for (Stamping s : pd.stampings) {

      if (s.date.isEqual(stamping.dateTime)) {
        return true;
      }
      if (s.date.isEqual(stamping.dateTime.minusMinutes(1))
              && ((s.isIn() && stamping.inOut == 0)
              || (s.isOut() && stamping.inOut == 1))) {
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
   * @param date               data
   * @param activePersonsInDay lista delle persone da verificare
   * @return numero di coppie
   */
  public final int maxNumberOfStampingsInMonth(final LocalDate date,
                                               final List<Person> activePersonsInDay) {

    int max = 0;

    for (Person person : activePersonsInDay) {
      PersonDay personDay = null;
      Optional<PersonDay> pd = personDayDao.getPersonDay(person, date);

      if (pd.isPresent()) {
        personDay = pd.get();
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
   * Stamping dal formato del client al formato ePAS.
   */
  public boolean createStampingFromClient(
          StampingFromClient stampingFromClient, boolean recompute) {

    // Check della richiesta

    if (stampingFromClient == null) {
      return false;
    }

    Person person = personDao.getPersonById(stampingFromClient.personId);
    if (person == null) {
      log.warn("L'id della persona passata tramite json non ha trovato "
              + "corrispondenza nell'anagrafica del personale. "
              + "Controllare id = {}", stampingFromClient.personId);
      return false;
    }

    // Recuperare il personDay
    PersonDay personDay = personDayDao
            .getOrCreateAndPersistPersonDay(person, stampingFromClient.dateTime.toLocalDate());

    // Check stamping duplicata
    if (checkDuplicateStamping(personDay, stampingFromClient)) {
      log.info("All'interno della lista di timbrature di {} nel giorno {} "
                      + "c'è una timbratura uguale a quella passata dallo"
                      + "stampingsFromClient: {}",
              new Object[]{person.getFullname(), personDay.date, stampingFromClient.dateTime});
      return true;
    }

    //Creazione stamping e inserimento
    Stamping stamping = new Stamping(personDay, stampingFromClient.dateTime);
    stamping.date = stampingFromClient.dateTime;
    stamping.markedByAdmin = stampingFromClient.markedByAdmin;
    stamping.way = stampingFromClient.inOut == 0 ? WayType.in : WayType.out;
    stamping.stampType = stampingFromClient.stampType;
    stamping.personDay = personDay;
    stamping.save();

    personDay.stampings.add(stamping);
    personDay.save();

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

    List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();
    for (Person person : activePersonsInDay) {

      PersonDay personDay = null;
      person = personDao.getPersonById(person.id);
      Optional<PersonDay> pd = personDayDao.getPersonDay(person, dayPresence);

      if (!pd.isPresent()) {
//      personDay = new PersonDay(person, dayPresence);
//             personDay.create();
        continue;
      } else {
        personDay = pd.get();
      }

      personDayManager.computeValidStampings(personDay);
      daysRecap.add(stampingDayRecapFactory
              .create(personDay, numberOfInOut, Optional.<List<Contract>>absent()));

    }
    return daysRecap;
  }


}
