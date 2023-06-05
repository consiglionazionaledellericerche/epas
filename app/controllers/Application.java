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

import dao.OfficeDao;
import dao.wrapper.IWrapperFactory;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller con i metodi index dell'Applicazione.
 *
 */
@With({Resecure.class})
public class Application extends Controller {

  @Inject
  static OfficeDao officeDao;
  @Inject
  static IWrapperFactory wrapperFactory;

  /**
   * Ritorna l'index per amministratore.
   */
  public static void indexAdmin() {
    Logger.debug("chiamato metodo indexAdmin dell'Application controller");
    render();

  }

  /**
   * Ritorna l'index in base al ruolo.
   */
  public static void index() {

    //Utenti di sistema (developer,admin)
    if (Security.getUser().get().getPerson() == null) {
      if (officeDao.getAllOffices().isEmpty()) {
        //Db vuoto è necessario creare prima gli istituti.
        Institutes.list(null, null, null);
      } else {
        Persons.list(null, null);
      }
      return;
    }

    //inizializzazione functional menu dopo login
    session.put("monthSelected", new LocalDate().getMonthOfYear());
    session.put("yearSelected", new LocalDate().getYear());
    session.put("personSelected", Security.getUser().get().getPerson().id);

    session.put("methodSelected", "stampingsAdmin");
    session.put("actionSelected", "Stampings.stampings");

    Stampings.stampings(new LocalDate().getYear(), new LocalDate().getMonthOfYear());
  }

}