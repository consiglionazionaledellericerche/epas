package manager;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.PersonDao;
import dao.PersonMonthRecapDao;
import dao.wrapper.IWrapperContractMonthRecap;

import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;

import models.Absence;
import models.Competence;
import models.Office;
import models.Person;
import models.PersonMonthRecap;

import play.Play;

import java.util.List;

public class UploadSituationManager {

  
  public static final String FILE_PREFIX = "situazioneMensile";
  public static final String FILE_SUFFIX = ".txt";
  public static final String HEADING = "heading";
  private final PersonDao personDao;
  private AbsenceDao absenceDao;
  private CompetenceDao competenceDao;
  private PersonMonthRecapDao personMonthRecapDao;
  private static PersonStampingRecapFactory stampingsRecapFactory;
  
  /**
   */
  @Inject
  public UploadSituationManager(PersonDao personDao, AbsenceDao absenceDao, 
      CompetenceDao competenceDao, PersonMonthRecapDao personMonthRecapDao,
      PersonStampingRecapFactory stampingsRecapFactory) {
    this.personDao = personDao;
    this.absenceDao = absenceDao;
    this.competenceDao = competenceDao;
    this.personMonthRecapDao = personMonthRecapDao;
    this.stampingsRecapFactory = stampingsRecapFactory;
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
    
    String body = Play.configuration.getProperty(HEADING) + "\r\n" 
        + office.codeId + " " + year + " " + month + "\r\n";
    
    for (Person person : personList) {
      // la parte delle assenze
      List<Absence> absenceList  = absenceDao.getAbsencesNotInternalUseInMonth(person, year, month);
      for (Absence abs : absenceList) {
        body = body + person.number + " A " + abs.absenceType.code + " " 
             + abs.personDay.date.getDayOfMonth() 
             + " " + abs.personDay.date.getDayOfMonth() + " \r\n"; 
      }
      //la parte delle competenze
      List<Competence> competenceList = competenceDao
          .getCompetenceInMonthForUploadSituation(person, year, month);
      for (Competence comp : competenceList) {
        body = body + person.number + " C " + comp.competenceCode.code + " " 
            + comp.valueApproved + " \r\n";
      }
      List<PersonMonthRecap> pmrList = personMonthRecapDao.getPersonMonthRecapInYearOrWithMoreDetails
          (person, year, Optional.fromNullable(month),Optional.<Boolean>absent());
      for(PersonMonthRecap pmr : pmrList){
        body = body + person.number + " F " + pmr.fromDate.getDayOfMonth() + " " 
            + pmr.toDate.getDayOfMonth() + " " + pmr.trainingHours + " \r\n";
      }
      PersonStampingRecap psDto = stampingsRecapFactory.create(person, year, month, false);
      for(IWrapperContractMonthRecap cmr : psDto.contractMonths){
        body = body + person.number + " B " + cmr.getValue().buoniPastoUsatiNelMese + " " 
            + cmr.getValue().buoniPastoUsatiNelMese + " \r\n";
      }
    }
    
    return body;
  }

}
