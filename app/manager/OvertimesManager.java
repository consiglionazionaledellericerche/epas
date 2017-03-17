package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dao.CompetenceDao;

import javax.inject.Inject;

import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonHourForOvertime;
import models.exports.PersonsCompetences;
import models.exports.PersonsList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvertimesManager {

  private static final Logger log = LoggerFactory.getLogger(OvertimesManager.class);
  private final CompetenceDao competenceDao;

  @Inject
  public OvertimesManager(CompetenceDao competenceDao) {
    this.competenceDao = competenceDao;
  }

  /**
   * @return la tabella contenente la struttura di persona-reason della 
   *        competenza-codice competenza.
   */
  public Table<String, String, Integer> buildMonthForExport(
      PersonsList body, CompetenceCode code, int year, int month) {

    final Table<String, String, Integer> overtimesMonth =
        TreeBasedTable.<String, String, Integer>create();

    for (Person person : body.persons) {
      Optional<Competence> competence = competenceDao.getCompetence(person, year, month, code);
      log.debug("find  Competence {} per person={}, year={}, month={}, competenceCode={}",
          new Object[]{competence, person, year, month, code});

      if ((competence.isPresent()) && (competence.get().valueApproved != 0)) {

        overtimesMonth.put(
            person.surname + " " + person.name, competence.get().reason != null
            ? competence.get().reason : "", competence.get().valueApproved);
        log.debug("Inserita riga person={} reason={} and valueApproved={}",
            new Object[]{person, competence.get().reason, competence.get().valueApproved});
      }
    }

    return overtimesMonth;
  }


  public void setRequestedOvertime(PersonsCompetences body, int year, int month) {
    for (Competence competence : body.competences) {
      Optional<Competence> oldCompetence =
          competenceDao.getCompetence(competence.person, year, month, competence.competenceCode);
      if (oldCompetence.isPresent()) {
        // update the requested hours
        oldCompetence.get().setValueApproved(competence.getValueApproved(), competence.getReason());
        oldCompetence.get().save();

        log.debug("Aggiornata competenza {}", oldCompetence);
      } else {
        // insert a new competence with the requested hours an reason
        competence.setYear(year);
        competence.setMonth(month);
        competence.save();

        log.debug("Creata competenza {}", competence);
      }
    }
  }

  /**
   * Imposta il quantitativo orario (hours) a disposizione del responsabile (person).
   */
  public void setSupervisorOvertime(Person person, Integer hours) {
    PersonHourForOvertime personHourForOvertime = competenceDao.getPersonHourForOvertime(person);
    if (personHourForOvertime == null) {
      personHourForOvertime = new PersonHourForOvertime(person, hours);
      log.debug("Created PersonHourForOvertime with persons {} and  hours={}",
          person.getFullname(), hours);
    } else {
      personHourForOvertime.setNumberOfHourForOvertime(hours);
      log.debug("Updated PersonHourForOvertime of persons {} with hours={}",
          person.getFullname(), hours);
    }
    personHourForOvertime.save();
  }
}
