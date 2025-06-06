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

package manager;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dao.PersonDayInTroubleDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.function.WrapperModelFunctionFactory;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.User;
import models.enumerate.Troubles;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;
import play.jobs.Job;
import play.libs.Mail;

/**
 * Manager per la gestione dei giorni con problemi di timbrature/assenze.
 */
@Slf4j
public class PersonDayInTroubleManager {

  private final IWrapperFactory factory;
  private final PersonDayInTroubleDao personDayInTroubleDao;
  private final ConfigurationManager configurationManager;
  private final WrapperModelFunctionFactory wrapperModelFunctionFactory;

  /**
   * Costruttore.
   *
   * @param personDayInTroubleDao       personDayInTroubleDao
   * @param configurationManager        configurationManager
   * @param factory                     factory
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
   *
   * @param pd    giorno
   * @param cause causa
   */
  public void setTrouble(PersonDay pd, Troubles cause) {

    for (PersonDayInTrouble pdt : pd.getTroubles()) {
      if (pdt.getCause() == cause) {
        // Se esiste gia' non faccio nulla
        return;
      }
    }

    // Se non esiste lo creo
    PersonDayInTrouble trouble = new PersonDayInTrouble(pd, cause);
    trouble.save();
    pd.getTroubles().add(trouble);

    log.info("Nuovo PersonDayInTrouble {} - {} - {}",
        pd.getPerson().getFullname(), pd.getDate(), cause);
  }


  /**
   * Metodo per rimuovere i problemi con una determinata causale all'interno del
   * personDay.
   */
  public void fixTrouble(final PersonDay pd, final Troubles cause) {

    Iterables.removeIf(pd.getTroubles(), pdt -> {
      if (pdt.getCause() == cause) {
        pdt.delete();

        log.info("Rimosso PersonDayInTrouble {} - {} - {}",
            pd.getPerson().getFullname(), pd.getDate(), cause);
        return true;
      }
      return false;
    });
  }

  /**
   * Invia le email alle persone appartenenti alla lista, inerenti i giorni con problemi
   * nell'intervallo fromDate, toDate del tipo appartenente alla lista di troblesToSend.
   *
   * @param personList          le persone a cui inviare le email
   * @param fromDate            da
   * @param toDate              fino
   * @param troubleCausesToSend tipi di problemi da inviare.
   */
  public void sendTroubleEmails(List<Person> personList, LocalDate fromDate, LocalDate toDate,
      List<Troubles> troubleCausesToSend) {

    log.info("Invio mail troubles per i giorni dal {} al {}. I troubles considerati sono {}.",
        fromDate, toDate, troubleCausesToSend);

    for (Person person : personList) {

      final Optional<Contract> currentContract = factory.create(person).getCurrentContract();
      if (!currentContract.isPresent()) {
        log.error("Nessun contratto trovato attivo alla data odierna per {} - {} ", person,
            person.getOffice());
        continue;
      }
      DateInterval intervalToCheck = DateUtility.intervalIntersection(
          factory.create(currentContract.get()).getContractDatabaseInterval(),
          new DateInterval(fromDate, toDate));
      if (intervalToCheck == null) {
        continue;
      }

      List<PersonDayInTrouble> pdList = personDayInTroubleDao.getPersonDayInTroubleInPeriod(person,
          Optional.fromNullable(intervalToCheck.getBegin()),
          Optional.fromNullable(intervalToCheck.getEnd()),
          Optional.of(troubleCausesToSend));

      if (pdList.isEmpty()) {
        log.debug("{} (matricola = {})  non ha problemi da segnalare.", person, person.getNumber());
        continue;
      }

      try {
        // Invio della e-mail ...
        log.trace("Preparo invio mail per {}", person.getFullname());
        SimpleEmail simpleEmail = new SimpleEmail();
        String reply = (String) configurationManager
            .configValue(person.getOffice(), EpasParam.EMAIL_TO_CONTACT);

        if (!reply.isEmpty()) {
          simpleEmail.addReplyTo(reply);
        }
        simpleEmail.addTo(person.getEmail());
        simpleEmail.setSubject("ePas Controllo timbrature");
        simpleEmail.setMsg(troubleEmailBody(person, pdList, troubleCausesToSend));
        Mail.send(simpleEmail);

        log.info("Inviata mail a {} (matricola = {})  per segnalare i problemi {}",
            person, person.getNumber(), troubleCausesToSend);

        // Imposto il campo e-mails inviate ...
        for (PersonDayInTrouble pd : pdList) {
          pd.setEmailSent(true);
          pd.save();
        }

      } catch (Exception ex) {
        log.error("sendEmailToPerson({}, {}, {}): fallito invio email per {}",
            pdList, person, troubleCausesToSend, person.getFullname(), ex);
      }
    }
  }

