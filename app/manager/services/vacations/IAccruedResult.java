package manager.services.vacations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;

import lombok.Getter;

import manager.services.vacations.impl.AccruedConverter;
import manager.services.vacations.impl.AccruedResult;
import manager.services.vacations.impl.AccruedResultInPeriod;
import manager.services.vacations.impl.VacationsRecap;
import manager.services.vacations.impl.VacationsTypeResult;
import manager.services.vacations.impl.VacationsRecap.VacationsRequest;
import manager.services.vacations.impl.VacationsTypeResult.TypeVacation;

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
