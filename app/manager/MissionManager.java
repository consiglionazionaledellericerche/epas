package manager;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.Security;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.PersonDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.PersonDay;
import models.User;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.CategoryTab;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultGroup;
import models.exports.MissionFromClient;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.testng.collections.Lists;
import play.db.jpa.JPA;

@Slf4j
public class MissionManager {

  private final PersonDao personDao;
  private final AbsenceService absenceService;
  private final AbsenceManager absenceManager;
  private final PersonDayManager personDayManager;
  private final ConsistencyManager consistencyManager;
  private final NotificationManager notificationManager;
  private final AbsenceDao absenceDao;
  private final AbsenceTypeDao absenceTypeDao;
  private final ConfigurationManager configurationManager;
  private final IWrapperFactory wrapperFactory;
  private final AbsenceComponentDao absComponentDao;
  

  public static final String LOG_PREFIX = "Integrazione Missioni. ";
  
  /**
   * Default constructor.
   */
  @Inject
  public MissionManager(PersonDao personDao, AbsenceService absenceService, 
      AbsenceManager absenceManager, PersonDayManager personDayManager,
      ConsistencyManager consistencyManager, NotificationManager notificationManager,
      AbsenceDao absenceDao, AbsenceTypeDao absenceTypeDao, 
      ConfigurationManager configurationManager, 
      IWrapperFactory wrapperFactory, AbsenceComponentDao absComponentDao) {
    this.personDao = personDao;
    this.absenceService = absenceService;
    this.absenceManager = absenceManager;
    this.personDayManager = personDayManager;
    this.consistencyManager = consistencyManager;
    this.notificationManager = notificationManager;
    this.absenceDao = absenceDao;
    this.absenceTypeDao = absenceTypeDao;
    this.configurationManager = configurationManager;
    this.wrapperFactory = wrapperFactory;
    this.absComponentDao = absComponentDao;
  }

  
  /**
   * Metodo che verifica se al numero di matricola passato come parametro nel dto corrisponde
   * effettivamente una persona in anagrafica.
   * @param mission il dto creato dal json arrivato dal listener
   * @return la person, se esiste, associata alla matricola contenuta nel dto.
   */
  public Optional<Person> linkToPerson(MissionFromClient mission) {

    Optional<User> user = Security.getUser();
    if (!user.isPresent()) {
      log.error(LOG_PREFIX + "Impossibile recuperare l'utente che ha inviato la missione: {}.", 
          mission);
      return Optional.absent();
    }

    final Optional<Person> person = Optional.fromNullable(personDao
        .getPersonByNumber(mission.matricola));

    if (person.isPresent()) {
      mission.person = person.get();      
    } else {
      log.warn(LOG_PREFIX +  "Non e' stato possibile recuperare la persona a cui si riferisce la "
          + "missione, matricola={}. Controllare il database.", mission.matricola);
    }

    return person;
  }

