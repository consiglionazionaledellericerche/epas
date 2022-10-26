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

import dao.CompetenceCodeDao;
import helpers.Web;
import java.util.List;
import javax.inject.Inject;
import models.CompetenceCode;
import models.MonthlyCompetenceType;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione dei MonthlyCompetenceType.
 */
@With({Resecure.class})
public class MonthlyCompetenceTypes extends Controller {
  
  @Inject
  private static CompetenceCodeDao competenceCodeDao;

  /**
   * Genera la form di creazione di un monthlyCompetenceType.
   */
  public static void insertMonthlyCompetenceType() {
    
    MonthlyCompetenceType monthlyCompetencetype = new MonthlyCompetenceType();
    List<CompetenceCode> competenceCodeList = competenceCodeDao.getAllCompetenceCode();
        
    render(monthlyCompetencetype, competenceCodeList);
  }
  
  /**
   * Persiste il monthlyCompetenceType.
   *
   * @param monthlyCompetencetype l'oggetto da persistere
   */
  public static void saveType(MonthlyCompetenceType monthlyCompetencetype) {
    
    if (Validation.hasErrors()) {
      response.status = 400;
      flash.error(Web.msgHasErrors());
      render("@insertMonthlyCompetenceType", monthlyCompetencetype);      
    }
    
    monthlyCompetencetype.save();
    flash.success(String.format("Codice %s aggiunto con successo", monthlyCompetencetype.getName()));

    Competences.manageCompetenceCode();
  }
}
