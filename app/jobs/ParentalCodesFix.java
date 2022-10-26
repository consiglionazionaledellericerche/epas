package jobs;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import dao.absences.AbsenceComponentDao;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import models.absences.Absence;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@Slf4j
@OnApplicationStart(async = true)
public class ParentalCodesFix extends Job {
  
  @Inject
  static AbsenceComponentDao absenceComponentDao;
  
  @Override
  public void doJob() {
    
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.info("Start Job parental codes fix");
    List<String> codes = ImmutableList.of("25O", "252O", "253O", "254O", "25MO", "252MO", "253MO", 
        "254MO", "25OH7", "252OH7", "253OH7", "254OH7");
    List<Absence> allChildrenCodes = absenceComponentDao.absences(codes);
    List<Absence> childrenCodesFiltered = allChildrenCodes.stream()
        .filter(ab -> !ab.getPersonDay().getDate().isBefore(new LocalDate(2022, 8, 13)))
        .collect(Collectors.toList());
    log.debug("Ci sono {} assenze da modificare", childrenCodesFiltered.size() );
    for (Absence abs : childrenCodesFiltered) {
      switch (abs.getAbsenceType().getCode()) {
        case "25O":
          abs.setAbsenceType(absenceComponentDao.absenceTypeByCode("25").get());          
          break;
        case "252O":
          abs.setAbsenceType(absenceComponentDao.absenceTypeByCode("252").get());
          break;
        case "253O":
          abs.setAbsenceType(absenceComponentDao.absenceTypeByCode("253").get());
          break;
        case "254O":
          abs.setAbsenceType(absenceComponentDao.absenceTypeByCode("254").get());
          break;
        case "25MO":
          abs.setAbsenceType(absenceComponentDao.absenceTypeByCode("25M").get());
          break;
        case "252MO":
          abs.setAbsenceType(absenceComponentDao.absenceTypeByCode("252M").get());
          break;
        case "253MO":
          abs.setAbsenceType(absenceComponentDao.absenceTypeByCode("253M").get());
          break;
        case "254MO":
          abs.setAbsenceType(absenceComponentDao.absenceTypeByCode("254M").get());
          break;
        case "25OH7":
          abs.setAbsenceType(absenceComponentDao.absenceTypeByCode("25H7").get());
          break;
        case "252OH7":
          abs.setAbsenceType(absenceComponentDao.absenceTypeByCode("252H7").get());
          break;
        case "253OH7":
          abs.setAbsenceType(absenceComponentDao.absenceTypeByCode("253H7").get());
          break;
        case "254OH7":
          abs.setAbsenceType(absenceComponentDao.absenceTypeByCode("254H7").get());
          break;          
        default:
          break;          
      }
      abs.save();
    }
    
    log.info("End Job parental codes fix");
    
  }
}
