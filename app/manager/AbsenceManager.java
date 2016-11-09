package manager;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import dao.AbsenceDao;
import dao.ContractDao;
import dao.PersonChildrenDao;
import dao.PersonDayDao;
import dao.PersonReperibilityDayDao;
import dao.PersonShiftDayDao;
import dao.WorkingTimeTypeDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperFactory;

import lombok.extern.slf4j.Slf4j;

import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.response.AbsenceInsertReport;
import manager.response.AbsencesResponse;
import manager.services.vacations.IVacationsService;

import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.PersonChildren;
import models.PersonDay;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.JustifiedTimeAtWork;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.db.jpa.Blob;
import play.libs.Mail;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;


/**
 * Manager per le assenze.
 *
 * @author alessandro
 */
@Slf4j
public class AbsenceManager {

  private static final String DATE_NON_VALIDE = "L'intervallo di date specificato non è corretto";
  private final ContractMonthRecapManager contractMonthRecapManager;
  private final WorkingTimeTypeDao workingTimeTypeDao;
  private final PersonManager personManager;
  private final PersonDayDao personDayDao;
  private final IVacationsService vacationsService;
  private final ContractDao contractDao;
  private final AbsenceDao absenceDao;
  private final PersonReperibilityDayDao personReperibilityDayDao;
  private final PersonShiftDayDao personShiftDayDao;
  private final PersonChildrenDao personChildrenDao;
  private final ConsistencyManager consistencyManager;
  private final ConfigurationManager configurationManager;
  private final IWrapperFactory wrapperFactory;
  private final PersonDayManager personDayManager;
  private final AbsenceComponentDao absenceComponentDao;

  /**
   * Costruttore.
   *
   * @param personDayDao              personDayDao
   * @param workingTimeTypeDao        workingTimeTypeDao
   * @param contractDao               contractDao
   * @param absenceDao                absenceDao
   * @param personReperibilityDayDao  personReperibilityDayDao
   * @param personShiftDayDao         personShiftDayDao
   * @param personChildrenDao         personChildrenDao
   * @param contractMonthRecapManager contractMonthRecapManager
   * @param personManager             personManager
   * @param consistencyManager        consistencyManager
   * @param configurationManager      configurationManager
   * @param wrapperFactory            wrapperFactory
   * @param vacationsService          vacationsService
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
      PersonChildrenDao personChildrenDao,

      ContractMonthRecapManager contractMonthRecapManager,
      PersonManager personManager,
      ConsistencyManager consistencyManager,
      ConfigurationManager configurationManager,
      PersonDayManager personDayManager,

      IWrapperFactory wrapperFactory,
      IVacationsService vacationsService) {

    this.absenceComponentDao = absenceComponentDao;
    this.contractMonthRecapManager = contractMonthRecapManager;
    this.workingTimeTypeDao = workingTimeTypeDao;
    this.personManager = personManager;
    this.personDayDao = personDayDao;
    this.configurationManager = configurationManager;
    this.vacationsService = vacationsService;
    this.contractDao = contractDao;
    this.absenceDao = absenceDao;
    this.personReperibilityDayDao = personReperibilityDayDao;
    this.personShiftDayDao = personShiftDayDao;
    this.personChildrenDao = personChildrenDao;
    this.consistencyManager = consistencyManager;
    this.wrapperFactory = wrapperFactory;
    this.personDayManager = personDayManager;
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
    cmr.year = dateForRecap.getYear();
    cmr.month = dateForRecap.getMonthOfYear();
    cmr.contract = contract;

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
        previousMonthRecap, yearMonth, dateForRecap, otherCompensatoryRest);

    if (recap.isPresent()) {
      int residualMinutes = recap.get().remainingMinutesCurrentYear
          + recap.get().remainingMinutesLastYear;

      for (Absence a : otherCompensatoryRest) {
        residualMinutes -= workingTimeTypeDao.getWorkingTimeType(a.date, contract.person)
            .get().workingTimeTypeDays.get(a.date.getDayOfWeek() - 1).workingTime;
      }
      return residualMinutes >= workingTimeTypeDao
          .getWorkingTimeType(date, contract.person).get().workingTimeTypeDays
          .get(date.getDayOfWeek() - 1).workingTime;
    }
    return false;
  }

  /**
   * Se si vuole solo simulare l'inserimento di una assenza. - no persistenza assenza - no ricalcoli
   * person situation - no invio email per conflitto reperibilità
   */
  public AbsenceInsertReport insertAbsenceSimulation(
      Person person, LocalDate dateFrom, Optional<LocalDate> dateTo,
      AbsenceType absenceType, Optional<Blob> file, Optional<String> mealTicket,
      Optional<Integer> justifiedMinutes) {

    return insertAbsence(person, dateFrom, dateTo, absenceType, file, mealTicket, justifiedMinutes,
        true, false);
  }

