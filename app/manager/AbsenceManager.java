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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import common.security.SecurityRules;
import controllers.Security;
import dao.AbsenceDao;
import dao.ContractDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonReperibilityDayDao;
import dao.PersonShiftDayDao;
import dao.WorkingTimeTypeDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import it.cnr.iit.epas.DateUtility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.charts.ChartsManager.PersonStampingDayRecapHeader;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.response.AbsenceInsertReport;
import manager.response.AbsencesResponse;
import manager.services.absences.AbsenceService.InsertReport;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.PersonDay;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.CategoryTab;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultGroup;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.MealTicketBehaviour;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.db.jpa.Blob;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.libs.Mail;


/**
 * Manager per le assenze.
 *
 * @author Alessandro Martelli
 */
@Slf4j
public class AbsenceManager {

  private static final String DATE_NON_VALIDE = "L'intervallo di date specificato non è corretto";
  private final ContractMonthRecapManager contractMonthRecapManager;
  private final WorkingTimeTypeDao workingTimeTypeDao;
  private final PersonManager personManager;
  private final PersonDayDao personDayDao;
  private final ContractDao contractDao;
  private final AbsenceDao absenceDao;
  private final PersonReperibilityDayDao personReperibilityDayDao;
  private final PersonShiftDayDao personShiftDayDao;
  private final ConsistencyManager consistencyManager;
  private final ConfigurationManager configurationManager;
  private final IWrapperFactory wrapperFactory;
  private final PersonDayManager personDayManager;
  private final AbsenceComponentDao absenceComponentDao;
  private final NotificationManager notificationManager;
  private final SecurityRules rules;
  private final PersonDao personDao;

  /**
   * Costruttore.
   *
   * @param personDayDao              personDayDao
   * @param workingTimeTypeDao        workingTimeTypeDao
   * @param contractDao               contractDao
   * @param absenceDao                absenceDao
   * @param personReperibilityDayDao  personReperibilityDayDao
   * @param personShiftDayDao         personShiftDayDao
   * @param contractMonthRecapManager contractMonthRecapManager
   * @param personManager             personManager
   * @param consistencyManager        consistencyManager
   * @param configurationManager      configurationManager
   * @param wrapperFactory            wrapperFactory
   */
  @Inject
  public AbsenceManager(
      PersonDayDao personDayDao,
      WorkingTimeTypeDao workingTimeTypeDao,
      ContractDao contractDao,
      AbsenceDao absenceDao,
      AbsenceComponentDao absenceComponentDao,
      PersonReperibilityDayDao personReperibilityDayDao,
      PersonShiftDayDao personShiftDayDao,
      ContractMonthRecapManager contractMonthRecapManager,
      PersonManager personManager,
      ConsistencyManager consistencyManager,
      ConfigurationManager configurationManager,
      PersonDayManager personDayManager,
      IWrapperFactory wrapperFactory,
      NotificationManager notificationManager,
      SecurityRules rules, PersonDao personDao) {

    this.absenceComponentDao = absenceComponentDao;
    this.contractMonthRecapManager = contractMonthRecapManager;
    this.workingTimeTypeDao = workingTimeTypeDao;
    this.personManager = personManager;
    this.personDayDao = personDayDao;
    this.configurationManager = configurationManager;
    this.contractDao = contractDao;
    this.absenceDao = absenceDao;
    this.personReperibilityDayDao = personReperibilityDayDao;
    this.personShiftDayDao = personShiftDayDao;
    this.consistencyManager = consistencyManager;
    this.wrapperFactory = wrapperFactory;
    this.personDayManager = personDayManager;
    this.notificationManager = notificationManager;
    this.rules = rules;
    this.personDao = personDao;
  }

  /**
   * Salva l'assenza.
   *
   * @param insertReport il report di inserimento assenza
   * @param person la persona per cui salvare l'assenza
   * @param from la data da cui salvare
   * @param recoveryDate se esiste una data entro cui occorre recuperare l'assenza (es.:91CE)
   * @param justifiedType il tipo di giustificazione
   * @param groupAbsenceType il gruppo di appartenenza dell'assenza
   */
  public List<Absence> saveAbsences(InsertReport insertReport, Person person, LocalDate from, 
      LocalDate recoveryDate, JustifiedType justifiedType, GroupAbsenceType groupAbsenceType) {

    List<Absence> newAbsences = Lists.newArrayList();
    //Persistenza
    if (!insertReport.absencesToPersist.isEmpty()) {
      for (Absence absence : insertReport.absencesToPersist) {
        PersonDay personDay = personDayManager
            .getOrCreateAndPersistPersonDay(person, absence.getAbsenceDate());
        absence.setPersonDay(personDay);
        if (justifiedType.getName().equals(JustifiedTypeName.recover_time)) {

          absence = handleRecoveryAbsence(absence, person, recoveryDate);
        }
        personDay.getAbsences().add(absence);
        rules.check("AbsenceGroups.save", absence);
        absence.save();
        newAbsences.add(absence);
        personDay.save();

        notificationManager.notificationAbsencePolicy(Security.getUser().get(),
            absence, groupAbsenceType, true, false, false);

      }
      if (!insertReport.reperibilityShiftDate().isEmpty()) {
        sendReperibilityShiftEmail(person, insertReport.reperibilityShiftDate());
        log.info("Inserite assenze con reperibilità e turni {} {}. Le email sono disabilitate.",
            person.fullName(), insertReport.reperibilityShiftDate());
      }
      log.trace("Prima del lancio dei ricalcoli");      
      JPA.em().flush();
      log.trace("Flush dell'entity manager effettuata");

      consistencyManager.updatePersonSituation(person.id, from);
    }
    return newAbsences;
  }

