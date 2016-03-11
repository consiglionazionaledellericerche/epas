package manager.services.vacations;

import lombok.Getter;
import lombok.Setter;

import models.enumerate.VacationCode;

/**
 * I giorni maturati nell'intervallo, tenuto conto dei postPartum, del tipo di richiesta
 * (in accruedState) e del vacationCode.
 * @author alessandro
 *
 */
public class AccruedResultInPeriod extends AccruedResult {

  @Getter @Setter private VacationCode vacationCode;

}