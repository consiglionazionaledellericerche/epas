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

package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import common.security.SecurityRules;
import controllers.RequestInit.CurrentData;
import dao.AbsenceDao;
import dao.GeneralSettingDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.ShiftTypeMonthDao;
import dao.wrapper.IWrapperFactory;
import helpers.CacheValues;
import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.AbsenceManager;
import manager.CertificationManager;
import manager.attestati.service.ICertificationService;
import manager.attestati.service.PersonCertData;
import manager.configurations.EpasParam;
import models.Office;
import models.Person;
import models.ShiftTypeMonth;
import models.absences.Absence;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Il controller per l'invio dei dati certificati al nuovo attestati.
 *
 * @author Alessandro Martelli
 */
@Slf4j
@With(Resecure.class)
public class Certifications extends Controller {

  @Inject
  static SecurityRules rules;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static ShiftTypeMonthDao shiftTypeMonthDao;
  @Inject
  static IWrapperFactory factory;
  @Inject
  static PersonDao personDao;
  @Inject
  static ICertificationService certService;
  @Inject
  static CacheValues cacheValues;
  @Inject
  static GeneralSettingDao generalSettingDao;
  @Inject
  static CertificationManager certificationManager;
  @Inject
  static AbsenceManager absenceManager;

  private static final String PROCESS_COMMAND_KEY = "id-%s-year-%s-month-%s";

  public static void invalidateOauthTokenCache() {
    certService.invalidateOauthTokenCache();
    flash.success("Invalidata cache OAuth Token");
    Administration.utilities();
  }

  /**
   * Pagina principale nuovo invio attestati.
   *
   * @param officeId sede
   * @param year anno
   * @param month mese
   */
  public static void certifications(Long officeId, Integer year, Integer month) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    //Mese selezionato
    Optional<YearMonth> monthToUpload;

    if (year != null && month != null) {
      monthToUpload = Optional.fromNullable(new YearMonth(year, month));
    } else {
      monthToUpload = factory.create(office).nextYearMonthToUpload();
    }

    Verify.verify(monthToUpload.isPresent());

    int validYear = monthToUpload.get().getYear();
    int validMonth = monthToUpload.get().getMonthOfYear();    