  /**
   * Metodo che crea la missione a partire dai dati del dto inviato da Missioni.
   * @param body il dto ricavato dal json arrivato dal listener
   * @param recompute se deve essere avviato il ricalcolo
   * @return true se riesce a inserire la missione con i parametri esplicitati nel body, 
   *     false altrimenti.
   */
  public boolean createMissionFromClient(MissionFromClient body, boolean recompute) {

    //AbsenceForm absenceForm = buildAbsenceForm(body);
    if (body.dataInizio.isAfter(body.dataFine)) {
      log.warn(LOG_PREFIX + "Le date di inizio e fine sono invertite!! La missione {} di {} "
          + "non può essere processata!! Verificare!", body.id, body.person.fullName());
      return false;
    }
    
    Office office = body.person.office;

    //verifico il parametro di ora inizio lavoro in sede
    LocalTimeInterval workInterval = (LocalTimeInterval) configurationManager.configValue(
        office, EpasParam.WORK_INTERVAL_MISSION_DAY, body.dataInizio.toLocalDate());
    if (workInterval == null) {
      log.warn(LOG_PREFIX +  "Il parametro di orario di lavoro missione "
          + "non è valorizzato per la sede {}", office.name);
      return false;
    }
    List<Absence> existingMission = absenceDao.absencesPersistedByMissions(body.id);
    if (!existingMission.isEmpty()) {
      //Se esiste già una missione con quell'identificativo, la scarto.
      log.warn(LOG_PREFIX +  "E' stata riscontrata una missione con lo stesso identificativo di "
          + "quella passata come parametro: {}. Questa missione non viene processata.", body.id);
      return false;      
    }
    
    //controllo se sono già stati inseriti a mano i giorni di missione
    final List<JustifiedTypeName> types = ImmutableList
        .of(JustifiedTypeName.complete_day_and_add_overtime, JustifiedTypeName.specified_minutes);
    List<Absence> existingMissionWithoutId = 
        absenceDao.filteredByTypes(body.person, body.dataInizio.toLocalDate(), 
            body.dataFine.toLocalDate(), types, Optional.<Boolean>absent());
    if (!existingMissionWithoutId.isEmpty()  
        && existingMissionWithoutId.stream().allMatch(abs -> abs.absenceType.code.equals("92") 
            || abs.absenceType.code.equals("92M"))) {
      log.warn(LOG_PREFIX +  "Sono stati riscontrati codici di missione già inseriti manualmente"
          + " nei giorni {}-{}. Questa missione non viene processata.", 
          body.dataInizio.toLocalDate(), body.dataFine.toLocalDate());
      return false;
    }

    Situation situation;
    
    if (body.dataInizio.toLocalDate().isEqual(body.dataFine.toLocalDate())) {
      //caso di giorno unico di missione
      situation = getSituation(body.dataInizio, body, workInterval);
      if (insertMission(body.destinazioneMissione, body.person, 
          new Integer(situation.difference / DateTimeConstants.MINUTES_PER_HOUR), 
          new Integer(situation.difference % DateTimeConstants.MINUTES_PER_HOUR),
          body.dataInizio, body.dataFine, body.id, body.idOrdine, body.anno, body.numero)) {
        recalculate(body, Optional.<List<Absence>>absent());
        return true;
      } else {
        return false;
      }
    }

    LocalDateTime actualDate = body.dataInizio;
    while (!actualDate.toLocalDate().isAfter(body.dataFine.toLocalDate())) {
      situation = getSituation(actualDate, body, workInterval);      

      atomicInsert(situation, body, actualDate);
      actualDate = actualDate.plusDays(1);
      
    }    
    recalculate(body, Optional.<List<Absence>>absent());
    return true;
  }
  
  /**
   * Metodo di utilità per calcolare la situazione delle ore da giustificare per la missione.
   * @param actualDate la data che si sta considerando
   * @param body l'oggetto contenente le info di missione
   * @param workInterval l'intervallo di inizio/fine attività lavorativa per missione
   * @return un oggetto contenente la situazione del giorno di missione che si sta controllando.
   */
  private Situation getSituation(LocalDateTime actualDate, MissionFromClient body, 
      LocalTimeInterval workInterval) {
    Situation situation = new Situation();
    int workMinutes = 0;
    int missionMinutes = 0;
    if (actualDate.toLocalDate().isEqual(body.dataInizio.toLocalDate()) 
        && actualDate.toLocalDate().isEqual(body.dataFine.toLocalDate())) {
      
      //TODO: verificare nel caso di un solo giorno di missioni se è possibile attribuire tutti i
      //minuti in caso di missione che comincia prima dell'inizio della giornata di missione o 
      //termini dopo la fine della giornata di missione
      situation.difference = (body.dataFine.getHourOfDay() 
          * DateTimeConstants.MINUTES_PER_HOUR + body.dataFine.getMinuteOfHour()) 
          - (body.dataInizio.getHourOfDay() 
              * DateTimeConstants.MINUTES_PER_HOUR + body.dataInizio.getMinuteOfHour());
      situation.isFirstOrLastDay = true;

    } else if (actualDate.toLocalDate().isEqual(body.dataInizio.toLocalDate())) {
      //primo giorno di missione
    
      workMinutes = workInterval.to.getHourOfDay() * DateTimeConstants.MINUTES_PER_HOUR
          + workInterval.to.getMinuteOfHour();
      missionMinutes = actualDate.getHourOfDay() * DateTimeConstants.MINUTES_PER_HOUR
          + actualDate.getMinuteOfHour();  
      situation.difference = workMinutes - missionMinutes;
      situation.isFirstOrLastDay = true;
      
    } else if (actualDate.toLocalDate().isEqual(body.dataFine.toLocalDate())) {
      //ultimo giorno di missione

      actualDate = actualDate.hourOfDay().setCopy(body.dataFine.getHourOfDay());
      actualDate = actualDate.minuteOfHour().setCopy(body.dataFine.getMinuteOfHour());
      workMinutes = workInterval.from.getHourOfDay() * DateTimeConstants.MINUTES_PER_HOUR 
          + workInterval.from.getMinuteOfHour();
      missionMinutes = actualDate.getHourOfDay() * DateTimeConstants.MINUTES_PER_HOUR
          + actualDate.getMinuteOfHour(); 
      situation.difference = missionMinutes - workMinutes;
      situation.isFirstOrLastDay = true;
    } else {
      //qualsiasi altro giorno di missione

      situation.difference = 0;
      situation.isFirstOrLastDay = false;
    }
    return situation;
  }

