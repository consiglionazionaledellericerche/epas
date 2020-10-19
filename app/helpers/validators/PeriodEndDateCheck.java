package helpers.validators;

import models.base.PeriodModel;
import play.data.validation.Check;

/**
 * Verifica che la data di fine di un PeriodModel non sia precedente alla
 * data di inizio.
 * 
 * @author cristian
 *
 */
public class PeriodEndDateCheck extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {
    if (!(validatedObject instanceof PeriodModel)) {
      return false;
    }
    final PeriodModel period = (PeriodModel) validatedObject;
    if (period.endDate != null && period.endDate.isBefore(period.beginDate)) {
      setMessage("validation.period.endDateBeforeBeginDate");
      return false;
    }
    return true;
  }

}
