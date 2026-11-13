/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

import common.security.SecurityRules;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonOfficeDao;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.PersonOfficeManager;
import models.Office;
import models.PersonOffice;
import org.joda.time.LocalDate;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Gestione delle afferenze periodicizzate delle persone alle sedi.
 * Le azioni sono riservate agli utenti con ruolo di sistema ADMIN o DEVELOPER: le afferenze
 * sono destinate ad essere alimentate automaticamente da sistemi esterni.
 */
@Slf4j
@With(Resecure.class)
public class PersonOffices extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static PersonOfficeDao personOfficeDao;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static PersonOfficeManager personOfficeManager;
  @Inject
  static SecurityRules rules;

  /**
   * Le afferenze della persona.
   */
  public static void personOffices(Long personId) {
    val person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted();
    val personOffices = personOfficeDao.byPerson(person);
    render(person, personOffices);
  }

  /**
   * Form per il trasferimento della persona in una nuova sede.
   */
  public static void blank(Long personId) {
    val person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted();
    val offices = officeDao.allEnabledOffices();
    render(person, offices);
  }

  /**
   * Form di modifica di un'afferenza.
   */
  public static void edit(Long id) {
    val personOffice = personOfficeDao.getById(id);
    notFoundIfNull(personOffice);
    rules.checkIfPermitted();
    val person = personOffice.getPerson();
    val offices = officeDao.allEnabledOffices();
    render("@blank", personOffice, person, offices);
  }

  /**
   * Inserisce un trasferimento (id nullo) o modifica un'afferenza esistente.
   */
  public static void save(Long id, @Required Long personId, @Required Office office,
      @Required LocalDate beginDate, LocalDate endDate) {
    rules.checkIfPermitted();
    val person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    val personOffice = id == null ? null : personOfficeDao.getById(id);
    if (id != null) {
      notFoundIfNull(personOffice);
    }
    if (endDate != null && beginDate != null && endDate.isBefore(beginDate)) {
      Validation.addError("endDate", "Deve essere successiva alla data di inizio");
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      val offices = officeDao.allEnabledOffices();
      render("@blank", personOffice, person, offices, office, beginDate, endDate);
    }

    if (personOffice == null) {
      final List<String> messages =
          personOfficeManager.changeOffice(person, office, beginDate).getMessages();
      flash.success("Trasferimento di %s nella sede %s dal %s inserito.%s", person.getFullname(),
          office.getName(), beginDate.toString("dd/MM/yyyy"),
          messages.isEmpty() ? "" : " " + String.join(". ", messages) + ".");
    } else {
      final List<String> errors =
          personOfficeManager.update(personOffice, office, beginDate, endDate);
      if (!errors.isEmpty()) {
        flash.error(String.join(". ", errors));
        personOffices(person.id);
      }
      flash.success("Afferenza modificata");
    }
    personOffices(person.id);
  }

  /**
   * Eliminazione di un'afferenza, previa conferma.
   */
  public static void delete(Long id, boolean confirmed) {
    val personOffice = personOfficeDao.getById(id);
    notFoundIfNull(personOffice);
    rules.checkIfPermitted();
    val person = personOffice.getPerson();
    if (!confirmed || !"POST".equals(request.method)) {
      render(personOffice, person);
    }
    final List<String> errors = personOfficeManager.delete(personOffice);
    if (errors.isEmpty()) {
      flash.success("Eliminata afferenza di %s alla sede %s", person.getFullname(),
          personOffice.getOffice().getName());
    } else {
      flash.error(String.join(". ", errors));
    }
    personOffices(person.id);
  }
}
