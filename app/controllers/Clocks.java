package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;

import manager.ConfGeneralManager;
import manager.ConsistencyManager;
import manager.OfficeManager;
import manager.PersonDayManager;
import manager.recaps.personStamping.PersonStampingDayRecap;
import manager.recaps.personStamping.PersonStampingDayRecapFactory;

import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Minutes;

import controllers.Resecure.NoCheck;
import play.Logger;
import play.Play;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;
import security.SecurityRules;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

@With({RequestInit.class, Resecure.class})
public class Clocks extends Controller {

  public final static String SKIP_IP_CHECK = "skip.ip.check";
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static OfficeManager officeManager;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static ConfGeneralManager confGeneralManager;
  @Inject
  private static PersonDayManager personDayManager;
  @Inject
  private static PersonStampingDayRecapFactory stampingDayRecapFactory;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static SecurityRules rules;

  @NoCheck
  public static void show() {

    LocalDate data = new LocalDate();
    Set<Office> offices;

    if ("true".equals(Play.configuration.getProperty(SKIP_IP_CHECK))) {
      offices = FluentIterable.from(officeDao.getAllOffices()).toSet();
    } else {
      String remoteAddress = Http.Request.current().remoteAddress;
      offices = officeManager.getOfficesWithAllowedIp(remoteAddress);
    }

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

    List<Person> personList = personDao.list(Optional.<String>absent(), offices, false, data, data, true).list();
    render(data, personList);
  }

  @NoCheck
  public static void clockLogin(Person person, String password) {

    if (person == null) {
      flash.error("Selezionare una persona dall'elenco del personale.");
      show();
    }

    User user = person.user;
    if (user == null) {
      flash.error("La persona selezionata non dispone di un account valido."
              + " Contattare l'amministratore");
      show();
    }

    if (!"true".equals(Play.configuration.getProperty(SKIP_IP_CHECK))) {

      String addressesAllowed = confGeneralManager.getFieldValue(Parameter.ADDRESSES_ALLOWED, user.person.office);

      if (!addressesAllowed.contains(Http.Request.current().remoteAddress)) {

        flash.error("Le timbrature web per la persona indicata non sono abilitate da questo terminale!" +
                "Inserire l'indirizzo ip nella configurazione della propria sede per abilitarlo");
        show();
      }
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
//		Se non e' presente lo user in sessione non posso accedere al metodo per via della resecure,
//		Quindi non dovrebbe mai accadere di avere a questo punto uno user null.
    User user = Security.getUser().orNull();

    if (!"true".equals(Play.configuration.getProperty(SKIP_IP_CHECK))) {

      String addressesAllowed = confGeneralManager.getFieldValue(Parameter.ADDRESSES_ALLOWED, user.person.office);

      if (!addressesAllowed.contains(Http.Request.current().remoteAddress)) {

        flash.error("Le timbrature web per la persona indicata non sono abilitate da questo terminale!" +
                "Inserire l'indirizzo ip nella configurazione della propria sede per abilitarlo");
        show();
      }
    }

    LocalDate today = LocalDate.now();

    PersonDay personDay = personDayDao.getPersonDay(user.person, today).orNull();

    if (personDay == null) {
      Logger.debug("Prima timbratura per %s non c'è il personday quindi va creato.", user.person.fullName());
      personDay = new PersonDay(user.person, today);
      personDay.create();
    }

    int numberOfInOut = personDayManager.numberOfInOutInPersonDay(personDay) + 1;

    PersonStampingDayRecap dayRecap = stampingDayRecapFactory
            .create(personDay, numberOfInOut, Optional.<List<Contract>>absent());

    render(user, dayRecap, numberOfInOut);

  }

  public static void webStamping(Long personDayId, WayType wayType) {

    PersonDay personDay = personDayDao.getPersonDayById(personDayId);

    if (personDay == null || personDay.person != Security.getUser().get().person) {
      flash.error("Errore nel recupero del personDay");
      daySituation();
    }

    render(personDay, wayType);
  }


  public static void insertWebStamping(Stamping stamping) {

    rules.checkIfPermitted(stamping.personDay.person);

    User user = Security.getUser().orNull();

    if (!"true".equals(Play.configuration.getProperty(SKIP_IP_CHECK))) {

      String addressesAllowed = confGeneralManager.getFieldValue(Parameter.ADDRESSES_ALLOWED, user.person.office);

      if (!addressesAllowed.contains(Http.Request.current().remoteAddress)) {

        flash.error("Le timbrature web per la persona indicata non sono abilitate da questo terminale!" +
                "Inserire l'indirizzo ip nella configurazione della propria sede per abilitarlo");
        show();
      }
    }

    stamping.date = LocalDateTime.now();

    for (Stamping s : stamping.personDay.stampings) {

      if (Minutes.minutesBetween(s.date, stamping.date).getMinutes() < 1
              || (s.way.equals(stamping.way) &&
              Minutes.minutesBetween(s.date, stamping.date).getMinutes() < 2)) {

        flash.error("Impossibile inserire 2 timbrature così ravvicinate."
                + "Attendere 1 minuto per timbrature di verso opposto o "
                + "2 minuti per timbrature dello stesso verso");
        daySituation();
      }
    }

    stamping.markedByAdmin = false;
    stamping.save();

    consistencyManager.updatePersonSituation(stamping.personDay.person.id, stamping.personDay.date);

    daySituation();
  }
}