  /**
   * Formatta il corpo della email da inviare al dipendente con i suoi troubles.
   *
   * @param person              persona
   * @param daysInTrouble       la lista dei personDaysInTrouble da segnalare.
   * @param troubleCausesToSend i troubles da inviare.
   * @return il corpo
   */
  private String troubleEmailBody(Person person, List<PersonDayInTrouble> daysInTrouble,
      List<Troubles> troubleCausesToSend) {

    final String dateFormatter = "dd/MM/YYYY";

    final StringBuilder message = new StringBuilder()
        .append(String.format("Gentile %s,\r\n", person.fullName()));

    // caso del Expandable
    if (troubleCausesToSend.contains(Troubles.NO_ABS_NO_STAMP)) {

      final List<String> formattedDates = daysInTrouble.stream()
          .filter(pdt -> pdt.getCause() == Troubles.NO_ABS_NO_STAMP)
          .map(pdt -> pdt.getPersonDay().getDate()).sorted()
          .map(localDate -> localDate.toString(dateFormatter))
          .collect(Collectors.toList());

      if (!formattedDates.isEmpty()) {
        message.append("\r\nNelle seguenti date: ")
            .append(Joiner.on(", ").skipNulls().join(formattedDates)).append("\r\n")
            .append("Il sistema ePAS ha rilevato un caso di "
                + "mancanza di timbrature e di codici di assenza.\r\n");
      }
    }

    //caso del DarkNight
    if (troubleCausesToSend.contains(Troubles.UNCOUPLED_WORKING)) {

      final List<String> formattedDates = daysInTrouble.stream()
          .filter(pdt -> pdt.getCause() == Troubles.UNCOUPLED_WORKING)
          .map(pdt -> pdt.getPersonDay().getDate()).sorted()
          .map(localDate -> localDate.toString(dateFormatter))
          .collect(Collectors.toList());

      if (!formattedDates.isEmpty()) {
        message.append("\r\nNelle seguenti date: ")
            .append(Joiner.on(", ").skipNulls().join(formattedDates)).append("\r\n")
            .append("Il sistema ePAS ha rilevato un caso di timbratura disaccoppiata.\r\n");
      }
    }

    message.append("\r\nLa preghiamo di contattare l'ufficio del"
        + " personale per regolarizzare la sua posizione.\r\n")
        .append("\r\nSaluti,\r\n")
        .append("Il team di ePAS");

    return message.toString();
  }

  /**
   * Elimina i personDayInTrouble che non appartengono ad alcun contratto valido ePAS. (Cioè
   * anche quelli che appartengono ad un contratto ma sono precedenti la sua inizializzazione).
   *
   * @param person persona
   */
  public final void cleanPersonDayInTrouble(Person person) {
    new Job<Void>() {
      @Override
      public void doJob() {
        final List<PersonDayInTrouble> pdtList = 
            personDayInTroubleDao.getPersonDayInTroubleInPeriod(
            person, Optional.absent(), Optional.absent(), Optional.absent());

        List<IWrapperContract> wrapperContracts = FluentIterable.from(person.getContracts())
            .transform(wrapperModelFunctionFactory.contract()).toList();

        for (PersonDayInTrouble pdt : pdtList) {
          boolean toDelete = wrapperContracts.stream()
              .noneMatch(wrContract -> DateUtility.isDateIntoInterval(pdt.getPersonDay().getDate(),
                  wrContract.getContractDatabaseInterval()));
          if (toDelete) {
            log.info("Eliminato Pd-Trouble di {} data {}", person.fullName(),
                pdt.getPersonDay().getDate());
            pdt.delete();
          }
        }         
      }
    }.now();

  }
  
