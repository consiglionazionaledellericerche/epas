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
