package manager;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import controllers.Security;

import dao.PersonDao;

import lombok.extern.slf4j.Slf4j;

import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;

import models.Person;
import models.User;
import models.exports.MissionFromClient;
import models.exports.StampingFromClient;



@Slf4j
public class MissionManager {
  
  private final PersonDao personDao;
  private final AbsenceService absenceService;
  private final AbsenceManager absenceManager;
  
  @Inject
  public MissionManager(PersonDao personDao, AbsenceService absenceService, 
      AbsenceManager absenceManager) {
    this.personDao = personDao;
    this.absenceService = absenceService;
    this.absenceManager = absenceManager;
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
    
    //TODO: recuperare le info che mancano per chiamare questa funzionalità correttamente
//    InsertReport insertReport = absenceService.insert(person, groupAbsenceType, from, to,
//        absenceType, justifiedType, hours, minutes, forceInsert, absenceManager);
    return false;
  }
}