  /**
   * Metodo che invia le mail agli amministratori del personale delle varie sedi per informare
   * sui giorni con problemi dei dipendenti della loro sede.
   * @param daysInTrouble la lista dei giorni con problemi
   * @param administrators la lista degli amministratori di una sede
   * @param office la sede di riferimento
   * @throws EmailException
   */
  public void sendOfficeTroubleEmailsToAdministrators(List<PersonDayInTrouble> daysInTrouble, 
      List<User> administrators, Office office) throws EmailException {
    Map<Person, List<String>> map = createTroubleMap(daysInTrouble);
    String message = createMessageForAdministrator(map);
    for (User user : administrators) {

      SimpleEmail simpleEmail = new SimpleEmail();
      simpleEmail.setSubject("ePas Controllo timbrature per Amministratori");
      
      simpleEmail.setMsg(message);
      if (user == null || user.getPerson() == null) {
        log.info("Trovato utente senza persona associata: {}", user.getUsername());
        continue;
      }
      if (user.getPerson().getEmail() == null) {
        continue;
      }
      simpleEmail.addTo(user.getPerson().getEmail());
      try {
        Mail.send(simpleEmail);
      } catch (Exception ex) {
        log.error("Fallito invio email all'amministratore {} della sede {} per {}",
            user.getPerson().getFullname(), office.getName(), ex);
      }
      
      log.info("Inviata mail a {} per segnalare i problemi dei dipendenti della sede {}",
          user.getPerson(), user.getPerson().getOffice());
    }

  }
  
  /**
   * 
   * @param administrators
   * @param office
   * @param person
   * @throws EmailException
   */
  public void sendTrespassingWeeklyWorkingTimeThreshold(List<User> administrators, Office office, Person person)
      throws EmailException {
    String message = createMessageForTrepassingWeeklyTimeAtWorkThreshold(person);
    for (User user: administrators) {
      SimpleEmail simpleEmail = new SimpleEmail();
      simpleEmail.setSubject("ePAS Controllo superamento limite orario settimanale");
      simpleEmail.setMsg(message);
      if (user == null || user.getPerson() == null) {
        log.info("Trovato utente senza persona associata: {}", user.getUsername());
        continue;
      }
      if (user.getPerson().getEmail() == null) {
        continue;
      }
      simpleEmail.addTo(user.getPerson().getEmail());
      try {
        Mail.send(simpleEmail);
      } catch (Exception ex) {
        log.error("Fallito invio email all'amministratore {} della sede {} per {}",
            user.getPerson().getFullname(), office.getName(), ex);
      }
      
      log.info("Inviata mail a {} per segnalare il superamento del limite orario settimanale di {}",
          user.getPerson().getFullname(), person.getFullname());
    }
    
  }
  
  /**
   * Metodo privato che genera la mappa Persona-lista di stringhe contenente le giornate in cui
   * il dipendente ha un trouble.
   * @param daysInTrouble la lista di trouble 
   * @return la mappa persona-lista di stringhe contenente le giornate in cui i dipendenti
   * hanno un trouble.
   */
  private Map<Person, List<String>> createTroubleMap(List<PersonDayInTrouble> daysInTrouble) {
    final String dateFormatter = "dd/MM/YYYY";
    Map<Person, List<String>> map = Maps.newHashMap();
    for (PersonDayInTrouble pdit : daysInTrouble) {
      List<String> list = map.get(pdit.getPersonDay().getPerson());
      if (list == null || list.isEmpty()) {
        list = Lists.newArrayList();
      }
      list.add(pdit.getPersonDay().getDate().toString(dateFormatter));
      map.put(pdit.getPersonDay().getPerson(), list);
    }
    return map;
  }
  
  /**
   * Metodo che crea il messaggio da mandare agli amministratori.
   * @param map la mappa contenente per ogni persona la lista di date in cui ci sono problemi
   * @return il messaggio da mandare agli amministratori del personale.
   */
  private String createMessageForAdministrator(Map<Person, List<String>> map) {
    final StringBuilder message = new StringBuilder()
        .append(String.format("Qui di seguito i dipendenti che presentano giorni con problemi "
            + "e le date in cui sono stati riscontrati: \r\n"));
    for (Map.Entry<Person, List<String>> entry : map.entrySet()) {
      message.append(String.format("\r\n %s nei giorni: ", entry.getKey().getFullname()));
      for (String s : entry.getValue()) {
        message.append(String.format("%s - ", s));
      }
    }
      
    message.append("\r\nLa preghiamo di verificare con i dipendenti i giorni in oggetto.\r\n")
        .append("\r\nSaluti,\r\n")
        .append("Il team di ePAS");
      
    return message.toString();      
  }
  
  private String createMessageForTrepassingWeeklyTimeAtWorkThreshold(Person person) {
    final StringBuilder message = new StringBuilder()
        .append(String.format("Il dipendente: %s", person.getFullname()));
    message.append(String.format("\r\nha superato il limite settimanale previsto dalla circolare "
        + "12/2004 di 48 ore lavorative."));          
    message.append("\r\nLa preghiamo di verificare con il dipendente il problema in oggetto.\r\n")
        .append("\r\nSaluti,\r\n")
        .append("Il team di ePAS");
      
    return message.toString(); 
  }
}