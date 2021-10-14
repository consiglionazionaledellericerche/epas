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

package controllers;

import dao.CheckGreenPassDao;
import dao.OfficeDao;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.CheckGreenPass;
import models.Office;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With({Resecure.class})
public class CheckGreenPasses extends Controller {
  
  @Inject
  static OfficeDao officeDao;
  @Inject
  static SecurityRules rules;
  @Inject
  static CheckGreenPassDao passDao;

  /**
   * Ritorna la lista dei sorteggiati per il check del green pass.
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @param day il giorno di riferimento
   * @param officeId l'identificativo della sede
   */
  public static void dailySituation(final Integer year, final Integer month,
      final Integer day, final Long officeId) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    LocalDate date;
    if (year == null || month == null || day == null) {
      date = LocalDate.now();
    } else {
      date = new LocalDate(year, month, day);
    }
    List<CheckGreenPass> list = passDao.listByDate(date, office);
    render(list, office, date);
  }
  
  public static void addPerson() {
    render();
  }
  
  public static void deletePerson(long personId) {
    render();
  }
  
  public static void checkPerson(long personId) {
    render();
  }
}