  /**
   * Metodo di utilità.
   * @param body il dto proveniente da Missioni
   * @param actualDate il giorno di missione
   * @return il workingTimeTypeDay dell'ultimo giorno di missione.
   */
  private WorkingTimeTypeDay getFromDayOfMission(Person person, LocalDate actualDate) {
    IWrapperPerson wrappedPerson = wrapperFactory.create(person);  
    Optional<ContractWorkingTimeType> cwtt = wrappedPerson.getCurrentContractWorkingTimeType();
    WorkingTimeTypeDay dayNumber = null;
    if (cwtt.isPresent()) {
      WorkingTimeType wtt = cwtt.get().workingTimeType;
      int day = actualDate.getDayOfWeek();

      for (WorkingTimeTypeDay wttd : wtt.workingTimeTypeDays) {
        if (wttd.dayOfWeek == day) {
          dayNumber = wttd;
        }
      }
    }
    return dayNumber;
  }


  /**
   * Metodo che ricontrolla le date della missione dopo le info pervenute da Missioni in occasione
   * del rimborso.
   * @param body il dto contenente i dati della missione
   * @param recompute se deve essere effettuato il ricalcolo
   * @return true se la gestione della missione è andata a buon fine, false altrimenti.
   */
  public boolean manageMissionFromClient(MissionFromClient body, boolean recompute) {
    if (body.idOrdine == null) {
      return false;
    }
    /*
     * Fase I: controllo i giorni in più o in meno rispetto alla missione originale 
     */
    List<Absence> missions = absenceDao.absencesPersistedByMissions(body.idOrdine);
    if (missions.isEmpty()) {
      return false;
    }
    LocalTimeInterval workInterval = (LocalTimeInterval) configurationManager.configValue(
        body.person.office, EpasParam.WORK_INTERVAL_MISSION_DAY, body.dataInizio.toLocalDate());
    if (workInterval == null) {
      log.warn(LOG_PREFIX +  "Il parametro di orario di lavoro missione "
          + "non è valorizzato per la sede {}", body.person.office.name);
      return false;
    }
    //AbsenceForm absenceForm = buildAbsenceForm(body);
    GroupAbsenceType group = null;
    switch (body.destinazioneMissione) {
      case "ITALIA":
        group = absComponentDao
        .groupAbsenceTypeByName(DefaultGroup.MISSIONE_GIORNALIERA.name()).get();
        break;
      case "ESTERA":
        group = absComponentDao.groupAbsenceTypeByName(DefaultGroup.MISSIONE_ESTERA.name()).get();
        break;
      default:
        break;
    }
    List<LocalDate> dates = datesToCompute(body);
    //lista assenze da rimuovere
    List<Absence> toRemove = missions.stream()
        .filter(abs -> !dates.contains(abs.personDay.date)).collect(Collectors.toList());
    List<LocalDate> missionsDate = missions.stream()
        .map(a -> a.personDay.date).collect(Collectors.toList());
    //lista assenze da inserire
    List<LocalDate> toAdd = dates.stream()
        .filter(p -> !missionsDate.contains(p)).collect(Collectors.toList());        

    if (!performDeleteMission(toRemove, group)) {
      return false;
    }
   
    Situation situation = null;
    for (LocalDate date : toAdd) {
      LocalDateTime dateToConsider = rightDate(date, body);
      if (dateToConsider == null) {
        return false;
      }
      situation = getSituation(dateToConsider, body, workInterval);
      atomicInsert(situation, body, dateToConsider);
      
    }
    /*
     * Fase II: per ogni giorno di missione così corretta, verifico gli orari se sono cambiati
     */
    JPA.em().flush();
    missions = absenceDao.absencesPersistedByMissions(body.idOrdine);
    
    for (Absence abs : missions) {
      LocalDateTime actual = body.dataInizio;
      while (!actual.toLocalDate().isAfter(body.dataFine.toLocalDate())) {
        if (abs.personDay.date.isEqual(actual.toLocalDate())) {
          int time = getFromDayOfMission(body.person, actual.toLocalDate()).workingTime;
          int minutes = abs.justifiedMinutes;
          Situation sit = getSituation(actual, body, workInterval);
          if (minutes != sit.difference) {
            if ((minutes == 0 && sit.difference >= time) 
                || (sit.difference >= time && minutes > time)) {
              log.info("Si inserirebbe un codice di missione identico al precedente. "
                  + "Non faccio niente.");
            } else {
              atomicRemoval(abs, true);
              atomicInsert(sit, body, actual);
            }
          }
        }        
        actual = actual.plusDays(1);
      }
    }    
    
    //consistencyManager.updatePersonSituation(body.person.id, body.dataInizio.toLocalDate());
    recalculate(body, Optional.fromNullable(missions));
    log.debug("Lanciati i ricalcoli");
    return true;
  }

