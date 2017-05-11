package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import controllers.Resecure.NoCheck;

import dao.OfficeDao;
import dao.PersonDao;

import it.cnr.iit.epas.NullStringBinder;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.ConsistencyManager;
import manager.OfficeManager;
import manager.PersonDayManager;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingDayRecapFactory;

import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.enumerate.StampTypes;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Minutes;

import play.data.binding.As;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

@With(Resecure.class)
public class Clocks extends Controller {

  @Inject
  static OfficeDao officeDao;
  @Inject
  static OfficeManager officeManager;
  @Inject
  static PersonDao personDao;
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
      } catch (Throwable ex) {
        // TODO Auto-generated catch block
        ex.printStackTrace();
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

    final LocalDate today = LocalDate.now();

    final PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(user.person, today);

    int numberOfInOut = personDayManager.numberOfInOutInPersonDay(personDay) + 1;

    PersonStampingDayRecap dayRecap = stampingDayRecapFactory
        .create(personDay, numberOfInOut, true, Optional.<List<Contract>>absent());

    render(user, dayRecap, numberOfInOut);

  }

  /**
   * @param wayType verso timbratura.
   */
  public static void webStamping(@Required WayType wayType) {

    if (Validation.hasErrors()) {
      flash.error("E' necessario indicare un verso corretto per la timbratura");
      daySituation();
    }

    final Person currentPerson = Security.getUser().get().person;
    final LocalDate today = LocalDate.now();
    render(wayType, currentPerson, today);
  }

  /**
   * @param way       verso timbratura
   * @param stampType Causale timbratura
   * @param note      eventuali note.
   */
  public static void insertWebStamping(WayType way, StampTypes stampType,
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

    final PersonDay personDay = personDayManager
        .getOrCreateAndPersistPersonDay(user.person, LocalDate.now());
    final Stamping stamping = new Stamping(personDay, LocalDateTime.now());

    stamping.way = way;
    stamping.stampType = stampType;
    stamping.note = note;

    validation.valid(stamping);

    if (Validation.hasErrors()) {
      flash.error("Timbratura mal formata. Impossibile salvarla");
      daySituation();
    }

    stamping.personDay.stampings.stream().filter(s -> !stamping.equals(s)).forEach(s -> {

      if (Minutes.minutesBetween(s.date, stamping.date).getMinutes() < 1
          || s.way == stamping.way
          && Minutes.minutesBetween(s.date, stamping.date).getMinutes() < 2) {

        flash.error("Impossibile inserire 2 timbrature così ravvicinate."
            + "Attendere 1 minuto per timbrature nel verso opposto o "
            + "2 minuti per timbrature dello stesso verso");
        daySituation();
      }
    });

    stamping.save();

    consistencyManager.updatePersonSituation(personDay.person.id, personDay.date);

    daySituation();
  }
}
