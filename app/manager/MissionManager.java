package manager;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import controllers.Security;

import dao.AbsenceDao;
import dao.PersonDao;
import dao.UserDao;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;

import models.Person;
import models.PersonDay;
import models.User;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.CategoryTab;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.exports.MissionFromClient;
import models.exports.StampingFromClient;

import org.joda.time.LocalDate;
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

  @Inject
  public MissionManager(PersonDao personDao, AbsenceService absenceService, 
      AbsenceManager absenceManager, PersonDayManager personDayManager,
      ConsistencyManager consistencyManager, NotificationManager notificationManager,
      AbsenceDao absenceDao) {
    this.personDao = personDao;
    this.absenceService = absenceService;
    this.absenceManager = absenceManager;
    this.personDayManager = personDayManager;
    this.consistencyManager = consistencyManager;
    this.notificationManager = notificationManager;
    this.absenceDao = absenceDao;
  }

    
  /**
   * 
   * @param mission il dto creato dal json arrivato dal listener
   * @return la person, se esiste, associata alla matricola contenuta nel dto.
   */
  public Optional<Person> linkToPerson(MissionFromClient mission) {

    Optional<User> user = Security.getUser();
    if (!user.isPresent()) {
      log.error("Impossibile recuperare l'utente che ha inviato la missione: {}", mission);
      return Optional.absent();
    }

    final Optional<Person> person = Optional.fromNullable(personDao
        .getPersonByNumber(mission.matricola));

    if (person.isPresent()) {
      mission.person = person.get();      
    } else {
      log.warn("Non e' stato possibile recuperare la persona a cui si riferisce la missione,"
          + " matricola={}. Controllare il database.", mission.matricola);
    }

    return person;
  }

  /**
   * 
   * @param body il dto ricavato dal json arrivato dal listener
   * @param recompute se deve essere avviato il ricalcolo
   * @return true se riesce a inserire la missione con i parametri esplicitati nel body, 
   *     false altrimenti.
   */
  public boolean createMissionFromClient(MissionFromClient body, boolean recompute) {


    AbsenceForm absenceForm = buildAbsenceForm(body);
    LocalDate begin = body.dataInizio;
    LocalDate end = body.dataFine;
    if (insertMission(body, absenceForm, null, null, body.dataInizio, body.dataFine)) {
      recalculate(body, Optional.<List<Absence>>absent());
      return true;
    }
    return false;
  }

  /**
   * 
   * @param body il dto contenente i dati della missione
   * @param recompute se deve essere effettuato il ricalcolo
   * @return true se la gestione della missione è andata a buon fine, false altrimenti.
   */
  public boolean manageMissionFromClient(MissionFromClient body, boolean recompute) {
    if (body.idOrdine == null) {
      return false;
    }
    List<Absence> missions = absenceDao.absencesPersistedByMissions(body.idOrdine);
    if (missions.isEmpty()) {
      return false;
    }
    AbsenceForm absenceForm = buildAbsenceForm(body);
    
    List<LocalDate> dates = Lists.newArrayList();
    LocalDate current = body.dataInizio;
    while (!current.isAfter(body.dataFine)) {
      dates.add(current);
      current = current.plusDays(1);
    }
    List<Absence> toRemove = missions.stream()
        .filter(abs -> !dates.contains(abs.personDay.date)).collect(Collectors.toList());
    List<LocalDate> missionsDate = missions.stream()
        .map(a -> a.personDay.date).collect(Collectors.toList());
    List<LocalDate> toAdd = dates.stream()
        .filter(p -> !missionsDate.contains(p)).collect(Collectors.toList());        

    for (Absence abs : toRemove) {
      abs.delete();
      final User currentUser = Security.getUser().get();
      notificationManager.notificationAbsencePolicy(currentUser, 
          abs, absenceForm.groupSelected, false, true, false);
      log.debug("rimossa assenza {} del {}", abs.absenceType.code, abs.personDay.date);
    }
    
    boolean result = false;
    for (LocalDate date : toAdd) {
      result = insertMission(body, absenceForm, null, null, date, date);
    }
    recalculate(body, Optional.fromNullable(missions));
    if (result) {
      return true;
    }
    return false;
  }
  
  
  private boolean insertMission(MissionFromClient body, AbsenceForm absenceForm, 
      Integer hours, Integer minutes, LocalDate from, LocalDate to) {
    InsertReport insertReport = 
        absenceService.insert(body.person, absenceForm.groupSelected, from, 
            to, absenceForm.absenceTypeSelected, absenceForm.justifiedTypeSelected, 
            hours, minutes, false, absenceManager);
    if (insertReport.criticalErrors.isEmpty() || insertReport.warningsPreviousVersion.isEmpty()) {
      for (Absence absence : insertReport.absencesToPersist) {
        PersonDay personDay = personDayManager
            .getOrCreateAndPersistPersonDay(body.person, absence.getAbsenceDate());
        absence.personDay = personDay;
        if (body.idOrdine != null) {
          absence.externalIdentifier = body.idOrdine;
        } else {
          absence.externalIdentifier = body.id;
        }        
        personDay.absences.add(absence);
        absence.save();
        personDay.save();

        final User currentUser = Security.getUser().get();
        notificationManager.notificationAbsencePolicy(currentUser, 
            absence, absenceForm.groupSelected, true, false, false);

      }
      if (!insertReport.reperibilityShiftDate().isEmpty()) {
        absenceManager
        .sendReperibilityShiftEmail(body.person, insertReport.reperibilityShiftDate());
        log.info("Inserite assenze con reperibilità e turni {} {}. Le email sono disabilitate.",
            body.person.fullName(), insertReport.reperibilityShiftDate());
      }
      JPA.em().flush();
      return true;
    }
    
    return false;
  }
  
  private AbsenceForm buildAbsenceForm(MissionFromClient body) {
    CategoryTab categoryTab = null;
    GroupAbsenceType groupAbsenceType = null;
    AbsenceType absenceType = null;
    JustifiedType justifiedType = null;
    Integer hours = null;
    Integer minutes = null;
    AbsenceForm absenceForm =
        absenceService.buildAbsenceForm(body.person, body.dataInizio, categoryTab, body.dataFine,
            groupAbsenceType, false, absenceType, justifiedType, hours, minutes, false);
    return absenceForm;
  }
  
  /**
   * ricalcola la situazione del dipendente.
   * @param body il dto contenente le info dell'ordine/rimborso di missione
   */
  private void recalculate(MissionFromClient body, Optional<List<Absence>> missions) {
    LocalDate begin = body.dataInizio;
    if (missions.isPresent()) {
      
      for (Absence abs : missions.get()) {
        if (abs.personDay.date.isBefore(begin)) {
          begin = abs.personDay.date;
        }
      }
    }
    consistencyManager.updatePersonSituation(body.person.id, begin);
  }
}