  /**
   * Metodo che cancella una missione inviata precedentemente da Missioni.
   * @param body il dto contenente i dati della missione
   * @param recompute se devono essere effettuati i ricalcoli
   * @return true se la missione viene cancellata, false altrimenti.
   */
  public boolean deleteMissionFromClient(MissionFromClient body, boolean recompute) {
    List<Absence> missions = absenceDao.absencesPersistedByMissions(body.idOrdine);
    if (missions.isEmpty()) {
      return false;
    }
    //AbsenceForm absenceForm = buildAbsenceForm(body);
    GroupAbsenceType group = null;
    switch (body.destinazioneMissione) {
      case "ITALIA":
        group = absComponentDao
        .groupAbsenceTypeByName(DefaultGroup.MISSIONE_GIORNALIERA.name()).get();
        break;
      case "ESTERA":
        group = absComponentDao.groupAbsenceTypeByName(DefaultGroup.MISSIONE_ESTERA.name()).get();
        break;
      default:
        break;
    }
    if (!performDeleteMission(missions, group)) {
      return false;
    }
    recalculate(body, Optional.fromNullable(missions));
    return true;
  }

  /**
   * Metodo privato che ritorna la lista delle date su cui fare i controlli.
   * @param body il dto contenente i dati della missione
   * @return la lista delle date da considerare per gestire la missione.
   */
  private List<LocalDate> datesToCompute(MissionFromClient body) {
    List<LocalDate> dates = Lists.newArrayList();
    LocalDateTime current = body.dataInizio;
    while (!current.toLocalDate().isAfter(body.dataFine.toLocalDate())) {
      dates.add(current.toLocalDate());
      current = current.plusDays(1);
    }
    return dates;
  }

