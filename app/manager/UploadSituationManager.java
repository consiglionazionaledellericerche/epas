package manager;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;

import manager.recaps.personStamping.PersonStampingDayRecap;
import manager.recaps.personStamping.PersonStampingDayRecapFactory;

import models.Absence;
import models.Competence;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.exports.StampingFromClient;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class UploadSituationManager {

  
  public static final String FILE_PREFIX = "situazioneMensile";
  public static final String FILE_SUFFIX = ".txt";
  private final PersonDao personDao;
  private AbsenceDao absenceDao;
  private CompetenceDao competenceDao;
  
  /**
   */
  @Inject
  public UploadSituationManager(PersonDao personDao, AbsenceDao absenceDao, 
      CompetenceDao competenceDao) {
    this.personDao = personDao;
    this.absenceDao = absenceDao;
    this.competenceDao = competenceDao;
  }
  
  /**
   * Produce il contenuto del file per attestati circa mese e anno per tutti i dipendenti della
   *  sede passata come paramemtro.
   * @param office office
   * @param year anno 
   * @param month mese 
   * @return contenuto del file
   */
  public String createFile(Office office, Integer year, Integer month) {
    
    List<Person> personList = personDao.getPersonsWithNumber(Optional.of(office));
    
    String body = office.codeId + " " + month + year + "\r\n";
    
    for (Person person : personList) {
      // la parte delle assenze
      List<Absence> absenceList  = absenceDao.getAbsencesNotInternalUseInMonth(person, year, month);
      for (Absence abs : absenceList) {
        body = body + person.number + " A " + abs.absenceType.code + " " 
             + abs.personDay.date.getDayOfMonth() 
             + " " + abs.personDay.date.getDayOfMonth() + " 0\r\n"; 
      }
      //la parte delle competenze
      List<Competence> competenceList = competenceDao
          .getCompetenceInMonthForUploadSituation(person, year, month);
      for (Competence comp : competenceList) {
        body = body + person.number + " C " + comp.competenceCode.code + " " 
            + comp.valueApproved + " 0 0\r\n";
      }
    }
    
    return body;
  }

}
