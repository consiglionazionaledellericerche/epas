package manager;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import controllers.Security;

import dao.PersonDao;
import dao.UserDao;

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

import play.db.jpa.JPA;



@Slf4j
public class MissionManager {
  
  private final PersonDao personDao;
  private final AbsenceService absenceService;
  private final AbsenceManager absenceManager;
  private final PersonDayManager personDayManager;
  private final ConsistencyManager consistencyManager;
  private final NotificationManager notificationManager;
  private final UserDao userDao;
  
  @Inject
  public MissionManager(PersonDao personDao, AbsenceService absenceService, 
      AbsenceManager absenceManager, PersonDayManager personDayManager,
      ConsistencyManager consistencyManager, NotificationManager notificationManager,
      UserDao userDao) {
    this.personDao = personDao;
    this.absenceService = absenceService;
    this.absenceManager = absenceManager;
    this.personDayManager = personDayManager;
    this.consistencyManager = consistencyManager;
    this.notificationManager = notificationManager;
    this.userDao = userDao;
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
    
    CategoryTab categoryTab = null;
    GroupAbsenceType groupAbsenceType = null;
    AbsenceType absenceType = null;
    JustifiedType justifiedType = null;
    Integer hours = null;
    Integer minutes = null;
    AbsenceForm absenceForm =
        absenceService.buildAbsenceForm(body.person, body.dataInizio, categoryTab, body.dataFine,
             groupAbsenceType, false, absenceType, justifiedType, hours, minutes, false);
    
    InsertReport insertReport = 
        absenceService.insert(body.person, absenceForm.groupSelected, body.dataInizio, 
            body.dataFine, absenceForm.absenceTypeSelected, absenceForm.justifiedTypeSelected, 
            hours, minutes, false, absenceManager);
    if (insertReport.criticalErrors.isEmpty() || insertReport.warningsPreviousVersion.isEmpty()) {
      for (Absence absence : insertReport.absencesToPersist) {
        PersonDay personDay = personDayManager
            .getOrCreateAndPersistPersonDay(body.person, absence.getAbsenceDate());
        absence.personDay = personDay;
        personDay.absences.add(absence);
        absence.save();
        personDay.save();
        
        final User currentUser = Security.getUser().get();
        notificationManager.notificationAbsencePolicy(currentUser, 
            absence, absenceForm.groupSelected, true);
        
      }
      if (!insertReport.reperibilityShiftDate().isEmpty()) {
        absenceManager
        .sendReperibilityShiftEmail(body.person, insertReport.reperibilityShiftDate());
        log.info("Inserite assenze con reperibilità e turni {} {}. Le email sono disabilitate.",
            body.person.fullName(), insertReport.reperibilityShiftDate());
      }
      JPA.em().flush();
      consistencyManager.updatePersonSituation(body.person.id, body.dataFine);
      return true;
    }
    return false;
  }
}
