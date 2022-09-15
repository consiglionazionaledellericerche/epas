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
@OnApplicationStart
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
        .filter(ab -> !ab.personDay.date.isBefore(new LocalDate(2022, 8, 13)))
        .collect(Collectors.toList());
    for (Absence abs : childrenCodesFiltered) {
      switch (abs.absenceType.code) {
        case "25O":
          break;
        case "252O":
          break;
        case "253O":
          break;
        case "254O":
          break;
        case "25MO":
          break;
        case "252MO":
          break;
        case "253MO":
          break;
        case "254MO":
          break;
        case "25OH7":
          break;
        case "252OH7":
          break;
        case "253OH7":
          break;
        case "254OH7":
          break;
        default:
          break;
      }
    }
    
    log.debug("End Job parental codes fix");
    
  }
}
