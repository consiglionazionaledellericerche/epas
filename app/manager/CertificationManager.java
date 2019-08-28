package manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.joda.time.YearMonth;
import com.google.common.collect.Lists;
import dao.PersonDayDao;
import it.cnr.iit.epas.DateUtility;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import models.Person;

@Slf4j
public class CertificationManager {
  
  private PersonDayManager personDayManager;
  private PersonDayDao personDayDao;
  
  @Inject
  public CertificationManager(PersonDayManager personDayManager, PersonDayDao personDayDao) {
    this.personDayDao = personDayDao;
    this.personDayManager = personDayManager;
  }
  
  /**
   * 
   * @param people la lista di persone di cui cercare i buoni pasto
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return il file .csv contenente la lista di persone associate alla propria matricola 
   *     e al numero di buoni pasto maturati nell'anno/mese di riferimento
   */
  public File createFile(List<Person> people, int year, int month) {

    File file = new File("Situazione_mensile buoni pasto " 
        + DateUtility.fromIntToStringMonth(month) + "-" + year + ".csv");
    try {
      PrintWriter pw= new PrintWriter(new File(file.getAbsolutePath()));
      StringBuilder sb=new StringBuilder();
      for (Person person : people) {
        sb.append(person.getFullname());
        sb.append(",");
        sb.append(person.number);
        sb.append(",");        
        val mealTicket = personDayManager.numberOfMealTicketToUse(personDayDao
            .getPersonDayInMonth(person, new YearMonth(year, month)));
        sb.append(String.valueOf(mealTicket));
        sb.append("\r\n");
      }
      pw.write(sb.toString());
      pw.close();
    } catch(Exception ex) {
      log.error("Error in CsvFileWriter !!!");
      ex.printStackTrace();
    }
    
    return file;
  }
}