  /**
   * Verifica la possibilità che la persona possa usufruire di un riposo compensativo nella data
   * specificata. Se voglio inserire un riposo compensativo per il mese successivo a oggi considero
   * il residuo a ieri. N.B Non posso inserire un riposo compensativo oltre il mese successivo a
   * oggi.
   */
  private boolean canTakeCompensatoryRest(Person person, LocalDate date,
      List<Absence> otherCompensatoryRest) {
    //Data da considerare

    // (1) Se voglio inserire un riposo compensativo per il mese successivo considero il residuo
    // a ieri.
    //N.B Non posso inserire un riposo compensativo oltre il mese successivo.
    LocalDate dateForRecap = date;
    //Caso generale
    if (dateForRecap.getMonthOfYear() == LocalDate.now().getMonthOfYear() + 1) {
      dateForRecap = LocalDate.now();
    } else if (dateForRecap.getYear() == LocalDate.now().getYear() + 1
        && dateForRecap.getMonthOfYear() == 1 && LocalDate.now().getMonthOfYear() == 12) {
      //Caso particolare dicembre - gennaio
      dateForRecap = LocalDate.now();
    }

    // (2) Calcolo il residuo alla data precedente di quella che voglio considerare.
    if (dateForRecap.getDayOfMonth() > 1) {
      dateForRecap = dateForRecap.minusDays(1);
    }

    Contract contract = contractDao.getContract(dateForRecap, person);

    Optional<YearMonth> firstContractMonthRecap = wrapperFactory
        .create(contract).getFirstMonthToRecap();
    if (!firstContractMonthRecap.isPresent()) {
      //TODO: Meglio ancora eccezione.
      return false;
    }

    ContractMonthRecap cmr = new ContractMonthRecap();
    cmr.setYear(dateForRecap.getYear());
    cmr.setMonth(dateForRecap.getMonthOfYear());
    cmr.setContract(contract);

    YearMonth yearMonth = new YearMonth(dateForRecap);

    //Se serve il riepilogo precedente devo recuperarlo.
    Optional<ContractMonthRecap> previousMonthRecap = Optional.<ContractMonthRecap>absent();

    if (yearMonth.isAfter(firstContractMonthRecap.get())) {
      previousMonthRecap = wrapperFactory.create(contract)
          .getContractMonthRecap(yearMonth.minusMonths(1));
      if (!previousMonthRecap.isPresent()) {
        //TODO: Meglio ancora eccezione.
        return false;
      }
    }

    Optional<ContractMonthRecap> recap = contractMonthRecapManager.computeResidualModule(cmr,
        previousMonthRecap, yearMonth, dateForRecap, otherCompensatoryRest, Optional.absent());

    if (recap.isPresent()) {
      int residualMinutes = recap.get().getRemainingMinutesCurrentYear()
          + recap.get().getRemainingMinutesLastYear();

      return residualMinutes >= workingTimeTypeDao
          .getWorkingTimeType(date, contract.getPerson()).get().getWorkingTimeTypeDays()
          .get(date.getDayOfWeek() - 1).getWorkingTime();
    }
    return false;
  }

  /**
   * Se si vuole solo simulare l'inserimento di una assenza. - no persistenza assenza - no ricalcoli
   * person situation - no invio email per conflitto reperibilità
   */
  public AbsenceInsertReport insertAbsenceSimulation(
      Person person, LocalDate dateFrom, Optional<LocalDate> dateTo, 
      AbsenceType absenceType, Optional<Blob> file, 
      Optional<String> mealTicket, Optional<Integer> justifiedMinutes) {

    return insertAbsence(person, dateFrom, dateTo,  
        absenceType, file, mealTicket, justifiedMinutes,
        true, false);
  }

  /**
   * Metodo full per inserimento assenza. - persistenza assenza - ricalcoli person situation - invio
   * email per conflitto reperibilità
   */
  public AbsenceInsertReport insertAbsenceRecompute(
      Person person, LocalDate dateFrom, Optional<LocalDate> dateTo, 
      Optional<LocalDate> recoveryDate, AbsenceType absenceType, Optional<Blob> file, 
      Optional<String> mealTicket, Optional<Integer> justifiedMinutes) {

    return insertAbsence(person, dateFrom, dateTo, 
        absenceType, file, mealTicket, justifiedMinutes,
        false, true);
  }

