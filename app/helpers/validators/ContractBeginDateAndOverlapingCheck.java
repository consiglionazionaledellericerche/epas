package helpers.validators;

import lombok.extern.slf4j.Slf4j;
import models.Contract;
import play.data.validation.Check;

/**
 * Verifica che non ci siano sovrapposizioni tra le date dei contratti
 * di una persona.
 * 
 * @author cristian
 *
 */
@Slf4j
public class ContractBeginDateAndOverlapingCheck extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {
    if (!(validatedObject instanceof Contract)) {
      return false;
    }
    final Contract contract = (Contract) validatedObject;
    if (contract.person.contracts.stream()
        .filter(c -> !c.id.equals(contract.getId()))
        .anyMatch(con -> con.overlap(contract))) {
      log.info("Contract {} contract is overlaping");
      setMessage("validation.contract.contractOverlaping");
      return false;
    }
    if (contract.sourceDateResidual != null
        && contract.sourceDateResidual.isBefore(contract.beginDate)) {
      log.info("Contract.sourceDateResidual {} is before contract.beginDate {}");
      setMessage("validation.contract.sourceDateResidualBeforeBeginDate");
      return false;
    }
    return true;
  }

}
