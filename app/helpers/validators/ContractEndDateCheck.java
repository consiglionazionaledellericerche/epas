package helpers.validators;

import models.Contract;
import play.data.validation.Check;

/**
 * Verifica che la data di scadenza del contratto non sia precedente alla
 * data di inizio.
 * 
 * @author cristian
 *
 */
public class ContractEndDateCheck extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {
    if (!(validatedObject instanceof Contract)) {
      return false;
    }
    final Contract contract = (Contract) validatedObject;
    if (contract.endDate != null && contract.endDate.isBefore(contract.beginDate)) {
      setMessage("validation.contract.endDateBeforeBeginContract");
      return false;
    }
    return true;
  }

}
