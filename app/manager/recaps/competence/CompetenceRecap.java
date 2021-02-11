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

package manager.recaps.competence;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dao.CompetenceDao;
import dao.PersonDao;
import java.util.List;
import manager.CompetenceManager;
import models.Competence;
import models.CompetenceCode;
import models.Office;
import models.Person;
import models.TotalOvertime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

/**
 * DTO per riepilogo Competence.
 */
public class CompetenceRecap {

  public int month;
  public int year;
  public Office office;
  public CompetenceCode competenceCode;

  public int totaleOreStraordinarioMensile = 0;
  public int totaleOreStraordinarioAnnuale = 0;
  public int totaleMonteOre = 0;

  List<CompetenceCode> competenceCodeList = Lists.newArrayList();
  List<Competence> compList = Lists.newArrayList();

  /**
   * Costruttore competenceRecap.
   *
   * @param personDao il dao sulla persona
   * @param competenceManager il manager sulle competnze
   * @param competenceDao il dao sulle competenze
   * @param year l'anno
   * @param month il mese
   * @param office l'ufficio
   * @param competenceCode il codice di competenza
   */
  public CompetenceRecap(PersonDao personDao, CompetenceManager competenceManager,
      CompetenceDao competenceDao, int year, int month, Office office,
      CompetenceCode competenceCode) {
    this.month = month;
    this.year = year;
    this.office = office;
    this.competenceCode = competenceCode;

    List<Person> personList = personDao
        .listForCompetence(Sets.newHashSet(office), new YearMonth(year, month), competenceCode);
    this.compList = competenceManager
        .createCompetenceList(personList, new LocalDate(year, month, 1), competenceCode);

    List<String> code = competenceManager.populateListWithOvertimeCodes();

    List<Competence> competenceList = competenceDao
        .getCompetencesInOffice(year, month, code, office, false);
    this.totaleOreStraordinarioMensile = competenceManager.getTotalMonthlyOvertime(competenceList);

    List<Competence> competenceYearList = competenceDao
        .getCompetencesInOffice(year, month, code, office, true);
    this.totaleOreStraordinarioAnnuale = competenceManager
        .getTotalYearlyOvertime(competenceYearList);

    List<TotalOvertime> total = competenceDao.getTotalOvertime(year, office);
    this.totaleMonteOre = competenceManager.getTotalOvertime(total);
  }
}
