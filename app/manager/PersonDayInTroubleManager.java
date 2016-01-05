package manager;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import dao.PersonDayDao;
import dao.PersonDayInTroubleDao;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.extern.slf4j.Slf4j;

import models.Contract;
import models.ContractStampProfile;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.enumerate.Parameter;
import models.enumerate.Troubles;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.libs.Mail;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

@Slf4j
public class PersonDayInTroubleManager {

  private final IWrapperFactory factory;
  private final PersonDayInTroubleDao personDayInTroubleDao;
  private final ConfGeneralManager confGeneralManager;

  @Inject
  public PersonDayInTroubleManager(
          PersonDayInTroubleDao personDayInTroubleDao,
          ConfGeneralManager confGeneralManager,
          PersonDayDao personDayDao, IWrapperFactory factory) {

    this.personDayInTroubleDao = personDayInTroubleDao;
    this.confGeneralManager = confGeneralManager;
    this.factory = factory;
  }

  public void setTrouble(PersonDay pd, Troubles cause) {

    for (PersonDayInTrouble pdt : pd.troubles) {
      if (pdt.cause.equals(cause)) {
        // Se esiste gia' non faccio nulla
        return;
      }
    }

    // Se non esiste lo creo
    PersonDayInTrouble trouble = new PersonDayInTrouble(pd, cause);
    trouble.save();
    pd.troubles.add(trouble);

    log.info("Nuovo PersonDayInTrouble {} - {} - {}",
            pd.person.getFullname(), pd.date, cause);
  }


  /**
   * Metodo per rimuovere i problemi con una determinata causale all'interno del
   * personDay.
   */
  public void fixTrouble(final PersonDay pd, final Troubles cause) {

    // Questo codice schianta se si fa una remove e si continua l'iterazione
    //
    //  for(PersonDayInTrouble pdt : pd.troubles){
    //    if( pdt.cause.equals(cause)){
    //      pd.troubles.remove(pdt);
    //      pdt.delete();
    //
    //      log.info("Rimosso PersonDayInTrouble {} - {} - {}",
    //        pd.person.getFullname(), pd.date, cause);
    //     }
    //  }

    Iterables.removeIf(pd.troubles, new Predicate<PersonDayInTrouble>() {
      @Override
      public boolean apply(PersonDayInTrouble pdt) {
        if (pdt.cause.equals(cause)) {
          pdt.delete();

          log.info("Rimosso PersonDayInTrouble {} - {} - {}",
                  pd.person.getFullname(), pd.date, cause);
          return true;
        }
        return false;
      }
    });
  }


  /**
   * Metodo che controlla i giorni con problemi dei dipendenti che non hanno timbratura fixed e
   * invia mail nel caso in cui esistano timbrature disaccoppiate.
   */
  private void checkPersonDayForSendingEmail(
      Person person, LocalDate begin, LocalDate end, String cause) {

    if (person.surname.equals("Conti") && person.name.equals("Marco")) {

      log.debug("Trovato Marco Conti, capire cosa fare con la sua situazione...");
      return;
    }

    Contract currentActiveContract = factory.create(person).getCurrentContract().orNull();
    // Se la persona e' fuori contratto non si prosegue con i controlli
    if (currentActiveContract == null) {
      return;
    }

    DateInterval intervalToCheck = DateUtility.intervalIntersection(
            factory.create(currentActiveContract).getContractDateInterval(),
            new DateInterval(begin, end));

    List<PersonDayInTrouble> pdList = personDayInTroubleDao
            .getPersonDayInTroubleInPeriod(person, intervalToCheck.getBegin(),
                    intervalToCheck.getEnd());

    List<LocalDate> dateTroubleStampingList = new ArrayList<LocalDate>();


    for (PersonDayInTrouble pdt : pdList) {

      Optional<ContractStampProfile> csp = currentActiveContract
              .getContractStampProfileFromDate(pdt.personDay.date);

      if (csp.isPresent() && csp.get().fixedworkingtime == true) {
        continue;
      }

      if (pdt.cause.description.contains(cause) && !pdt.personDay.isHoliday) {
        dateTroubleStampingList.add(pdt.personDay.date);
      }
    }

    boolean flag;
    try {

      flag = sendEmailToPerson(dateTroubleStampingList, person, cause);

    } catch (Exception e) {

      log.error("sendEmailToPerson({}, {}, {}): fallito invio email per {}",
              new Object[]{dateTroubleStampingList, person, cause, person.getFullname()});
      e.printStackTrace();
      return;
    }

    //se ho inviato mail devo andare a settare 'true' i campi emailSent dei
    //personDayInTrouble relativi
    if (flag) {
      for (PersonDayInTrouble pd : pdList) {
        pd.emailSent = true;
        pd.save();
      }
    }
  }

