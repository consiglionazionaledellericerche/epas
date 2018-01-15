package jobs;



import com.google.common.base.Optional;

import dao.CompetenceCodeDao;
import dao.PersonShiftDayDao;

import lombok.extern.slf4j.Slf4j;

import java.util.List;


import javax.inject.Inject;

import manager.CompetenceManager;

import models.CompetenceCode;
import models.Person;
import models.PersonCompetenceCodes;
import models.PersonShift;
import models.dto.ShiftEvent;

import org.joda.time.LocalDate;

import play.jobs.Job;
import play.jobs.OnApplicationStart;

@Slf4j
@OnApplicationStart(async = true)
public class FixPersonShiftDates extends Job<Void> {
  
  @Inject
  static PersonShiftDayDao dao;
  @Inject
  static CompetenceCodeDao codeDao;
  
  static final String T1 = "T1";
  static final String T2 = "T2";
  static final String T3 = "T3";
  
  @Override
  public void doJob() {
    CompetenceCode t1 = codeDao.getCompetenceCodeByCode(T1);
//    CompetenceCode t2 = codeDao.getCompetenceCodeByCode(T2);
//    CompetenceCode t3 = codeDao.getCompetenceCodeByCode(T3);
    
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
        
  }

}
