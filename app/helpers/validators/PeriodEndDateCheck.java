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

import models.base.PeriodModel;
import play.data.validation.Check;

/**
 * Verifica che la data di fine di un PeriodModel non sia precedente alla
 * data di inizio.
 *
 * @author Cristian Lucchesi
 *
 */
public class PeriodEndDateCheck extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {
    if (!(validatedObject instanceof PeriodModel)) {
      return false;
    }
    final PeriodModel period = (PeriodModel) validatedObject;
    if (period.getEndDate() != null && period.getEndDate().isBefore(period.getBeginDate())) {
      setMessage("validation.period.endDateBeforeBeginDate");
      return false;
    }
    return true;
  }

}
