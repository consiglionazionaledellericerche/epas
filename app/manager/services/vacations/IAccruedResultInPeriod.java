package manager.services.vacations;

import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;

import manager.services.vacations.impl.AccruedResultInPeriod;
import manager.services.vacations.impl.VacationsTypeResult;

import models.Absence;
import models.VacationCode;

import java.util.List;

public interface IAccruedResultInPeriod {

  VacationCode getVacationCode();
  
  DateInterval getInterval();

  List<Absence> getPostPartum();
  int getDays();
  int getAccrued();
  int getFixed();
  
}
