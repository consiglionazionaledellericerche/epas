package jobs;



import com.google.common.base.Optional;

import dao.CompetenceCodeDao;
import dao.PersonShiftDayDao;

import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.CompetenceManager;

import models.CompetenceCode;
import models.Person;
import models.PersonCompetenceCodes;
import models.PersonReperibility;
import models.PersonShift;
import models.dto.ShiftEvent;

import org.joda.time.LocalDate;

import play.jobs.Job;
import play.jobs.OnApplicationStart;

@Slf4j
@OnApplicationStart(async = true)
public class FixReperibilityShiftDates extends Job<Void> {
  
  @Inject
  static PersonShiftDayDao dao;
  @Inject
  static CompetenceCodeDao codeDao;
  
  static final String T1 = "T1";
  static final String Fer = "207";
  
  
  @Override
  public void doJob() {
    CompetenceCode t1 = codeDao.getCompetenceCodeByCode(T1);
    CompetenceCode fer = codeDao.getCompetenceCodeByCode(Fer);
    
    List<PersonShift> list = PersonShift.findAll();
    for (PersonShift ps : list) {
      Optional<PersonCompetenceCodes> pcc = 
          codeDao.getByPersonAndCodeAndDate(ps.person, t1, LocalDate.now());
      if (pcc.isPresent()) {
        ps.beginDate = pcc.get().beginDate;
        ps.endDate = pcc.get().endDate;
        ps.disabled = false;
        
      } else {
        ps.disabled = true;
        ps.beginDate = LocalDate.now().minusMonths(1).dayOfMonth().withMinimumValue();
        ps.endDate = LocalDate.now().dayOfMonth().withMaximumValue();
      }
      log.info("Aggiornata situazione date di abilitazione ai turni di {}", ps.person.fullName());
      ps.save();
    }
    
    List<PersonReperibility> reperibilities = PersonReperibility.findAll();
    for (PersonReperibility pr : reperibilities) {
      Optional<PersonCompetenceCodes> pcc = 
          codeDao.getByPersonAndCodeAndDate(pr.person, fer, LocalDate.now());
      if (pcc.isPresent() && pr.startDate == null) {
        pr.startDate = pcc.get().beginDate;
      }
      log.info("Aggiornata situazione date di abilitazione alla reperibilità di {}", 
          pr.person.fullName());
      pr.save();
    }
        
  }

}
