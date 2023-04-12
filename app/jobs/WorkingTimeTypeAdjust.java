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

package jobs;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import models.WorkingTimeType;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Correzioni sulle tipologie di orario di lavoro legate
 * alla maternità / allattamento.
 */
@OnApplicationStart(async = true)
@Slf4j
public class WorkingTimeTypeAdjust extends Job<Void> {

  private final String maternita = "Maternita";
  private final String maternitaAccento = "Maternità";
  private final String maternitaLowerCase = "maternita";
  private final String maternitaLowerCaseAccento = "maternità";
  private final String allattamento = "Allattamento";
  private final String allattamentoLowerCase = "allattamento";
  
  @Override
  public void doJob() {
    List<WorkingTimeType> wttList = WorkingTimeType.findAll();
    for (WorkingTimeType wtt : wttList) {
      if ((wtt.getDescription().contains(maternita) 
          || wtt.getDescription().contains(maternitaAccento) 
          || wtt.getDescription().contains(maternitaLowerCase) 
          || wtt.getDescription().contains(maternitaLowerCaseAccento)
          || wtt.getDescription().contains(allattamento)
          || wtt.getDescription().contains(allattamentoLowerCase)) 
          && wtt.isEnableAdjustmentForQuantity() == true) {
        wtt.setEnableAdjustmentForQuantity(false);
        log.info("Messo a false il campo enableAdjustmentForQuantity per l'orario {} "
            + "della sede {}", wtt.getDescription(), wtt.getOffice());
      }
      wtt.save();
    }
  }
}