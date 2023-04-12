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

/**
 * Manager per la gestione degli straordinari.
 */
public class OvertimesManager {

  private static final Logger log = LoggerFactory.getLogger(OvertimesManager.class);
  private final CompetenceDao competenceDao;

  @Inject
  public OvertimesManager(CompetenceDao competenceDao) {
    this.competenceDao = competenceDao;
  }

  /**
   * Ritorna la tabella contenente le associazioni persona-reason competenza-codice.
   *
   * @param body l'oggetto contenente la lista di persone
   * @param code il codice di competenza
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return la tabella contenente la struttura di persona-reason della competenza-codice
   *        competenza.
   */
  public Table<String, String, Integer> buildMonthForExport(
      PersonsList body, CompetenceCode code, int year, int month) {

    final Table<String, String, Integer> overtimesMonth =
        TreeBasedTable.<String, String, Integer>create();

    for (Person person : body.persons) {
      Optional<Competence> competence = competenceDao.getCompetence(person, year, month, code);
      log.debug("find  Competence {} per person={}, year={}, month={}, competenceCode={}",
          new Object[]{competence, person, year, month, code});

      if ((competence.isPresent()) && (competence.get().getValueApproved() != 0)) {

        overtimesMonth.put(
            person.getSurname() + " " + person.getName(), competence.get().getReason() != null
                ? competence.get().getReason() : "", competence.get().getValueApproved());
        log.debug("Inserita riga person={} reason={} and valueApproved={}",
            new Object[]{person, competence.get().getReason(), 
                competence.get().getValueApproved()});
      }
    }

    return overtimesMonth;
  }

  /**
   * Assegna la quantità di straordinari richiesti nell'anno/mese.
   *
   * @param body la lista dei personsCompetences
   * @param year l'anno 
   * @param month il mese
   */
  public void setRequestedOvertime(PersonsCompetences body, int year, int month) {
    for (Competence competence : body.competences) {
      Optional<Competence> oldCompetence =
          competenceDao.getCompetence(
              competence.getPerson(), year, month, competence.getCompetenceCode());
      if (oldCompetence.isPresent()) {
        // update the requested hours
        oldCompetence.get().setValueApproved(competence.getValueApproved());
        oldCompetence.get().setReason(competence.getReason());
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