  /**
   * Metodo per inserimento assenza senza ricalcoli. (Per adesso utilizzato solo da solari roma per
   * import iniziale di assenze molto indietro nel tempo. Non ritengo ci siano ulteriori utilità
   * future). - persistenza assenza - no ricalcoli person situation - no invio email per conflitto
   * reperibilità
   */
  public AbsenceInsertReport insertAbsenceNotRecompute(
      Person person, LocalDate dateFrom, Optional<LocalDate> dateTo, 
      Optional<LocalDate> recoveryDate, AbsenceType absenceType, Optional<Blob> file, 
      Optional<String> mealTicket, Optional<Integer> justifiedMinutes) {

    return insertAbsence(person, dateFrom, dateTo,  
        absenceType, file, mealTicket, justifiedMinutes,
        false, false);
  }

  private AbsenceInsertReport insertAbsence(
      Person person, LocalDate dateFrom, Optional<LocalDate> dateTo, 
      AbsenceType absenceType, Optional<Blob> file, 
      Optional<String> mealTicket, Optional<Integer> justifiedMinutes, 
      boolean onlySimulation, boolean recompute) {

    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(absenceType);
    Preconditions.checkNotNull(dateFrom);
    Preconditions.checkNotNull(dateTo);
    Preconditions.checkNotNull(file);
    Preconditions.checkNotNull(mealTicket);

    log.debug("Ricevuta richiesta di inserimento assenza per {}. AbsenceType = {} dal {} al {}, "
        + "mealTicket = {}. Attachment = {}, justifiedMinites = {}", 
        person.fullName(), absenceType.getCode(), dateFrom, dateTo.or(dateFrom), 
        mealTicket.orNull(), file.orNull(), justifiedMinutes.orNull());

    AbsenceInsertReport air = new AbsenceInsertReport();

    if (!absenceType.getQualifications().contains(person.getQualification())) {
      log.info("codice {} non utilizzabile per {} con qualifica {}", 
          absenceType, person.getFullname(), person.getQualification());
      air.getWarnings().add(AbsencesResponse.CODICE_NON_UTILIZZABILE);
      return air;
    }


    if (dateTo.isPresent() && dateFrom.isAfter(dateTo.get())) {
      air.getWarnings().add(DATE_NON_VALIDE);
      air.getDatesInTrouble().add(dateFrom);
      air.getDatesInTrouble().add(dateTo.get());
      return air;
    }

    List<Absence> absenceTypeAlreadyExisting = absenceTypeAlreadyExist(
        person, dateFrom, dateTo.or(dateFrom), absenceType);
    if (absenceTypeAlreadyExisting.size() > 0) {
      air.getWarnings().add(AbsencesResponse.CODICE_FERIE_GIA_PRESENTE);
      air.getDatesInTrouble().addAll(
          Collections2.transform(absenceTypeAlreadyExisting, AbsenceToDate.INSTANCE));
      return air;
    }

    List<Absence> allDayAbsenceAlreadyExisting =
        absenceDao.allDayAbsenceAlreadyExisting(person, dateFrom, dateTo);
    if (allDayAbsenceAlreadyExisting.size() > 0) {
      air.getWarnings().add(AbsencesResponse.CODICE_GIORNALIERO_GIA_PRESENTE);
      air.getDatesInTrouble().addAll(
          Collections2.transform(allDayAbsenceAlreadyExisting, AbsenceToDate.INSTANCE));
      return air;
    }

    LocalDate actualDate = dateFrom;

    List<Absence> otherAbsences = Lists.newArrayList();

    while (!actualDate.isAfter(dateTo.or(dateFrom))) {

      List<AbsencesResponse> aiList = Lists.newArrayList();

      if (AbsenceTypeMapping.RIPOSO_COMPENSATIVO.is(absenceType)) {
        aiList.add(
            handlerCompensatoryRest(
                person, actualDate, absenceType, file, otherAbsences, !onlySimulation));
      } else {
        aiList.add(handlerGenericAbsenceType(person, actualDate, absenceType, file,
            mealTicket, justifiedMinutes, !onlySimulation));
      }

      if (onlySimulation) {
        for (AbsencesResponse ai : aiList) {
          if (ai.getAbsenceAdded() != null) {
            otherAbsences.add(ai.getAbsenceAdded());
          } else {
            log.debug("Simulazione inserimento assenza");
          }
        }
      }

      for (AbsencesResponse ai : aiList) {
        air.add(ai);
      }

      actualDate = actualDate.plusDays(1);
    }

    if (!onlySimulation && recompute) {

      //Al termine dell'inserimento delle assenze aggiorno tutta la situazione dal primo giorno
      //di assenza fino ad oggi
      consistencyManager.updatePersonSituation(person.id, dateFrom);

      if (air.getAbsenceInReperibilityOrShift() > 0) {
        sendReperibilityShiftEmail(person, air.datesInReperibilityOrShift());
      }
    }
    return air;
  }

