package manager.services.vacations;

import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;

import lombok.Data;

import models.absences.Absence;

import java.util.List;

/**
 * I giorni maturati nell'intervallo, tenuto conto dei postPartum e del tipo di richiesta
 * (in vacationsResponse).
 * @author alessandro
 *
 */
@Data
public class AccruedResult {

  private static final int VACATION_UPPER_BOUND = 100;
  
  private VacationsTypeResult vacationsResult;

  private List<AccruedResultInPeriod> accruedResultsInPeriod = Lists.newArrayList();

  private DateInterval interval;

  private List<Absence> postPartum = Lists.newArrayList();
  private int days = 0;
  private int accrued = 0;
  
  private int upperBound = 0;
  private int lowerBound = VACATION_UPPER_BOUND;
  private int fixed = 0;

}