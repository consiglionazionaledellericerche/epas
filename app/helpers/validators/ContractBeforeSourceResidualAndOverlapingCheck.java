/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
 * @author Cristian Lucchesi
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
    if (contract.getPerson() == null || !contract.getPerson().isPersistent()) {
      return true;
    }
    if (contract.getPerson().getContracts().stream()
        .filter(c -> !c.id.equals(contract.getId()))
        .anyMatch(con -> con.overlap(contract))) {
      log.debug("Contract {} contract is overlaping");
      val validationMsgKey = "validation.contract.contractOverlaping";
      Validation.addError("contract.beginDate", validationMsgKey);
      setMessage(validationMsgKey);
      return false;
    }

    if (contract.getSourceDateResidual() != null
        && contract.getSourceDateResidual().isBefore(contract.getBeginDate())) {
      log.debug("Contract.sourceDateResidual {} is before contract.beginDate {}");
      val validationMsgKey = "validation.contract.sourceDateResidualBeforeBeginDate"; 
      Validation.addError("contract.beginDate", validationMsgKey);
      setMessage(validationMsgKey);
      return false;
    }
    return true;
  }

}