  /**
   * Inserisce l'assenza absenceType nel person day della persona nella data. Se dateFrom = dateTo
   * inserisce nel giorno singolo.
   *
   * @return un resoconto dell'inserimento tramite la classe AbsenceInsertModel
   */
  private AbsencesResponse insert(
      Person person, LocalDate date, AbsenceType absenceType, Optional<Blob> file,
      Optional<Integer> justifiedMinutes, boolean persist) {

    Preconditions.checkNotNull(person);
    Preconditions.checkState(person.isPersistent());
    Preconditions.checkNotNull(date);
    Preconditions.checkNotNull(absenceType);
    //Preconditions.checkState(absenceType.isPersistent());
    Preconditions.checkNotNull(file);

    AbsencesResponse ar = new AbsencesResponse(date, absenceType.getCode());

    Absence absence = new Absence();
    absence.date = date;
    absence.setAbsenceType(absenceType);
    if (absence.getAbsenceType().getJustifiedTypesPermitted().size() == 1) {
      absence.setJustifiedType(absence.getAbsenceType()
          .getJustifiedTypesPermitted().iterator().next());
    } else if (justifiedMinutes.isPresent()) {
      absence.setJustifiedMinutes(justifiedMinutes.get());
      absence.setJustifiedType(absenceComponentDao
          .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes));
    } else {
      absence.setJustifiedType(absenceComponentDao
          .getOrBuildJustifiedType(JustifiedTypeName.all_day));
    }

