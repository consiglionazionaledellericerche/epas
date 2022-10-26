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

import models.Contract;
import play.data.validation.Check;

/**
 * Verifica che la data di terminazione del contratto non sia precedente ne alla
 * data di inizio ne alla data di scadenza del contratto.
 *
 * @author Cristian Lucchesi
 *
 */
public class ContractEndContractCheck extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {
    if (!(validatedObject instanceof Contract)) {
      return false;
    }
    final Contract contract = (Contract) validatedObject;
    if (contract.getEndContract() != null && contract.getEndContract().isBefore(contract.getBeginDate())) {
      setMessage("validation.contract.endContractBeforeBeginContract");
      return false;
    }
    if (contract.getEndDate() != null && contract.getEndContract() != null 
        && contract.getEndContract().isAfter(contract.getEndDate())) {
      setMessage("validation.contract.endContractBeforeEndDate");
      return false;      
    }
    return true;
  }

}
