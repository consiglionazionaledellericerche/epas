package manager.vacations;

import it.cnr.iit.epas.DateInterval;

import lombok.Builder;

import models.Absence;
import models.VacationPeriod;

import org.joda.time.LocalDate;

import java.util.List;

public interface IVacationRecap {

  IVacationRecap build(int year, DateInterval contractInterval, 
      List<VacationPeriod> contractVacationPeriods, List<Absence> absencesToConsider,
      LocalDate accruedDate, LocalDate dateExpireLastYear, boolean considerDateExpireLastYear,
      LocalDate dateAsToday);
    
  
  
   
}
