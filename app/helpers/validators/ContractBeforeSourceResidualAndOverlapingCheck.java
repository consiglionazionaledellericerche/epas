package helpers.validators;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.Contract;
import play.data.validation.Check;
import play.data.validation.Validation;

/**
 * Verifica che non ci siano sovrapposizioni tra le date dei contratti
 * di una persona.
 * 
 * @author cristian
 *
 */
@Slf4j
public class ContractBeforeSourceResidualAndOverlapingCheck extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {
    if (!(validatedObject instanceof Contract)) {
      return false;
    }
    final Contract contract = (Contract) validatedObject;
    // sto creando una perosna, non ci sono contratti che si intersecano
    if (contract.person == null || !contract.person.isPersistent()) {
      return true;
    }
    if (contract.person.contracts.stream()
        .filter(c -> !c.id.equals(contract.getId()))
        .anyMatch(con -> con.overlap(contract))) {
      log.debug("Contract {} contract is overlaping");
      val validationMsgKey = "validation.contract.contractOverlaping";
      Validation.addError("contract.beginDate", validationMsgKey);
      setMessage(validationMsgKey);
      return false;
    }

    if (contract.sourceDateResidual != null
        && contract.sourceDateResidual.isBefore(contract.beginDate)) {
      log.debug("Contract.sourceDateResidual {} is before contract.beginDate {}");
      val validationMsgKey = "validation.contract.sourceDateResidualBeforeBeginDate"; 
      Validation.addError("contract.beginDate", validationMsgKey);
      setMessage(validationMsgKey);
      return false;
    }
    return true;
  }

}