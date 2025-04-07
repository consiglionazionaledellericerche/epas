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

import com.google.common.base.Optional;
import common.security.SecurityRules;
import dao.AffiliationDao;
import dao.GroupDao;
import dao.PersonDao;
import helpers.Web;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.flows.Affiliation;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione delle affiliazione delle persone ai gruppi.
 *
 * @author Cristian Lucchesi
 *
 */
@Slf4j
@With(Resecure.class)
public class Affiliations extends Controller {

  @Inject 
  static PersonDao personDao;
  @Inject
  static AffiliationDao affiliationDao;
  @Inject
  static GroupDao groupDao;
  @Inject
  static SecurityRules rules;  

  /**
   * Tutte le appartenenze ai gruppi di una persona.
   *
   * @param personId l'id della persona di cui mostrare
   *     le appartenenze ai gruppi.
   */
  public static void personAffiliations(Long personId) {
    val person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    render(person);
  }

  /**
   * Form per la creazione di una nuova associazione tra
   * persona e gruppo.
   */
  public static void blankByPerson(Long personId) {
    val person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    val activeGroups = 
        groupDao.groupsByOffice(person.getCurrentOffice().get(), Optional.absent(), 
            Optional.of(false));

    val affiliation = new Affiliation();
    affiliation.setPerson(person);
    render(affiliation, person, activeGroups);
  }

  /**
   * Inserimento/Modifica di un'affiliazione.
   */
  public static void save(@Valid @Required Affiliation affiliation) {
    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors: {}", validation.errorsMap());
      val activeGroups = 
          groupDao.groupsByOffice(
              affiliation.getPerson().getCurrentOffice().get(), Optional.absent(), 
              Optional.of(false));      

      render("@blankByPerson", affiliation, activeGroups);
    }

    rules.checkIfPermitted(affiliation.getPerson().getCurrentOffice().get());

    affiliation.save();

    log.info("Aggiunta/Modificata affiliazione di {} al gruppo {}", 
        affiliation.getPerson().getFullname(), affiliation.getGroup().getName());
    flash.success(Web.msgSaved(Affiliation.class));

    personAffiliations(affiliation.getPerson().id);
  }

  /**
   * Cancellazione di una associazione ad un gruppo.
   */
  public static void delete(Long id, boolean confirmed) {
    notFoundIfNull(id);
    val affiliation = affiliationDao.byId(id).orElse(null);
    notFoundIfNull(affiliation);
    val person = affiliation.getPerson();
    if (!confirmed) {      
      render("@delete", affiliation, person);
    }
    affiliation.delete();
    flash.success("Eliminata associazione di %s al gruppo %s", 
        person.getFullname(), affiliation.getGroup().getName());
    personAffiliations(person.id);
  }

  /**
   * Form modifica dei dati di una affiliazione.
   */
  public static void edit(Long id) {
    notFoundIfNull(id);
    val affiliation = affiliationDao.byId(id).orElse(null);
    notFoundIfNull(affiliation);
    val person = affiliation.getPerson();
    val activeGroups = 
        groupDao.groupsByOffice(person.getCurrentOffice().get(), Optional.absent(), 
            Optional.of(false));

    render("@blankByPerson", affiliation, activeGroups);
  }
}