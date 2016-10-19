package manager;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import dao.PersonDayInTroubleDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.function.WrapperModelFunctionFactory;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.extern.slf4j.Slf4j;

import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;

import models.Contract;
import models.ContractStampProfile;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.enumerate.Troubles;

import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;
import org.joda.time.MonthDay;
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
  private final ConfigurationManager configurationManager;
  private final WrapperModelFunctionFactory wrapperModelFunctionFactory;

  /**
   * Costruttore.
   * @param personDayInTroubleDao personDayInTroubleDao
   * @param configurationManager configurationManager
   * @param factory factory
   * @param wrapperModelFunctionFactory wrapperModelFunctionFactory
   */
  @Inject
  public PersonDayInTroubleManager(
          PersonDayInTroubleDao personDayInTroubleDao,
          ConfigurationManager configurationManager,
          IWrapperFactory factory, 
          WrapperModelFunctionFactory wrapperModelFunctionFactory) {

    this.personDayInTroubleDao = personDayInTroubleDao;
    this.configurationManager = configurationManager;
    this.factory = factory;
    this.wrapperModelFunctionFactory = wrapperModelFunctionFactory;
  }

  /**
   * Crea il personDayInTrouble per quel giorno (se non esiste già).
   * @param pd giorno 
   * @param cause causa
   */
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
   * Invia le email alle persone appartenenti alla lista, inerenti i giorni con problemi 
   * nell'intervallo fromDate, toDate del tipo appartenente alla lista di troblesToSend.
   * @param personList le persone a cui inviare le email
   * @param fromDate da
   * @param toDate fino
   * @param troubleCausesToSend tipi di problemi da inviare.
   */
  public void sendTroubleEmails(List<Person> personList, LocalDate fromDate, LocalDate toDate, 
      List<Troubles> troubleCausesToSend) {

    log.info("Invio mail troubles per i giorni dal {} al {}. I troubles considerati sono {}.", 
        fromDate, toDate, troubleCausesToSend);
    int noContract = 0;
    int noWantEmail = 0;
    int officeDisabled = 0;
    int tr = 0;
    int total = 0;
    for (Person person : personList) {

      // FIXME ma non viene già fatto nella query?? PersonDao -> riga 673
      // Controllo se la persona ha attualmente un contratto attivo ...
      Optional<Contract> currentContract = factory.create(person).getCurrentContract();
      if (!currentContract.isPresent()) {
        log.info("PERSONA SCARTATA PER CONTRATTO NON PRESENTE {}", person);
        noContract++;
        continue;
      }
      
      // Controllo se la persona desidera ricevere le e-mails ...
      
      if (!person.wantEmail) {
        log.info("PERSONA SCARTATA PER WantEmail false {}", person);
        noWantEmail++;
        log.info("Non verrà inviata la mail a {} in quanto il suo campo di invio mail è false",
            person.getFullname());
        continue;
      }
      
      if (!(Boolean)configurationManager.configValue(person.office, EpasParam.SEND_EMAIL)) {
        log.info("Non verrà inviata la mail a {} in quanto "
            + "la sua sede {} ha invio mail disabilitato",
            person.getFullname(), person.office.name);
        log.info("PERSONA SCARTATA PER OfficeDisabled false {}", person);
        officeDisabled++;
        continue;
      }

      if (person.surname.equals("Conti") && person.name.equals("Marco")) {
        continue;
      }

      if (person.office.checkConf(EpasParam.TR_AUTOCERTIFICATION, "true")
          && person.qualification.qualification <= 3) {
        log.info("PERSONA SCARTATA perchè TR {}", person);
        tr++;
        continue;
      }

      total++;
//      DateInterval intervalToCheck = DateUtility.intervalIntersection(
//          factory.create(currentContract.get()).getContractDateInterval(),
//          new DateInterval(fromDate, toDate));
//
//      List<PersonDayInTrouble> pdList = personDayInTroubleDao.getPersonDayInTroubleInPeriod(person,
//          Optional.fromNullable(intervalToCheck.getBegin()),
//          Optional.fromNullable(intervalToCheck.getEnd()));
//
//      // Selezione dei personDayInTroubles da inviare ...
//
//      List<LocalDate> troublesDateToSend = Lists.newArrayList();
//
//      for (PersonDayInTrouble pdt : pdList) {
//
//        if (pdt.personDay.isHoliday) {
//          continue;
//        }
//
//        Optional<ContractStampProfile> csp = currentContract.get()
//            .getContractStampProfileFromDate(pdt.personDay.date);
//        if (csp.isPresent() && csp.get().fixedworkingtime == true) {
//          continue;
//        }
//
//        if (troubleCausesToSend.contains(pdt.cause)) {
//          troublesDateToSend.add(pdt.personDay.date);
//        }
//      }
//
//      if (troublesDateToSend.isEmpty()) {
//        log.info("{} non ha problemi da segnalare.", person.getFullname());
//        continue;
//      }
//
//      try {
//
//        // Invio della e-mail ...
//
//        log.info("Preparo invio mail per {}", person.getFullname());
//        SimpleEmail simpleEmail = new SimpleEmail();
//        String reply = (String)configurationManager
//            .configValue(person.office, EpasParam.EMAIL_TO_CONTACT);
//
//        if (!reply.isEmpty()) {
//          simpleEmail.addReplyTo(reply);
//        }
//        simpleEmail.addTo(person.email);
//        simpleEmail.setSubject("ePas Controllo timbrature");
//        simpleEmail.setMsg(troubleEmailBody(person, troublesDateToSend, troubleCausesToSend));
//        Mail.send(simpleEmail);
//
//        log.info("Inviata mail a {} contenente le date da controllare : {}",
//            person.getFullname(), troublesDateToSend);
//
//        // Imposto il campo e-mails inviate ...
//        for (PersonDayInTrouble pd : pdList) {
//          pd.emailSent = true;
//          pd.save();
//        }

//      } catch (Exception e) {
//
//        log.error("sendEmailToPerson({}, {}, {}): fallito invio email per {}",
//                new Object[]{troublesDateToSend, person, troubleCausesToSend,
//                    person.getFullname()});
//        log.error(e.getStackTrace().toString());
//      }
    }

    log.info("NOCONTRAT {}", noContract);
    log.info("NO WANT EMAIL {}", noWantEmail);
    log.info("OFFICE DISABLED {}", officeDisabled);
    log.info("TR {}", tr);
    log.info("TOTAL {}", total);
  }
  
  /**
   * Formatta il corpo della email da inviare al dipendente con i suoi troubles.
   * @param person persona 
   * @param dates le date da segnalare nel corpo.
   * @param troubleCausesToSend i troubles da inviare.
   * @return il corpo
   */
  private String troubleEmailBody(Person person, List<LocalDate> dates, 
      List<Troubles> troubleCausesToSend) {
    
    // FIXME trovare un modo più furbo per creare la lista delle date.
    
    List<LocalDate> dateFormat = new ArrayList<LocalDate>();
    DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MM-YYYY");
    String date = "";
    for (LocalDate d : dates) {
      
      MonthDay patron = (MonthDay)configurationManager
          .configValue(person.office, EpasParam.DAY_OF_PATRON, d);
      
      if (!DateUtility.isGeneralHoliday(Optional.fromNullable(patron), d)) {
        dateFormat.add(d);
        String str = fmt.print(d);
        date = date + str + ", ";
      }
    }
    String incipit = "";
    if (dateFormat.size() == 0) {
      return "";
    }
    if (dateFormat.size() > 1) {
      incipit = "Nei giorni: ";
    }
    if (dateFormat.size() == 1) {
      incipit = "Nel giorno: ";
    }
    String message = "";
    
    // caso del Expandable
    if (troubleCausesToSend.contains(Troubles.NO_ABS_NO_STAMP)) {
      message = "Gentile " + person.name + " " + person.surname
          + "\r\n" + incipit + date
          + " il sistema ePAS ha rilevato un caso di mancanza di timbrature e di codici di assenza."
          + " \r\n "
          + "La preghiamo di contattare l'ufficio del personale per regolarizzare la sua posizione."
          + " \r\n"
          + "Saluti \r\n"
          + "Il team di ePAS";
    }
    
    //caso del DarkNight
    if (troubleCausesToSend.contains(Troubles.UNCOUPLED_FIXED)) {
      message = "Gentile " + person.name + " " + person.surname
          + "\r\n" + incipit + date + " il sistema ePAS ha rilevato un caso di timbratura "
          + "disaccoppiata. \r\n "
          + "La preghiamo di contattare l'ufficio del personale per regolarizzare la sua "
          + "posizione. \r\n"
          +  "Saluti \r\n"
          + "Il team di ePAS";

    }
    
    return message;
  }

  /**
   * Elimina i personDayInTrouble che non appartengono ad alcun contratto valido ePAS. (Cioè
   * anche quelli che appartengono ad un contratto ma sono precedenti la sua inizializzazione). 
   * @param person persona
   */
  public final void cleanPersonDayInTrouble(Person person) {
    
    final List<PersonDayInTrouble> pdtList = personDayInTroubleDao.getPersonDayInTroubleInPeriod(
        person, Optional.<LocalDate>absent(), Optional.<LocalDate>absent());

    List<IWrapperContract> wrapperContracts = FluentIterable.from(person.contracts)
        .transform(wrapperModelFunctionFactory.contract()).toList();
    
    for (PersonDayInTrouble pdt : pdtList) {
      boolean toDelete = true;
      for (IWrapperContract wrContract : wrapperContracts) {
        if ( DateUtility.isDateIntoInterval(pdt.personDay.date, 
            wrContract.getContractDatabaseInterval())) {
          toDelete = false;
          break;
        }
      }
      if (toDelete) {
        log.info("Eliminato Pd-Trouble di {} data {}", person.fullName(), pdt.personDay.date);
        pdt.delete();
      }
    }
  }
}
