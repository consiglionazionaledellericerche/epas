/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

import common.injection.StaticInject;
import dao.GeneralSettingDao;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import play.data.validation.Check;

/**
 * Controlla che una data non sia troppo lontana da oggi. Usato per l'inserimento
 * delle assenze. 
 * 
 * <p>Quanto può essere lontana la data dell'assenza è controllato da parametro
 * di configurazione maxMonthsInPastForAbsences</p>
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
public class LocalDateNotTooFar extends Check {

  @Inject
  static GeneralSettingDao generalSettingDao;
  
  @Override
  public boolean isSatisfied(Object validatedObject, Object date) {
    if (date == null) {
      return true;
    }
    int maxMonthsInPast = 
        generalSettingDao.generalSetting().getMaxMonthsInPastForAbsences(); 
    setMessage(
        String.format("La data non deve essere distante più di %d mesi nel passato e non più "
            + "di un anno da oggi nel futuro.", maxMonthsInPast));
    return !((LocalDate) date).isAfter(LocalDate.now().plusYears(1))
        && !((LocalDate) date).isBefore(LocalDate.now().minusMonths(maxMonthsInPast));
  }
}