  /**
   * Metodo privato che inserisce i giorni di missione.
   * @param body il dto contenente le informazioni della missione
   * @param absenceForm l'absenceForm relativo alla missione
   * @param hours le ore 
   * @param minutes i minuti
   * @param from la data di inizio da cui inserire la missione
   * @param to la data di fine fino a cui inserire la missione
   * @return true se la missione è stata inserita, false altrimenti.
   */
  private boolean insertMission(String destination, Person person, 
      Integer hours, Integer minutes, LocalDateTime from, LocalDateTime to,
      Long id, Long idOrdine, int anno, Long numero) {

    AbsenceType mission = null;
    JustifiedType type = null;
    GroupAbsenceType group = null;
    switch (destination) {
      case "ITALIA":
        group = absComponentDao
        .groupAbsenceTypeByName(DefaultGroup.MISSIONE_GIORNALIERA.name()).get();
        mission = absenceTypeDao.getAbsenceTypeByCode("92").get(); 
        break;
      case "ESTERA":
        group = absComponentDao.groupAbsenceTypeByName(DefaultGroup.MISSIONE_ESTERA.name()).get();
        mission = absenceTypeDao.getAbsenceTypeByCode("92E").get();
        break;
      default:
        break;
    }
    int quantity = 0;
    if (hours != null && minutes != null) {
      quantity = hours * DateTimeConstants.MINUTES_PER_HOUR + minutes;
    }
    int day = getFromDayOfMission(person, to.toLocalDate()).dayOfWeek;
    if (quantity < 0) {
      mission = absenceTypeDao.getAbsenceTypeByCode("92NG").get();
      type = absComponentDao.getOrBuildJustifiedType(JustifiedTypeName.nothing);
      
    } else if (quantity == 0 || quantity > getFromDayOfMission(person, to.toLocalDate()).workingTime
        || day == DateTimeConstants.SATURDAY || day == DateTimeConstants.SUNDAY) {
      type = absComponentDao
          .getOrBuildJustifiedType(JustifiedTypeName.complete_day_and_add_overtime);
      
    } else {
      group = absComponentDao.groupAbsenceTypeByName(DefaultGroup.MISSIONE_ORARIA.name()).get();
      mission = absenceTypeDao.getAbsenceTypeByCode("92M").get();
      type = absComponentDao
          .getOrBuildJustifiedType(JustifiedType.JustifiedTypeName.specified_minutes);      
    }
    
    log.debug(LOG_PREFIX + "Sto per inserire una missione per {}. Codice {}, {} - {}, "
        + "tempo {}:{}", person, mission.code, from, to, hours, minutes);
    
    Integer localHours = hours;
    Integer localMinutes = minutes;
  
    //val absenceForm = buildAbsenceForm(person, from.toLocalDate(), to.toLocalDate(), group);

    InsertReport insertReport = 
        absenceService.insert(person, group, from.toLocalDate(), 
            to.toLocalDate(), mission, type, 
            localHours, localMinutes, false, absenceManager);
    if (insertReport.criticalErrors.isEmpty() || insertReport.warningsPreviousVersion.isEmpty()) {
      for (Absence absence : insertReport.absencesToPersist) {
        PersonDay personDay = personDayManager
            .getOrCreateAndPersistPersonDay(person, absence.getAbsenceDate());
        absence.personDay = personDay;
        personDay.absences.add(absence);
        if (idOrdine != null) {
          absence.externalIdentifier = idOrdine;
        } else {
          absence.externalIdentifier = id;
        }
        absence.note = "Missione: " + numero + '\n' 
            + "Anno: " + anno + '\n' 
            + "(Identificativo: " + absence.externalIdentifier + ")";
                
        absence.save();
        
        final Optional<User> currentUser = Security.getUser();
        if (currentUser.isPresent()) {
          notificationManager.notificationAbsencePolicy(currentUser.get(), 
              absence, group, true, false, false);  
        }

        log.info(LOG_PREFIX +  "Inserita assenza {} del {} per {}.", absence.absenceType.code, 
            absence.personDay.date, absence.personDay.person.fullName());

      }
      if (!insertReport.reperibilityShiftDate().isEmpty()) {
        absenceManager
        .sendReperibilityShiftEmail(person, insertReport.reperibilityShiftDate());
        log.info(LOG_PREFIX +  "Inserite assenze con reperibilità e turni {} {}. "
            + "Le email sono disabilitate.",
            person.fullName(), insertReport.reperibilityShiftDate());
      }
      JPA.em().flush();
      return true;
    }

    return false;
  }

  /**
   * Metodo che si occupa di cancellare i singoli giorni di missione.
   * @param missions la lista delle assenze da cancellare
   * @param absenceForm l'absenceForm per notificare la cancellazione dell'assenza
   * @return true se la cancellazione è andata a buon fine, false altrimenti.
   */
  private boolean performDeleteMission(List<Absence> missions, GroupAbsenceType group) {
    boolean result = false;
    if (missions.isEmpty()) {
      result = true;
      return result;
    }
    for (Absence abs : missions) {
      result = atomicRemoval(abs, result);
      final User currentUser = Security.getUser().get();
      notificationManager.notificationAbsencePolicy(currentUser, 
          abs, group, false, false, true);
      log.info(LOG_PREFIX + "Rimossa assenza {} del {} per {}.", 
          abs.absenceType.code, abs.personDay.date, abs.personDay.person.getFullname());

    }
    if (result) {
      return true;
    }
    log.error(LOG_PREFIX + "Errore in rimozione della missione. Giorni di missione: {}.", missions);
    return false;
  }

  /**
   * Metodo che cancella dal personDay l'assenza.
   * @param abs l'assenza (missione) da cancellare
   * @param result se la rimozione precedente è andata a buon fine
   * @return true se l'assenza è stata cancellata, false altrimenti.
   */
  private boolean atomicRemoval(Absence abs, boolean result) {
    try {
      abs.delete();
      abs.personDay.absences.remove(abs);
      abs.personDay.save();
      result = true;
    } catch (Exception ex) {
      result = false;
      throw new PersistenceException("Error in removing absence ");
    }

    return result;
  }

