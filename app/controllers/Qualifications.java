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

package controllers;

import dao.QualificationDao;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.Qualification;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione delle qualifice del personale.
 *
 * @author Cristian Lucchesi
 */
@Slf4j
@With({Resecure.class, RequestInit.class})
public class Qualifications extends Controller {

  @Inject
  private static QualificationDao qualificationDao;
  
  /**
   * End point lista qualifiche.
   */
  public static void list() {
    List<Qualification> qualifications = qualificationDao.findAll();
    render(qualifications);
  }

  /**
   * Form per l'inserimento di una nuova qualifica.
   */
  public static void insert() {
    Qualification qualification = new Qualification();
    qualification.setQualification(qualificationDao.getMaxQualification() + 1);
    render("@edit", qualification);
  }

  public static void edit(Long id) {
    notFoundIfNull(id);
    val qualification = qualificationDao.byId(id).orNull();
    notFoundIfNull(qualification);
    render(qualification);
  }

  /**
   * Inserimento o modifica di una qualifica.
   */
  public static void save(@Valid Qualification qualification) {
    if (Validation.hasErrors()) {
      flash.error("Correggere gli errori indicati");
      render("@edit", qualification);
    }
    qualification.save();
    flash.success("Inserito/modificato qualifica %s - %s", 
        qualification.getQualification(), qualification.getDescription());
    log.info("Inserito/modificato qualifica {} - {}", 
        qualification.getQualification(), qualification.getDescription());
    list();
  }

  /**
   * Cancellazione di una qualifica.
   *
   * <p>Possibile solo se la qualifica non è associata a persone e tipi di assenza.</p>
   */
  public static void delete(Long id) {
    notFoundIfNull(id);
    val qualification = qualificationDao.byId(id).orNull();
    notFoundIfNull(qualification);
    if (!qualification.getPersons().isEmpty()) {
      flash.error("Impossibile eliminare la qualifica %s %s perché associata a %s persone", 
          qualification.getQualification(), qualification.getDescription(), 
          qualification.getPersons().size());
      list();
    }
    if (!qualification.getAbsenceTypes().isEmpty()) {
      flash.error("Impossibile eliminare la qualifica %s %s perché associata a %s tipi di assenze", 
          qualification.getQualification(), qualification.getDescription(), 
          qualification.getAbsenceTypes().size());
      list();
    }
    qualification.delete();
    flash.success("Eliminata correttamente la qualifica %s %s", 
        qualification.getQualification(), qualification.getDescription());
    list();
  }
}
