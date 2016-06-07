package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import controllers.Resecure.NoCheck;

import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;

import it.cnr.iit.epas.NullStringBinder;

import manager.ConsistencyManager;
import manager.OfficeManager;
import manager.PersonDayManager;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingDayRecapFactory;

import models.Contract;
import models.Notification;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.enumerate.NotificationSubject;
import models.enumerate.StampTypes;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Minutes;

import play.Logger;
import play.data.binding.As;
import play.data.validation.Required;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

@With({RequestInit.class, Resecure.class})
public class Clocks extends Controller {

  @Inject
  static OfficeDao officeDao;
  @Inject
  static OfficeManager officeManager;
  @Inject
  static PersonDao personDao;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static PersonDayManager personDayManager;
  @Inject
  static PersonStampingDayRecapFactory stampingDayRecapFactory;
  @Inject
  static ConsistencyManager consistencyManager;

  @NoCheck
  public static void show() {

    LocalDate data = new LocalDate();

    final List<String> addresses = Lists.newArrayList(Splitter.on(",").trimResults()
        .split(Http.Request.current().remoteAddress));

    Set<Office> offices = officeManager.getOfficesWithAllowedIp(addresses);

    if (offices.isEmpty()) {
      flash.error("Le timbrature web non sono permesse da questo terminale! "
          + "Inserire l'indirizzo ip nella configurazione della propria sede per abilitarlo");
      try {
        Secure.login();
      } catch (Throwable e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    List<Person> personList =
        personDao.list(Optional.<String>absent(), offices, false, data, data, true).list();
    render(data, personList);
  }

  @NoCheck
  public static void clockLogin(Person person, String password) {

    if (person == null) {
      flash.error("Selezionare una persona dall'elenco del personale.");
      show();
    }

    final User user = person.user;

    if (user == null) {
      flash.error("La persona selezionata non dispone di un account valido."
          + " Contattare l'amministratore");
      show();
    }

    final List<String> addresses = Lists.newArrayList(Splitter.on(",").trimResults()
        .split(Http.Request.current().remoteAddress));

    if (!officeManager.getOfficesWithAllowedIp(addresses).contains(person.office)) {

      flash.error("Le timbrature web per la persona indicata non sono abilitate da questo"
          + "terminale! Inserire l'indirizzo ip nella configurazione della propria sede per"
          + " abilitarlo");
      show();

    }

    if (Security.authenticate(user.username, password)) {
      // Mark user as connected
      session.put("username", user.username);
      daySituation();
    } else {
      flash.error("Autenticazione fallita!");
      show();
    }
  }

  public static void daySituation() {
    // Se non e' presente lo user in sessione non posso accedere al metodo per via della resecure,
    // Quindi non dovrebbe mai accadere di avere a questo punto uno user null.
    User user = Security.getUser().orNull();

    final List<String> addresses = Lists.newArrayList(Splitter.on(",").trimResults()
        .split(Http.Request.current().remoteAddress));

    if (!officeManager.getOfficesWithAllowedIp(addresses).contains(user.person.office)) {

      flash.error("Le timbrature web per la persona indicata non sono abilitate da questo"
          + "terminale! Inserire l'indirizzo ip nella configurazione della propria sede per"
          + " abilitarlo");
      show();

    }

    LocalDate today = LocalDate.now();

    PersonDay personDay = personDayDao.getPersonDay(user.person, today).orNull();

    if (personDay == null) {
      Logger.debug("Prima timbratura per %s non c'è il personday quindi va creato.",
          user.person.fullName());
      personDay = new PersonDay(user.person, today);
      personDay.create();
    }

    int numberOfInOut = personDayManager.numberOfInOutInPersonDay(personDay) + 1;

    PersonStampingDayRecap dayRecap = stampingDayRecapFactory
        .create(personDay, numberOfInOut, true, Optional.<List<Contract>>absent());

    render(user, dayRecap, numberOfInOut);

  }

  /**
   * @param wayType verso timbratura.
   */
  public static void webStamping(WayType wayType) {
    final Person currentPerson = Security.getUser().get().person;
    final LocalDate today = LocalDate.now();
    render(wayType, currentPerson, today);
  }

  /**
   * @param way       verso timbratura
   * @param stampType Causale timbratura
   * @param note      eventuali note.
   */
  public static void insertWebStamping(@Required WayType way, StampTypes stampType,
                                       @As(binder = NullStringBinder.class) String note) {

    final User user = Security.getUser().get();

    final List<String> addresses = Lists.newArrayList(Splitter.on(",").trimResults()
        .split(Http.Request.current().remoteAddress));

    if (!officeManager.getOfficesWithAllowedIp(addresses).contains(user.person.office)) {

      flash.error("Le timbrature web per la persona indicata non sono abilitate da questo"
          + "terminale! Inserire l'indirizzo ip nella configurazione della propria sede per"
          + " abilitarlo");
      show();

    }

    final PersonDay personDay = personDayDao.getOrBuildPersonDay(user.person, LocalDate.now());

    final Stamping stamping = new Stamping(personDay, LocalDateTime.now());

    for (Stamping s : stamping.personDay.stampings) {

      if (Minutes.minutesBetween(s.date, stamping.date).getMinutes() < 1
          || (s.way.equals(stamping.way)
          && Minutes.minutesBetween(s.date, stamping.date).getMinutes() < 2)) {

        flash.error("Impossibile inserire 2 timbrature così ravvicinate."
            + "Attendere 1 minuto per timbrature nel verso opposto o "
            + "2 minuti per timbrature dello stesso verso");
        daySituation();
      }
    }

    stamping.way = way;
    stamping.stampType = stampType;
    stamping.note = note;
    stamping.markedByAdmin = false;

    stamping.save();

    new Notification.NotificationBuilder().destination(personDao.getPersonById(121L).user)
        .message(String.format("Il Tizio %s ha appena eseguito una timbratura web!!!", user.username))
        .subject(NotificationSubject.MESSAGE).create();

    consistencyManager.updatePersonSituation(user.person.id, stamping.personDay.date);

    daySituation();
  }
}
