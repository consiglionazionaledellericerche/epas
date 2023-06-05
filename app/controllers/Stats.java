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

package controllers;

import com.google.common.collect.Sets;
import dao.InstituteDao;
import dao.OfficeDao;
import dao.PersonDao;
import java.io.IOException;
import javax.inject.Inject;
import lombok.val;
import manager.StatsManager;
import org.joda.time.YearMonth;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la generazione delle statistiche di utilizzo dell'applicazione.
 *
 * @author Cristian Lucchesi
 *
 */
@With(Resecure.class)
public class Stats extends Controller {
 
  @Inject
  static InstituteDao instituteDao;
  
  @Inject
  static OfficeDao officeDao;
  
  @Inject
  static PersonDao personDao;
  
  @Inject
  static StatsManager statsManager;
  
  /**
   * Statistiche generali su utilizzatori di ePAS.
   */
  public static void general() {
    
    val institutes = statsManager.getInstitutes();

    val departments = statsManager.getDepartments();
    
    val offices = statsManager.getAllOffices();
    val numberOfOffices = offices.size();
    
    val headQuarterOffices = statsManager.getHeadQuarterOffices();

    val numberOfHeadQuarterPersons = 
        personDao.getActivePersonInMonth(headQuarterOffices, YearMonth.now()).size();
    
    val numberOfPersons = personDao.getActivePersonInMonth(
        Sets.newHashSet(), YearMonth.now()).size();
    
    val numberOfTechnicians = personDao.getActiveTechnicianInMonth(
        Sets.newHashSet(), YearMonth.now()).size();
    
    val numberOfTopLevels = numberOfPersons - numberOfTechnicians;
    
    render(institutes, departments, headQuarterOffices, 
        numberOfOffices, numberOfPersons, numberOfHeadQuarterPersons,
        numberOfTechnicians, numberOfTopLevels);
  }
  
  /**
   * Esportazione in Excel delle statistiche di utilizzo ePAS.
   */
  public static void exportStats() throws IOException {
    val file = statsManager.createFileXlsToExport(); 
    renderBinary(file);
  }
  
}