  /**
   * Metodo full per inserimento assenza. - persistenza assenza - ricalcoli person situation - invio
   * email per conflitto reperibilità
   */
  public AbsenceInsertReport insertAbsenceRecompute(
      Person person, LocalDate dateFrom, Optional<LocalDate> dateTo,
      AbsenceType absenceType, Optional<Blob> file, Optional<String> mealTicket,
      Optional<Integer> justifiedMinutes) {

    return insertAbsence(person, dateFrom, dateTo, absenceType, file, mealTicket, justifiedMinutes,
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
      AbsenceType absenceType, Optional<Blob> file, Optional<String> mealTicket,
      Optional<Integer> justifiedMinutes) {

    return insertAbsence(person, dateFrom, dateTo, absenceType, file, mealTicket, justifiedMinutes,
        false, false);
  }

  private AbsenceInsertReport insertAbsence(
      Person person, LocalDate dateFrom, Optional<LocalDate> dateTo,
      AbsenceType absenceType, Optional<Blob> file, Optional<String> mealTicket,
      Optional<Integer> justifiedMinutes, boolean onlySimulation, boolean recompute) {

    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(absenceType);
    Preconditions.checkNotNull(dateFrom);
    Preconditions.checkNotNull(dateTo);
    Preconditions.checkNotNull(file);
    Preconditions.checkNotNull(mealTicket);

    log.info("Ricevuta richiesta di inserimento assenza per {}. AbsenceType = {} dal {} al {}, "
            + "mealTicket = {}. Attachment = {}", person.fullName(), absenceType.code,
        dateFrom, dateTo.or(dateFrom), mealTicket.orNull(), file.orNull());

    AbsenceInsertReport air = new AbsenceInsertReport();

    if (!absenceType.qualifications.contains(person.qualification)) {
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
      } else if (AbsenceTypeMapping.FER.is(absenceType)) {
        aiList.add(
            handlerFer(person, actualDate, absenceType, file, otherAbsences, !onlySimulation));
      } else if (AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.is(absenceType)
          || AbsenceTypeMapping.FERIE_ANNO_CORRENTE.is(absenceType)
          || AbsenceTypeMapping.FESTIVITA_SOPPRESSE.is(absenceType)) {
        aiList.add(
            handler31_32_94(person, actualDate, absenceType, file, otherAbsences, !onlySimulation));
      } else if (AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.is(absenceType)) {
        aiList.add(
            handler37(person, actualDate, absenceType, file, otherAbsences, !onlySimulation));
      } else if ((absenceType.code.startsWith("12") || absenceType.code.startsWith("13"))) {
        // TODO: Inserire i codici di assenza necessari nell'AbsenceTypeMapping
        aiList.add(
            handlerChildIllness(
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

    AbsencesResponse ar = new AbsencesResponse(date, absenceType.code);

    Absence absence = new Absence();
    absence.date = date;
    absence.absenceType = absenceType;
    if (absence.absenceType.justifiedTypesPermitted.size() == 1) {
      absence.justifiedType = absence.absenceType.justifiedTypesPermitted.iterator().next();
    } else if (justifiedMinutes.isPresent()) {
      absence.justifiedMinutes = justifiedMinutes.get();
      absence.justifiedType = absenceComponentDao
          .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);
    } else {
      absence.justifiedType = absenceComponentDao
          .getOrBuildJustifiedType(JustifiedTypeName.all_day);
    }

    //se non devo considerare festa ed è festa non inserisco l'assenza
    if (!absenceType.consideredWeekEnd && personDayManager.isHoliday(person, date)) {
      ar.setHoliday(true);
      ar.setWarning(AbsencesResponse.NON_UTILIZZABILE_NEI_FESTIVI);
      ar.setAbsenceInError(absence);

    } else {
      // check sulla reperibilità
      if (checkIfAbsenceInReperibilityOrInShift(person, date)) {
        ar.setDayInReperibilityOrShift(true);
      }

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
        absence.personDay = pd;
        absence.absenceType = absenceType;
        PersonDay beginAbsence = personDayDao.getPersonDay(person, startAbsence).orNull();
        if (beginAbsence.date.isEqual(date)) {
          absence.absenceFile = file.orNull();
        } else {
          for (Absence abs : beginAbsence.absences) {
            if (abs.absenceFile == null) {
              absence.absenceFile = file.orNull();
            }
          }
        }

        log.info("Inserita nuova assenza {} per {} in data: {}",
            absence.absenceType.code, absence.personDay.person.getFullname(),
            absence.personDay.date);

        pd.absences.add(absence);
        pd.save();

      } else {
        absence.date = pd.date;

        log.debug("Simulato inserimento nuova assenza {} per {} (matricola = {}) in data: {}",
            absence.absenceType.code, pd.person, pd.person.number, absence.date);
      }

      ar.setAbsenceAdded(absence);
      ar.setAbsenceCode(absenceType.code);
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
          .configValue(person.office, EpasParam.EMAIL_TO_CONTACT);

      email.addTo(person.email);
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
    if (person.qualification.qualification <= 3) {
      maxRecoveryDays = (Integer) configurationManager
          .configValue(person.office, EpasParam.MAX_RECOVERY_DAYS_13, date.getYear());
    } else {
      maxRecoveryDays = (Integer) configurationManager
          .configValue(person.office, EpasParam.MAX_RECOVERY_DAYS_49, date.getYear());
    }

    // Raggiunto il limite dei riposi compensativi utilizzabili
    // maxRecoveryDays = 0 -> nessun vincolo sul numero utilizzabile
    if (maxRecoveryDays != 0 && (used >= maxRecoveryDays)) {
      return new AbsencesResponse(date, absenceType.code,
          String.format(AbsencesResponse.RIPOSI_COMPENSATIVI_ESAURITI + " - Usati %s", used));
    }

    //Controllo del residuo
    if (canTakeCompensatoryRest(person, date, otherAbsences)) {
      return insert(person, date, absenceType, file, Optional.<Integer>absent(), persist);
    }

    return new AbsencesResponse(date, absenceType.code, AbsencesResponse.MONTE_ORE_INSUFFICIENTE);
  }

  /**
   * Gestisce l'inserimento esplicito dei codici 31, 32 e 94.
   */
  private AbsencesResponse handler31_32_94(Person person, LocalDate date, AbsenceType absenceType,
      Optional<Blob> file, List<Absence> otherAbsences, boolean persist) {


    if (AbsenceTypeMapping.FERIE_ANNO_CORRENTE.is(absenceType)
        && vacationsService.canTake32(person, date, otherAbsences)) {
      return insert(person, date, absenceType, file, Optional.<Integer>absent(), persist);
    }

    if (AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.is(absenceType)
        && vacationsService.canTake31(person, date, otherAbsences)) {
      return insert(person, date, absenceType, file, Optional.<Integer>absent(), persist);
    }

    if (AbsenceTypeMapping.FESTIVITA_SOPPRESSE.is(absenceType)
        && vacationsService.canTake94(person, date, otherAbsences)) {
      return insert(person, date, absenceType, file, Optional.<Integer>absent(), persist);
    }

    //codice ferie non disponibile
    return new AbsencesResponse(date, absenceType.code,
        AbsencesResponse.NESSUN_CODICE_FERIE_DISPONIBILE_PER_IL_PERIODO_RICHIESTO);
  }

  /**
   * Gestisce una richiesta di inserimento codice 37 (utilizzo ferie anno precedente scadute).
   */
  private AbsencesResponse handler37(Person person, LocalDate date, AbsenceType absenceType,
      Optional<Blob> file, List<Absence> otherAbsences, boolean persist) {

    if (AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.is(absenceType)
        && vacationsService.canTake37(person, date, otherAbsences)) {
      return insert(person, date, absenceType, file, Optional.<Integer>absent(), persist);

    }

    //37 non disponibile
    return new AbsencesResponse(date, absenceType.code,
        AbsencesResponse.NESSUN_CODICE_FERIE_ANNO_PRECEDENTE_37);
  }

  /**
   * Handler inserimento assenza per malattia figli 12* 13*.
   */
  private AbsencesResponse handlerChildIllness(
      Person person, LocalDate date, AbsenceType absenceType,
      Optional<Blob> file, List<Absence> otherAbsences, boolean persist) {

    if (canTakePermissionIllnessChild(person, date, absenceType, otherAbsences)) {
      return insert(person, date, absenceType, file, Optional.<Integer>absent(), persist);
    }

    return new AbsencesResponse(date, absenceType.code,
        AbsencesResponse.CODICI_MALATTIA_FIGLI_NON_DISPONIBILE);
  }

  /**
   * Gestisce l'inserimento dei codici FER, 94-31-32 nell'ordine. Fino ad esaurimento.
   */
  private AbsencesResponse handlerFer(Person person, LocalDate date, AbsenceType absenceType,
      Optional<Blob> file, List<Absence> otherAbsences, boolean persist) {

    AbsenceType wichFer = vacationsService.whichVacationCode(person, date, otherAbsences);

    //FER esauriti
    if (wichFer == null) {
      return new AbsencesResponse(date, absenceType.code,
          AbsencesResponse.NESSUN_CODICE_FERIE_DISPONIBILE_PER_IL_PERIODO_RICHIESTO);
    }
    return insert(person, date, wichFer, file, Optional.<Integer>absent(), persist);
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

    if (abt == null || !abt.code.equals("92")) {
      pd.isTicketForcedByAdmin = false;    //una assenza diversa da 92 ha per forza campo calcolato
      pd.save();
      return;
    }
    if (mealTicket != null && mealTicket.equals("si")) {
      pd.isTicketForcedByAdmin = true;
      pd.isTicketAvailable = true;
      pd.save();
      return;
    }
    if (mealTicket != null && mealTicket.equals("no")) {
      pd.isTicketForcedByAdmin = true;
      pd.isTicketAvailable = false;
      pd.save();
      return;
    }

    if (mealTicket != null && mealTicket.equals("calcolato")) {
      pd.isTicketForcedByAdmin = false;
      pd.save();
      return;
    }
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
        if (absence.absenceType.code.equals(absenceType.code)) {
          if (absence.absenceFile.exists()) {
            absence.absenceFile.getFile().delete();
          }
          absence.delete();
          pd.absences.remove(absence);
          pd.isTicketForcedByAdmin = false;
          deleted++;
          pd.save();
          log.info("Rimossa assenza del {} per {}", actualDate, person.getFullname());
        }
      }
      if (pd.date.isAfter(today) && pd.absences.isEmpty() && pd.stampings.isEmpty()) {
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
   * metodo per stabilire se una persona può ancora prendere o meno giorni di permesso causa
   * malattia del figlio.
   */
  private boolean canTakePermissionIllnessChild(
      Person person, LocalDate date, AbsenceType abt, List<Absence> otherAbsences) {

    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(abt);
    Preconditions.checkNotNull(date);
    Preconditions.checkState(person.isPersistent());
    Preconditions.checkState(abt.isPersistent());

    List<PersonChildren> childList = personChildrenDao.getAllPersonChildren(person);

    // 1.Si verifica come prima cosa che la persona abbia il numero di figli adatto
    // all'utilizzo del codice richiesto

    int childNumber = 1;
    if (abt.code.length() >= 3) {
      // Se il codice è richiesto per i successivi figli lo recupero dal codice
      childNumber = Integer.parseInt(abt.code.substring(2));
    }
    if (childList.size() < childNumber) {
      return false;
    }

    // 2. Si verifica che il figlio sia in età per l'utilizzo del codice d'assenza

    LocalDate limitDate = null;
    PersonChildren child = childList.get(childNumber - 1);
    int yearAbsences = 0;
    if (abt.code.startsWith("12")) {
      limitDate = child.bornDate.plusYears(3);
      yearAbsences = 30;
    }
    if (abt.code.startsWith("13")) {
      limitDate = child.bornDate.plusYears(8);
      yearAbsences = 5;
    }
    if (limitDate.isBefore(date)) {
      return false;
    }

    // 3.  Verifica del numero di assenze prese con quel codice nell'ultimo anno permesso
    LocalDate begin = child.bornDate.withYear(date.getYear());
    if (!begin.isBefore(date)) {
      begin = begin.minusYears(1);
    }

    List<Absence> usateDb = absenceDao.getAbsenceByCodeInPeriod(Optional.of(person),
        Optional.of(abt.code), begin, begin.plusYears(1),
        Optional.<JustifiedTimeAtWork>absent(), false, false);

    // TODO: di otherAbsences devo conteggiare
    // le sole assenze [begin, begin.plusYears(1)]

    int usate = usateDb.size() + otherAbsences.size();

    log.info("usate {}, di totali {}", usate, yearAbsences);
    return usate < yearAbsences;

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
      if (!absentPersons.contains(abs.personDay.person)) {
        absentPersons.add(abs.personDay.person);
      }
    }

    return absentPersons;
  }

  /**
   * La data iniziale di una sequenza consecutiva di assenze dello stesso tipo
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
          pdPrevious.date, Optional.<LocalDate>absent(), false);
      if (abList.size() == 0) {
        begin = true;
      } else {
        for (Absence abs : abList) {
          if (!abs.absenceType.code.equals(absenceType.code)) {
            begin = true;
          } else {
            startAbsence = startAbsence.minusDays(1);
          }
        }
      }
    }
    return startAbsence;
  }

  public enum AbsenceToDate implements Function<Absence, LocalDate> {
    INSTANCE;

    @Override
    public LocalDate apply(Absence absence) {
      return absence.personDay.date;
    }
  }
}
