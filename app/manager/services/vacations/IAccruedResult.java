package manager.services.vacations;

import it.cnr.iit.epas.DateInterval;

import manager.services.vacations.impl.VacationsTypeResult;

import models.Absence;

import java.util.List;

public interface IAccruedResult {

  VacationsTypeResult getVacationsResult();
  //List<PeriodAccruedResult> childDecisions = Lists.newArrayList();;
  
  DateInterval getInterval();

  List<Absence> getPostPartum();
  int getDays();
  int getAccrued();
  int getFixed();
  
}
