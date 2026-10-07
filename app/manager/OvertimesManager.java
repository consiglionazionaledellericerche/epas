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
import com.google.common.collect.Lists;
import dao.CompetenceDao;
import java.util.List;
import javax.inject.Inject;
import models.CompetenceCode;
import models.Person;
import models.dto.PersonOvertimeSummary;
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
   * Ritorna una lista di dto con le informazioni per le ore di straordinario.
   * 
   * @param personList la lista delle persone per cui recuperare la situazione delle ore di straordinario
   * @param year l'anno di riferimento
   * @param codeList la lista dei codici di competenza per cui ricercare le ore disponibili
   * @return una lista di dto che contengono la situazione delle ore di straordinario dei dipendenti richiesti.
   */
  public List<PersonOvertimeSummary> generatePeopleOvertimeSummary(List<Person> personList, int year, List<CompetenceCode> codeList) {
    List<PersonOvertimeSummary> list = Lists.newArrayList();
    for (Person person : personList) {
      PersonOvertimeSummary pos = new PersonOvertimeSummary();
      pos.setPerson(person);
      pos.setRemainingHours(person.totalOvertimeHourInYear(year) - 
          competenceDao.valueOvertimeApprovedByMonthAndYear(year, Optional.absent(), 
              Optional.fromNullable(person), Optional.absent(), codeList).or(0));
      pos.setAssignedHours(person.totalOvertimeHourInYear(year));
      list.add(pos);
    }
    return list;
  }
}