package helpers.validators;

import models.Contract;
import play.data.validation.Check;

/**
 * Verifica che la data di terminazione del contratto non sia precedente ne alla
 * data di inizio ne alla data di scadenza del contratto.
 * 
 * @author cristian
 *
 */
public class ContractEndContractCheck extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {
    if (!(validatedObject instanceof Contract)) {
      return false;
    }
    final Contract contract = (Contract) validatedObject;
    if (contract.endContract != null && contract.endContract.isBefore(contract.beginDate)) {
      setMessage("validation.contract.endContractBeforeBeginContract");
      return false;
    }
    if (contract.endDate != null && contract.endContract != null 
        && contract.endContract.isAfter(contract.endDate)) {
      setMessage("validation.contract.endContractBeforeEndDate");
      return false;      
    }
    return true;
  }

}