    LocalDate monthBegin = new LocalDate(validYear, validMonth, 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();

    final List<Person> people = personDao.list(Optional.absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();

    if (generalSettingDao.generalSetting().isOnlyMealTicket() == true) {
      //Caso di invio solo buoni pasto per INAF
      render("@partialCertifications", people, office, validYear, validMonth);
      
    } else {
      //Caso di invio totale per CNR
      // Utilizzato per capire quando effettuare l'invio delle informazioni ad attestati
      // Questo perchè se utilizzassimo un controller apposito che si occupa anche di fare la render
      // rimarrebbe l'url nella barra degli indirizzi, un eventuale refresh ne causerebbe il reinvio
      // TODO trovare una soluzione più elegante
      final String commandKey = String.format(PROCESS_COMMAND_KEY, officeId, year, month);
      Boolean process = (Boolean) Cache.get(commandKey);
      Cache.safeDelete(commandKey);

      flash.clear();  //non avendo per adesso un meccanismo di redirect pulisco il flash...

      // Patch per la navigazione del menù ... ####################################
      // Al primo accesso (da menù) dove non ho mese e anno devo prendere il default
      // (NextMonthToUpload). In quel caso aggiorno la sessione nel cookie. Dovrebbe
      // occuparsene la RequestInit.
      session.put("monthSelected", validMonth);
      session.put("yearSelected", validYear);
      renderArgs.put("currentData", new CurrentData(validYear, validMonth,
          Integer.parseInt(session.get("daySelected")),
          Long.parseLong(session.get("personSelected")),
          office.id));
      // ##########################################################################

      
      Set<String> matricoleAttestati = new HashSet<>();
      
      final Map.Entry<Office, YearMonth> cacheKey = new AbstractMap
          .SimpleEntry<>(office, monthToUpload.get());
      
      try {
        matricoleAttestati = cacheValues.attestatiSerialNumbers.get(cacheKey);
      } catch (Exception ex) {
        flash.error("Errore di connessione al server di Attestati - %s",
            cleanMessage(ex).getMessage());
        log.error("Errore durante la connessione al server di attestati: {}", ex.getMessage());
        render(office, validYear, validMonth);
      }

      if (matricoleAttestati.isEmpty()) {
        flash.error("Nessuna matricola presente per il mese %s/%s.\r\n"
            + "Effettuare lo stralcio sul server di Attestati", validMonth, validYear);
        render(office, validYear, validMonth);
      }
      
      final Set<String> matricoleEpas = people.stream().map(person -> person.getNumber())
          .collect(Collectors.toSet());

      final Set<String> notInEpas = Sets.difference(matricoleAttestati, matricoleEpas);

      final Set<String> notInAttestati = Sets.difference(matricoleEpas, matricoleAttestati);

      final Set<String> matchNumbers = Sets.newHashSet(matricoleEpas);
      matchNumbers.retainAll(matricoleAttestati);

      // Controlli sull'abilitazione del calendario turni
      final boolean enabledCalendar = office.getConfigurations().stream()
          .anyMatch(configuration -> configuration.getEpasParam() == EpasParam.ENABLE_CALENDARSHIFT
              && "true".equals(configuration.getFieldValue()));

      final List<ShiftTypeMonth> unApprovedActivities;

      if (enabledCalendar) {
        unApprovedActivities = shiftTypeMonthDao
            .byOfficeInMonth(office, monthToUpload.get()).stream()
            .filter(shiftTypeMonth -> !shiftTypeMonth.isApproved()).collect(Collectors.toList());
      } else {
        unApprovedActivities = new ArrayList<>();
      }
      Map<Person, List<Absence>> parentalMap = absenceManager
          .createParentalMap(office, validYear, validMonth);
      

      render(office, validYear, validMonth, people, notInEpas, notInAttestati, matchNumbers,
          process, unApprovedActivities, enabledCalendar, parentalMap);
    }
    
  }

  /**
   * Metodo scritto per evitare di passare direttamente al controller il boolean per effetuare
   * l'invio degli attestati. In questo nell'url non rimane mai l'indirizzo che effettua l'invio dei
   * dati e se si ricarica la pagina non si corre il rischio di farlo. La soluzione fa abbastanza
   * schifo...trovarne una migliore
   *
   * @param officeId id Ufficio
   * @param year anno
   * @param month mese.
   * @throws NoSuchFieldException gestisce eccezione di campo non presente
   */
  public static void processAll(Long officeId, Integer year, Integer month) 
      throws NoSuchFieldException {

    final String commandKey = String.format(PROCESS_COMMAND_KEY, officeId, year, month);
    Cache.safeAdd(commandKey, Boolean.TRUE, "10s");
    certifications(officeId, year, month);
  }

  /**
   * invalida tutti i parametri in cache legati a quell'ufficio e quel mese per forzarne il
   * ricalcolo con le interrogazioni ad attestati.
   *
   * @param officeId id del'ufficio
   * @param year anno
   * @param month mese.
   * @throws NoSuchFieldException gestisce eccezione di campo non presente
   */
  public static void clearCacheValues(Long officeId, Integer year, Integer month) 
      throws NoSuchFieldException {
    final Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    final YearMonth yearMonth = new YearMonth(year, month);
    final Map.Entry<Office, YearMonth> cacheKey = new AbstractMap
        .SimpleEntry<>(office, yearMonth);

    cacheValues.attestatiSerialNumbers.invalidate(cacheKey);
    cacheValues.elaborationStep.invalidate(cacheKey);

    LocalDate monthBegin = new LocalDate(year, month, 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();

    personDao.list(Optional.absent(), Sets.newHashSet(Lists.newArrayList(office)),
        false, monthBegin, monthEnd, true).list().forEach(person -> {
          final Map.Entry<Person, YearMonth> personKey = new AbstractMap
              .SimpleEntry<>(person, yearMonth);
          cacheValues.personStatus.invalidate(personKey);
        });
    log.info("Svuotati tutti i valori dalla cache: ufficio {} - mese {}/{}", office, month, year);
    certifications(officeId, year, month);
  }

  /**
   * PersonStatus.
   *
   * @param personId id della persona
   * @param year anno
   * @param month mese
   */
  public static void personStatus(Long personId, int year, int month) {

    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person);
    PersonCertData personCertData = null;
    try {
      // Costruisco lo status generale
      final Map.Entry<Person, YearMonth> cacheKey = new AbstractMap
          .SimpleEntry<>(person, new YearMonth(year, month));
      personCertData = cacheValues.personStatus.get(cacheKey);
    } catch (Exception ex) {
      log.error("Errore nel recupero delle informazioni dal server di attestati per la persona {}: "
          + "{}", person, cleanMessage(ex).getMessage());
      render(person);
    }

    // La percentuale di completamento della progress bar rispetto al totale da elaborare
    double stepSize;
    try {
      final Map.Entry<Office, YearMonth> key = new AbstractMap
          .SimpleEntry<>(person.getOffice(), new YearMonth(year, month));
      stepSize = cacheValues.elaborationStep.get(key);
    } catch (Exception ex) {
      log.error("Impossibile recuperare la percentuale di avanzamento per la persona {}: {}",
          person, cleanMessage(ex).getMessage());
      return;
    }

    render(personCertData, stepSize, person);
  }

  /**
   * Codici.
   */
  public static void codici() {
    try {
      renderText(certService.absenceCodes());
    } catch (Exception ex) {
      renderText("Impossibile recuperare i codici dal server di attestati\r\n"
          + cleanMessage(ex).getMessage());
    }
  }

  /**
   * Effettua l'invio dei dati ad attestati e l'elaborazione di una persona.
   *
   * @param personId id della persona
   * @param year anno
   * @param month mese.
   * @throws NoSuchFieldException gestisce eccezione di campo non presente
   */
  public static void process(Long personId, int year, int month, boolean redirect) 
      throws NoSuchFieldException {

    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person);

    PersonCertData previousCertData = null;
    try {
      // Costruisco lo status generale
      // Non uso la cache qui per evitare eventuali stati incongruenti durante l'invio
      previousCertData = certService.buildPersonStaticStatus(person, year, month);
    } catch (Exception ex) {
      log.error("Errore nel recupero delle informazioni dal server di attestati"
          + " per la persona {}: {}", person, cleanMessage(ex).getMessage());
      render();
    }

    PersonCertData personCertData = null;
    if (!previousCertData.validate) {
      // Se l'attestato non è stato validato applico il process
      try {
        personCertData = certService.process(previousCertData);
      } catch (ExecutionException | NoSuchFieldException ex) {
        log.error("Errore nell'invio delle informazioni al server di attestati "
            + "per la persona {}: {}", person, cleanMessage(ex).getMessage());
      }
    }

    final Map.Entry<Person, YearMonth> cacheKey = new AbstractMap
        .SimpleEntry<>(person, new YearMonth(year, month));

    // Se riesco nell'invio ne aggiorno lo stato in cache
    if (personCertData != null) {
      cacheValues.personStatus.put(cacheKey, personCertData);
    } else {
      // Altrimenti invalido il valore presente
      cacheValues.personStatus.invalidate(cacheKey);
    }

    // La percentuale di completamento della progress bar rispetto al totale da elaborare
    double stepSize;
    try {
      final Map.Entry<Office, YearMonth> key = new AbstractMap
          .SimpleEntry<>(person.getOffice(), new YearMonth(year, month));
      stepSize = cacheValues.elaborationStep.get(key);
    } catch (Exception ex) {
      log.error("Impossibile recuperare la percentuale di avanzamento per la persona {}: {}",
          person, cleanMessage(ex).getMessage());
      return;
    }

    // permette di chiamare questo controller anche in maniera sincrona per il reinvio delle
    // informazioni per una sola persona tramite link (button sulla singola persona)
    if (redirect) {
      certifications(person.getOffice().id, year, month);
    }

    render("@personStatus", personCertData, stepSize, person);
    
  }
  
  /**
   * Metodo da chiamare per permettere l'invio dei soli buoni pasto (per INAF).
   *
   * @param officeId l'identificativo della sede
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   */
  public static void sendPartialCertification(Long officeId, int year, int month) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    
    LocalDate monthBegin = new LocalDate(year, month, 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    
    final List<Person> people = personDao.list(Optional.absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();
    
    File file = certificationManager.createFile(people, year, month);
    renderBinary(file);
  }


  /**
   * CleanMessage.
   *
   * @param ex eccezione
   * @return L'ultimo elemento Throwable di una concatenazione di eccezioni
   */
  private static Throwable cleanMessage(Exception ex) {
    // Recupera il messaggio pulito dalla gerarchia delle eccezioni
    Throwable throwable;
    if (ex.getCause() != null) {
      throwable = ex.getCause();
    } else {
      return ex;
    }
    while (throwable.getCause() != null) {
      throwable = throwable.getCause();
    }
    return throwable;
  }

}