  private AbsenceForm buildAbsenceForm(MissionFromClient body) {
    CategoryTab categoryTab = null;
    GroupAbsenceType groupAbsenceType = null;
    AbsenceType absenceType = null;
    JustifiedType justifiedType = null;
    Integer hours = null;
    Integer minutes = null;
    AbsenceForm absenceForm =
        absenceService.buildAbsenceForm(body.person, body.dataInizio.toLocalDate(), 
            categoryTab, body.dataFine.toLocalDate(), null, 
            groupAbsenceType, false, absenceType, justifiedType, hours, minutes, false, false);
    return absenceForm;
  }

  private AbsenceForm buildAbsenceForm(Person person, LocalDate dataInizio, 
      LocalDate dataFine, GroupAbsenceType group) {
    return absenceService.buildAbsenceForm(person, dataInizio, null, dataFine, null, group, false, 
        null, null, null, null, false, false);
  }

  /**
   * ricalcola la situazione del dipendente.
   * @param body il dto contenente le info dell'ordine/rimborso di missione
   */
  private void recalculate(MissionFromClient body, Optional<List<Absence>> missions) {
    LocalDate begin = body.dataInizio.toLocalDate();
    if (missions.isPresent()) {

      for (Absence abs : missions.get()) {
        if (abs.personDay.date.isBefore(begin)) {
          begin = abs.personDay.date;
        }
      }
    }
    consistencyManager.updatePersonSituation(body.person.id, begin);
  }
  
  /**
   * Metodo privato che ritorna la localDateTime associata alla data passata come parametro
   * da ricercare all'interno del periodo inizio/fine missione del body.
   * @param date la data che si sta cercando
   * @param body l'oggetto mission from client
   * @return il localDateTime relativo alla data passata come parametro calcolato internamente
   *     al periodo di inizio/fine presente nel body.
   */
  private LocalDateTime rightDate(LocalDate date, MissionFromClient body) {
    LocalDateTime actual = body.dataInizio.withHourOfDay(body.dataFine.getHourOfDay())
        .withMinuteOfHour(body.dataFine.getMinuteOfHour())
        .withSecondOfMinute(body.dataFine.getSecondOfMinute());
    
    while (!actual.isAfter(body.dataFine)) {
      if (actual.toLocalDate().isEqual(date)) {
        return actual;
      }
      actual = actual.plusDays(1);
    }
    return null;
  }
  
  /**
   * Metodo che consente l'inserimento di un'assenza per missione sul giorno indicato.
   * @param situation l'oggetto contenente la situazione relativa al giorno di missione
   * @param body l'oggetto dto proveniente dal mission manager
   * @param actualDate la data attuale su cui lavorare
   */
  private void atomicInsert(Situation situation, MissionFromClient body, LocalDateTime actualDate) {
    boolean missionInserted = false;
    
    if (situation.isFirstOrLastDay) {
      if (situation.difference < 0) {
        //sono partito dopo la fine della giornata lavorativa o sono tornato prima dell'inizio 
        //della stessa --> metto un 92M con 1 minuto
        if (insertMission(body.destinazioneMissione, body.person, new Integer(0), new Integer(-1), 
            actualDate, actualDate, body.id, body.idOrdine, body.anno, body.numero)) {
          missionInserted = true;
        } 
      } else {
        if (situation.difference 
            > getFromDayOfMission(body.person, actualDate.toLocalDate()).workingTime) {
          if (insertMission(body.destinazioneMissione, body.person,  
              null, null, actualDate, actualDate, body.id, body.idOrdine, body.anno, body.numero)) {
            missionInserted = true;
          } 
        } else {
          if (insertMission(body.destinazioneMissione, body.person,  
              new Integer(situation.difference / DateTimeConstants.MINUTES_PER_HOUR), 
              new Integer(situation.difference % DateTimeConstants.MINUTES_PER_HOUR), 
              actualDate, actualDate, body.id, body.idOrdine, body.anno, body.numero)) {
            missionInserted = true;
          } 
        }        
      }
    } else {
      if (insertMission(body.destinazioneMissione, body.person,  
          null, null, actualDate, actualDate, body.id, body.idOrdine, body.anno, body.numero)) {
        missionInserted = true;
      } 
    }
    if (missionInserted) {
      log.info("Inserita missione {} per il giorno {}. id = {}, "
          + "idOrdine = {}, anno = {}, numero = {}", body.destinazioneMissione, 
          actualDate, body.id, body.idOrdine, body.anno, body.numero);      
    }
  }
  
  /**
   * Classe privata di aiuto.
   * @author dario     
   */
  private static class Situation {
    private int difference;
    private boolean isFirstOrLastDay;
  }

}