  /**
   * Controlla ogni due giorni la presenza di giorni in cui non ci siano nè assenze nè timbrature
   * per tutti i dipendenti (invocato nell'expandableJob).
   */
  public void sendMail(
      List<Person> personList, LocalDate fromDate, LocalDate toDate, String cause) {

    for (Person p : personList) {

      log.debug("Chiamato controllo sul giorni {}-{}", fromDate, toDate);

      boolean officeMail = confGeneralManager.getBooleanFieldValue(Parameter.SEND_EMAIL, p.office);

      if (p.wantEmail && officeMail) {
        checkPersonDayForSendingEmail(p, fromDate, toDate, cause);
      } else {
        log.info("Non verrà inviata la mail a {} in quanto il campo di invio mail è false",
            p.getFullname());
      }
    }
  }

  /**
   * Invia la mail alla persona specificata in firma con la lista dei giorni in cui ha timbrature
   * disaccoppiate.
   *
   * @param date, person
   */
  private boolean sendEmailToPerson(
      List<LocalDate> dateList, Person person, String cause) throws EmailException {
    if (dateList.size() == 0) {
      return false;
    }
    log.info("Preparo invio mail per {}", person.getFullname());
    SimpleEmail simpleEmail = new SimpleEmail();
    try {
      simpleEmail.addReplyTo(
          confGeneralManager.getFieldValue(Parameter.EMAIL_TO_CONTACT, person.office));
    } catch (EmailException e1) {
      e1.printStackTrace();
    }
    try {
      simpleEmail.addTo(person.email);
    } catch (EmailException e) {

      e.printStackTrace();
    }
    List<LocalDate> dateFormat = new ArrayList<LocalDate>();
    DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MM-YYYY");
    String date = "";
    for (LocalDate d : dateList) {
      if (!DateUtility.isGeneralHoliday(confGeneralManager.officePatron(person.office), d)) {
        dateFormat.add(d);
        String str = fmt.print(d);
        date = date + str + ", ";
      }
    }
    String incipit = "";
    if (dateFormat.size() == 0) {
      return false;
    }
    if (dateFormat.size() > 1) {
      incipit = "Nei giorni: ";
    }
    if (dateFormat.size() == 1) {
      incipit = "Nel giorno: ";
    }

    simpleEmail.setSubject("ePas Controllo timbrature");
    String message = "";
    if (cause.equals("timbratura")) {
      message = "Gentile " + person.name + " " + person.surname
          + "\r\n" + incipit + date + " il sistema ePAS ha rilevato un caso di timbratura "
          + "disaccoppiata. \r\n "
          + "La preghiamo di contattare l'ufficio del personale per regolarizzare la sua "
          + "posizione. \r\n"
          +  "Saluti \r\n"
          + "Il team di ePAS";

    }
    if (cause.equals("no assenze")) {
      message = "Gentile " + person.name + " " + person.surname
          + "\r\n" + incipit + date
          + " il sistema ePAS ha rilevato un caso di mancanza di timbrature e di codici di assenza."
          + " \r\n "
          + "La preghiamo di contattare l'ufficio del personale per regolarizzare la sua posizione."
          + " \r\n"
          + "Saluti \r\n"
          + "Il team di ePAS";
    }

    simpleEmail.setMsg(message);

    Mail.send(simpleEmail);

    log.info("Inviata mail a {} contenente le date da controllare : {}",
        person.getFullname(), date);
    return true;

  }
}