    //se non devo considerare festa ed è festa non inserisco l'assenza
    if (!absenceType.isConsideredWeekEnd() && personDayManager.isHoliday(person, date)) {
      ar.setHoliday(true);
      ar.setWarning(AbsencesResponse.NON_UTILIZZABILE_NEI_FESTIVI);
      ar.setAbsenceInError(absence);

    } else {
      // check sulla reperibilità e turno
      if (checkIfAbsenceInReperibilityOrInShift(person, date)) {
        ar.setDayInReperibilityOrShift(true);
      }
      //controllo se la persona è in reperibilità
      ar.setDayInReperibility(
          personReperibilityDayDao.getPersonReperibilityDay(person, date).isPresent());
      //controllo se la persona è in turno
      ar.setDayInShift(personShiftDayDao.getPersonShiftDay(person, date).isPresent());

      final PersonDay pd = personDayManager.getOrCreateAndPersistPersonDay(person, date);

      LocalDate startAbsence = null;
      if (file.isPresent()) {
        startAbsence = beginDateToSequentialAbsences(date, person, absenceType);
        if (startAbsence == null) {
          ar.setWarning(AbsencesResponse.PERSONDAY_PRECEDENTE_NON_PRESENTE);
          return ar;
        }
      } else {
        startAbsence = date;
      }

      if (persist) {
        //creo l'assenza e l'aggiungo
        absence.setPersonDay(pd);
        absence.setAbsenceType(absenceType);
        PersonDay beginAbsence = personDayDao.getPersonDay(person, startAbsence).orNull();
        if (beginAbsence.getDate().isEqual(date)) {
          absence.setAbsenceFile(file.orNull());
        } else {
          for (Absence abs : beginAbsence.getAbsences()) {
            if (abs.getAbsenceFile() == null) {
              absence.setAbsenceFile(file.orNull());
            }
          }
        }

        log.info("Inserita nuova assenza {} per {} in data: {}",
            absence.getAbsenceType().getCode(), absence.getPersonDay().getPerson().getFullname(),
            absence.getPersonDay().getDate());

        pd.getAbsences().add(absence);
        pd.save();

      } else {
        absence.date = pd.getDate();

        log.debug("Simulato inserimento nuova assenza {} per {} (matricola = {}) in data: {}",
            absence.getAbsenceType().getCode(), pd.getPerson(), 
            pd.getPerson().getNumber(), absence.date);
      }

      ar.setAbsenceAdded(absence);
      ar.setAbsenceCode(absenceType.getCode());
      ar.setInsertSucceeded(true);
    }
    return ar;
  }

  /**
   * Controlla che nell'intervallo passato in args non esistano già assenze per quel tipo.
   */
  private List<Absence> absenceTypeAlreadyExist(Person person, LocalDate dateFrom,
      LocalDate dateTo, AbsenceType absenceType) {

    return absenceDao.findByPersonAndDate(person, dateFrom, Optional.of(dateTo),
        Optional.of(absenceType)).list();
  }

  /**
   * Metodo che invia la mail contenente i giorni in cui ci sono inserimenti di assenza in turno o
   * reperibilità.
   */
  public void sendReperibilityShiftEmail(Person person, List<LocalDate> dates) {
    MultiPartEmail email = new MultiPartEmail();

    try {
      String replayTo = (String) configurationManager
          .configValue(person.getOffice(), EpasParam.EMAIL_TO_CONTACT);

      email.addTo(person.getEmail());
      email.addReplyTo(replayTo);
      email.setSubject("Segnalazione inserimento assenza in giorno con reperibilità/turno");
      String date = "";
      for (LocalDate data : dates) {
        date = date + data + ' ';
      }
      email.setMsg("E' stato richiesto l'inserimento di una assenza per il giorno " + date
          + " per il quale risulta una reperibilità o un turno attivi. \n"
          + "Controllare tramite la segreteria del personale.\n"
          + "\n Servizio ePas");

    } catch (EmailException ex) {
      // TODO GESTIRE L'Eccezione nella generazione dell'email
      ex.printStackTrace();
    }

    Mail.send(email);
  }

  /**
   * controlla se si sta prendendo un codice di assenza in un giorno in cui si è reperibili.
   *
   * @return true se si sta prendendo assenza per un giorno in cui si è reperibili, false altrimenti
   */
  private boolean checkIfAbsenceInReperibilityOrInShift(Person person, LocalDate date) {

    //controllo se la persona è in reperibilità
    Optional<PersonReperibilityDay> prd =
        personReperibilityDayDao.getPersonReperibilityDay(person, date);
    //controllo se la persona è in turno
    Optional<PersonShiftDay> psd = personShiftDayDao.getPersonShiftDay(person, date);

    return psd.isPresent() || prd.isPresent();
  }

  /**
   * Gestisce l'inserimento dei codici 91 (1 o più consecutivi).
   */
  private AbsencesResponse handlerCompensatoryRest(
      Person person, LocalDate date, AbsenceType absenceType,
      Optional<Blob> file, List<Absence> otherAbsences, boolean persist) {

    // I riposi compensativi sono su base annua e non 'per contratto'
    final LocalDate beginOfYear = new LocalDate(date.getYear(), 1, 1);
    int used = personManager.numberOfCompensatoryRestUntilToday(person, beginOfYear, date)
        + otherAbsences.size();

    Integer maxRecoveryDays;
    if (person.getQualification().getQualification() <= 3) {
      maxRecoveryDays = (Integer) configurationManager
          .configValue(person.getOffice(), EpasParam.MAX_RECOVERY_DAYS_13, date.getYear());
    } else {
      maxRecoveryDays = (Integer) configurationManager
          .configValue(person.getOffice(), EpasParam.MAX_RECOVERY_DAYS_49, date.getYear());
    }

    // Raggiunto il limite dei riposi compensativi utilizzabili
    // maxRecoveryDays = 0 -> nessun vincolo sul numero utilizzabile
    if (maxRecoveryDays != 0 && (used >= maxRecoveryDays)) {
      return new AbsencesResponse(date, absenceType.getCode(),
          String.format(AbsencesResponse.RIPOSI_COMPENSATIVI_ESAURITI + " - Usati %s", used));
    }

    //Controllo del residuo
    if (canTakeCompensatoryRest(person, date, otherAbsences)) {
      return insert(person, date, absenceType, file, Optional.<Integer>absent(), persist);
    }

    return 
        new AbsencesResponse(date, absenceType.getCode(), AbsencesResponse.MONTE_ORE_INSUFFICIENTE);
  }

  private AbsencesResponse handlerGenericAbsenceType(
      Person person, LocalDate date, AbsenceType absenceType, Optional<Blob> file,
      Optional<String> mealTicket, Optional<Integer> justifiedMinutes, boolean persist) {

    AbsencesResponse aim = insert(person, date, absenceType, file, justifiedMinutes, persist);
    if (mealTicket.isPresent() && aim.isInsertSucceeded()) {
      checkMealTicket(date, person, mealTicket.get(), absenceType, persist);
    }

    return aim;
  }

  /**
   * Gestore della logica ticket forzato dall'amministratore, risponde solo in caso di codice 92.
   */
  private void checkMealTicket(LocalDate date, Person person, String mealTicket,
      AbsenceType abt, boolean persist) {

    if (!persist) {
      return;
    }

    Optional<PersonDay> option = personDayDao.getPersonDay(person, date);
    PersonDay pd;
    if (option.isPresent()) {
      pd = option.get();
    } else {
      pd = new PersonDay(person, date);
    }

    if (abt == null || !abt.getCode().equals("92")) {
      pd.setTicketForcedByAdmin(false);    //una assenza diversa da 92 ha per forza campo calcolato
      pd.save();
      return;
    }
    if (mealTicket != null && mealTicket.equals("si")) {
      pd.setTicketForcedByAdmin(true);
      pd.setTicketAvailable(MealTicketBehaviour.allowMealTicket);
      pd.save();
      return;
    }
    if (mealTicket != null && mealTicket.equals("no")) {
      pd.setTicketForcedByAdmin(true);
      pd.setTicketAvailable(MealTicketBehaviour.notAllowMealTicket);
      pd.save();
      return;
    }

    if (mealTicket != null && mealTicket.equals("calcolato")) {
      pd.setTicketForcedByAdmin(false);
      pd.save();
      return;
    }
  }

  /**
   * Metodo di utilità per popolare correttamente i cmapi dell'absence.
   *
   * @param absence l'assenza
   * @param person la persona
   * @param recoveryDate la data entro cui recuperare il riposo compensativo CE
   * @return l'assenza con aggiunti i campi utili per i riposi compensativi a recupero.
   */
  public Absence handleRecoveryAbsence(Absence absence, Person person, LocalDate recoveryDate) {
    absence.setExpireRecoverDate(recoveryDate);
    IWrapperPerson wrPerson = wrapperFactory.create(person);
    Optional<WorkingTimeType> wtt = wrPerson.getCurrentWorkingTimeType();
    if (wtt.isPresent()) {
      java.util.Optional<WorkingTimeTypeDay> wttd = wtt.get().getWorkingTimeTypeDays().stream()
          .filter(w -> w.getDayOfWeek() == absence.getAbsenceDate().getDayOfWeek()).findFirst();
      if (wttd.isPresent()) {
        absence.setTimeToRecover(wttd.get().getWorkingTime());
      } else {
        absence.setTimeToRecover(432);
      }

    }
    return absence;
  }

  /**
   * Rimuove una singola assenza.
   *
   * @param absence l'assenza da rimuovere
   */
  public void removeAbsence(Absence absence) {
    val pd = absence.getPersonDay();

    if (absence.getAbsenceFile().exists()) {
      absence.getAbsenceFile().getFile().delete();
    }

    absence.delete();
    pd.getAbsences().remove(absence);
    pd.setWorkingTimeInMission(0);
    pd.setTicketForcedByAdmin(false);
    pd.save();
    val person = pd.getPerson();
    consistencyManager.updatePersonSituation(person.id, pd.getDate());
    log.info("Rimossa assenza del {} per {}", 
        absence.date, absence.getPersonDay().getPerson().getFullname());
  }

  /**
   * Rimuove le assenze della persona nel periodo selezionato per il tipo di assenza.
   *
   * @param person      persona
   * @param dateFrom    data inizio
   * @param dateTo      data fine
   * @param absenceType tipo assenza da rimuovere
   * @return numero di assenze rimosse
   */
  public int removeAbsencesInPeriod(Person person, LocalDate dateFrom,
      LocalDate dateTo, AbsenceType absenceType) {

    LocalDate today = LocalDate.now();
    LocalDate actualDate = dateFrom;
    int deleted = 0;
    while (!actualDate.isAfter(dateTo)) {

      List<PersonDay> personDays =
          personDayDao.getPersonDayInPeriod(person, actualDate, Optional.<LocalDate>absent());
      PersonDay pd = FluentIterable.from(personDays).first().orNull();

      //Costruisco se non esiste il person day
      if (pd == null) {
        actualDate = actualDate.plusDays(1);
        continue;
      }

      List<Absence> absenceList =
          absenceDao
          .getAbsencesInPeriod(
              Optional.fromNullable(person), actualDate, Optional.<LocalDate>absent(), false);

      for (Absence absence : absenceList) {
        if (absence.getAbsenceType().getCode().equals(absenceType.getCode())) {
          if (absence.getAbsenceFile().exists()) {
            absence.getAbsenceFile().getFile().delete();
          }

          absence.delete();
          pd.getAbsences().remove(absence);
          pd.setWorkingTimeInMission(0);
          pd.setTicketForcedByAdmin(false);
          deleted++;
          pd.save();
          log.info("Rimossa assenza del {} per {}", actualDate, person.getFullname());
        }
      }
      if (pd.getDate().isAfter(today) && pd.getAbsences().isEmpty() 
          && pd.getStampings().isEmpty()) {
        //pd.delete();
        pd.reset();
      }
      actualDate = actualDate.plusDays(1);
    }

    //Al termine della cancellazione delle assenze aggiorno tutta la situazione dal primo
    //giorno di assenza fino ad oggi
    consistencyManager.updatePersonSituation(person.id, dateFrom);

    return deleted;
  }

  /**
   * Costruisce la liste delle persone assenti nel periodo indicato.
   *
   * @param absencePersonDays lista di giorni di assenza effettuati
   * @return absentPersons lista delle persone assenti coinvolte nelle assenze passate
   * @author arianna
   */
  public List<Person> getPersonsFromAbsentDays(List<Absence> absencePersonDays) {
    List<Person> absentPersons = new ArrayList<Person>();
    for (Absence abs : absencePersonDays) {
      if (!absentPersons.contains(abs.getPersonDay().getPerson())) {
        absentPersons.add(abs.getPersonDay().getPerson());
      }
    }

    return absentPersons;
  }

  /**
   * La data iniziale di una sequenza consecutiva di assenze dello stesso tipo.
   *
   * @param date        data
   * @param person      persona
   * @param absenceType tipo assenza
   * @return data iniziale.
   */
  private LocalDate beginDateToSequentialAbsences(LocalDate date, Person person,
      AbsenceType absenceType) {

    boolean begin = false;
    LocalDate startAbsence = date;
    while (begin == false) {
      PersonDay pdPrevious = personDayDao.getPreviousPersonDay(person, startAbsence);
      if (pdPrevious == null) {
        log.warn("Non è presente il personday precedente a quello in cui "
            + "si vuole inserire il primo giorno di assenza per il periodo. Verificare");
        return null;
      }
      List<Absence> abList = absenceDao.getAbsencesInPeriod(Optional.fromNullable(person),
          pdPrevious.getDate(), Optional.<LocalDate>absent(), false);
      if (abList.size() == 0) {
        begin = true;
      } else {
        for (Absence abs : abList) {
          if (!abs.getAbsenceType().getCode().equals(absenceType.getCode())) {
            begin = true;
          } else {
            startAbsence = startAbsence.minusDays(1);
          }
        }
      }
    }
    return startAbsence;
  }

  public Map<Person, List<Absence>> createParentalMap(
      Office office, int year, int month) {
    YearMonth yearMonth = new YearMonth(year, month);
    Set<Office> officeSet = Sets.newHashSet();
    officeSet.add(office);
    Map<Person, List<Absence>> map = Maps.newHashMap();
    List<Person> activePeople = personDao.getActivePersonInMonth(officeSet, yearMonth);
    List<Absence> parentalWithoutAttachment = Lists.newArrayList();
    List<String> codes = DefaultGroup.parentalLeaveAndChildIllnessCodes();
    for (Person person : activePeople) {
      List<Absence> absencesInMonth = 
          absenceDao.getAbsencesNotInternalUseInMonth(person, year, month);
      parentalWithoutAttachment = absencesInMonth.stream()
          .filter(abs -> codes.contains(abs.getCode()) && !abs.getAbsenceFile().exists())
          .collect(Collectors.toList());
      if (!parentalWithoutAttachment.isEmpty()) {
        map.put(person, parentalWithoutAttachment);
      }      
    }
    return map;
  }

  /**
   * Calcola il tempo giustitifcato da un'assenza in minuti. Il calcolo è
   * effettuato tenendo conto della tipologia di assenza (se a completamento, 
   * se tutto il giorno, etc) e dell'orario della persona nel giorno.
   *
   * @param absence assenza di cui calcolare il tempo giustificato.
   * @return i minuti che sono giustificati dall'assenza.
   */
  public Integer getJustifiedMinutes(Absence absence) {
    Integer timeToJustify = absence.getJustifiedMinutes();
    Optional<WorkingTimeTypeDay> workingTimeTypeDay = 
        workingTimeTypeDao.getWorkingTimeTypeDay(
            absence.getPersonDay().getDate(), absence.getPersonDay().getPerson());
    if (workingTimeTypeDay.isPresent()) {
      if (absence.getJustifiedType().getName().equals(JustifiedTypeName.all_day) 
          || absence.getJustifiedType().getName().equals(JustifiedTypeName.assign_all_day)) {
        timeToJustify = workingTimeTypeDay.get().getWorkingTime();
      }
      if (absence.getJustifiedType().getName()
          .equals(JustifiedTypeName.complete_day_and_add_overtime)) {
        timeToJustify = 
            workingTimeTypeDay.get().getWorkingTime() - absence.getPersonDay().getStampingsTime();
      }
      if (absence.getJustifiedType().getName().equals(JustifiedTypeName.absence_type_minutes)) {
        timeToJustify = absence.getAbsenceType().getJustifiedTime();
      }
    }
    return timeToJustify;
  }

  /**
   * Function per la trasformazione da Absence a LocalDate.
   */
  public enum AbsenceToDate implements Function<Absence, LocalDate> {
    INSTANCE;

    @Override
    public LocalDate apply(Absence absence) {
      return absence.getPersonDay().getDate();
    }
  }

  /**
   * Ritorna un calendario con le ics dei giorni di assenza.
   */
  public Calendar createIcsAbsencesCalendar(
      LocalDate from, LocalDate to, List<Absence> absences) {

    // Create a calendar
    //---------------------------
    Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
    icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
    icsCalendar.getProperties().add(CalScale.GREGORIAN);
    icsCalendar.getProperties().add(Version.VERSION_2_0);

    for (Absence absence : absences) {
      log.trace("absence={}, date = {}", absence, absence.getPersonDay().getDate());
      Date date = new Date(absence.getPersonDay().getDate()
          .toDateTimeAtStartOfDay(DateTimeZone.UTC).toDate().getTime());
      VEvent absenceEvent = new VEvent(date, Duration.ofDays(1), absence.getAbsenceType().getLabel());
      absenceEvent.getProperties().add(new Uid(UUID.randomUUID().toString()));
      icsCalendar.getComponents().add(absenceEvent);
    }

    return icsCalendar;
  }

  /**
   * Metodo di utilità da chiamare dal controller Administration per generare il file che contiene
   * la lista dei codici raggruppati con le loro caratteristiche.
   * @param categoryTabs la lista dell categorie dei codici
   * @return il file generato contenente i codici raggruppati con le loro caratteristiche.
   */
  public InputStream buildFile(List<CategoryTab> categoryTabs) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(out);
    byte[] buffer = new byte[1024];
    File file = null;
    try {
      try {
        file = File.createTempFile(
            "statoAssenzeEpas", ".xls");
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      Workbook wb = new HSSFWorkbook();
      FileOutputStream outStream = new FileOutputStream(file);
      Sheet sheet = wb.createSheet("codiciAssenza"); 

      Row row = null;
      Cell cell = null;

      row = sheet.createRow(0);
      row.setHeightInPoints(30);
      for (int i = 0; i < 5; i++) {
        sheet.setColumnWidth((short) i, (short) ((50 * 8) / ((double) 1 / 20)));
        cell = row.createCell(i);        
        switch (i) {
          case 0:
            cell.setCellValue("Gruppo");
            break;
          case 1:
            cell.setCellValue("Periodo");
            break;
          case 2:
            cell.setCellValue("Limite");
            break;
          case 3:
            cell.setCellValue("Codici");
            break;
          case 4:
            cell.setCellValue("Completamenti e Codici");
            break;
          default:
            break;
        }
      }
      int rownum = 1;
      for (CategoryTab category : categoryTabs) {
        for (CategoryGroupAbsenceType categoryGroup : category.getCategoryGroupAbsenceTypes()) {
          for (GroupAbsenceType group : categoryGroup.getGroupAbsenceTypes()) {
            row = sheet.createRow(rownum);

            for (int cellnum = 0; cellnum < 5; cellnum++) {
              cell = row.createCell(cellnum);
              switch (cellnum) {
                case 0:
                  cell.setCellValue(group.getDescription());
                  break;
                case 1:
                  cell.setCellValue(Messages.get(group.getPeriodType().toString()));
                  break;
                case 2:
                  if (group.getTakableAbsenceBehaviour().getFixedLimit() > 0) {
                    cell.setCellValue(group.getTakableAbsenceBehaviour().getFixedLimit().toString() +" "
                        + Messages.get(group.getTakableAbsenceBehaviour().getAmountType()));
                  } else {
                    cell.setCellValue("Nessun limite");
                  }           
                  break;
                case 3:
                  for (AbsenceType abt : group.getTakableAbsenceBehaviour().getTakableCodes()) {
                    String temp = cell.getStringCellValue();
                    cell.setCellValue(temp + " " + abt.getCode() +" ");
                  }
                  break;
                case 4:
                  if (group.getComplationAbsenceBehaviour() != null) {
                    cell.setCellValue("Completamento in " + Messages.get(group.getComplationAbsenceBehaviour().getAmountType()));
                    for (AbsenceType abt : group.getComplationAbsenceBehaviour().getComplationCodes()) {
                      if (!abt.isExpired()) {
                        String temp = cell.getStringCellValue();
                        cell.setCellValue(temp + " " + abt.getCode() +" ");
                      }                      
                    }
                  }
                  break;
                default:
                  break;
              }
            }
            rownum++;
          }
        }
      }		      

      try {
        wb.write(outStream);
        wb.close();
        out.close();
      } catch (IOException ex) {
        log.error("problema in chiusura stream");
        ex.printStackTrace();
      }
    } catch (IllegalArgumentException | FileNotFoundException ex) {
      log.error("Problema in riconoscimento file");
      ex.printStackTrace();
    }

    // faccio lo stream da inviare al chiamante...
    FileInputStream in = null;
    try {
      in = new FileInputStream(file);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      zos.putNextEntry(new ZipEntry(file.getName()));
      int length;
      while ((length = in.read(buffer)) > 0) {
        zos.write(buffer, 0, length);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    try {
      in.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    file.delete();
    try {
      zos.closeEntry();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      zos.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return new ByteArrayInputStream(out.toByteArray());
  }



  public InputStream buildAbsenceTypeListFile(List<AbsenceType> absenceTypeList) throws FileNotFoundException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(out);
    byte[] buffer = new byte[1024];
    File file = null;
    try {
      file = File.createTempFile(
          "codiciAssenzaEpas", ".xls");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Workbook wb = new HSSFWorkbook();

    FileOutputStream outStream = new FileOutputStream(file);

    Sheet sheet = wb.createSheet("codiciAssenza");
    Row row = null;
    Cell cell = null;

    row = sheet.createRow(0);
    row.setHeightInPoints(30);
    for (int i = 0; i < 5; i++) {
      sheet.setColumnWidth((short) i, (short) ((50 * 8) / ((double) 1 / 20)));
      cell = row.createCell(i);        
      switch (i) {
        case 0:
          cell.setCellValue("Codice assenza");
          break;
        case 1:
          cell.setCellValue("Descrizione");
          break;
        case 2:
          cell.setCellValue("Compatibile coi turni");
          break;
        case 3:
          cell.setCellValue("Compatibile con la reperibilità");
          break;
        case 4:
          cell.setCellValue("Usabile nel week end");
          break;
        default:
          break;
      }
    }
    int rownum = 1;
    for (AbsenceType abt : absenceTypeList) {
      row = sheet.createRow(rownum);

      for (int cellnum = 0; cellnum < 5; cellnum++) {
        cell = row.createCell(cellnum);
        switch (cellnum) {
          case 0:
            cell.setCellValue(abt.getCode());
            break;
          case 1:
            cell.setCellValue(abt.getDescription());
            break;
          case 2:
            if (abt.isShiftCompatible()) {
              cell.setCellValue("Sì");
            } else {
              cell.setCellValue("No");
            }            
            break;
          case 3:
            if (abt.isReperibilityCompatible()) {
              cell.setCellValue("Sì");
            } else {
              cell.setCellValue("No");
            }            
            break;
          case 4:
            if (abt.isConsideredWeekEnd()) {
              cell.setCellValue("Sì");
            } else {
              cell.setCellValue("No");
            }            
            break;
          default:
            break;
        }
      }
      rownum++;
    }
    try {
      wb.write(outStream);
      wb.close();
      out.close();
    } catch (IOException ex) {
      log.error("problema in chiusura stream");
      ex.printStackTrace();
    }


    // faccio lo stream da inviare al chiamante...
    FileInputStream in = null;
    try {
      in = new FileInputStream(file);
      
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      zos.putNextEntry(new ZipEntry(file.getName()));
      int length;
      while ((length = in.read(buffer)) > 0) {
        zos.write(buffer, 0, length);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    try {
      in.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    file.delete();
    try {
      zos.closeEntry();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      zos.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new ByteArrayInputStream(out.toByteArray());
  }


}
