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

import common.security.SecurityRules;
import controllers.Resecure.BasicAuth;
import helpers.JsonResponse;
import it.cnr.iit.epas.JsonStampingBinder;
import javax.inject.Inject;
import lombok.val;
import manager.StampingManager;
import models.exports.StampingFromClient;
import play.data.binding.As;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;


/**
 * Controller per la ricezione delle timbrature via JSON dai client REST.
 */
@With(Resecure.class)
public class StampingsFromClient extends Controller {

  @Inject
  static SecurityRules rules;
  @Inject
  static StampingManager stampingManager;

  /**
   * Aggiunge una timbratura ad una persona.
   */
  @BasicAuth
  public static void create(@As(binder = JsonStampingBinder.class) StampingFromClient body) {

    // Malformed Json (400)
    if (body == null) {
      JsonResponse.badRequest();
    }

    // Badge number not present (404)
    if (!stampingManager.linkToPerson(body).isPresent()) {
      JsonResponse.notFound();
    }

    // Controllo timbratura con data troppo vecchia o nel futuro
    checkDateTime(body);

    // Stamping already present (409)
    if (!stampingManager.createStampingFromClient(body, true).isPresent()) {
      JsonResponse.conflict();
    }

    // Success (200)
    JsonResponse.ok();
  }

  /**
   * Inserimento timbratura senza ricalcolo.
   */
  @BasicAuth
  public static void createNotRecompute(@As(binder = JsonStampingBinder.class)
      StampingFromClient body) {

    // Malformed Json (400)
    if (body == null) {
      JsonResponse.badRequest();
    }

    // Badge number not present (404)
    if (!stampingManager.linkToPerson(body).isPresent()) {
      JsonResponse.badRequest();
    }

    checkDateTime(body);

    // Stamping already present (409)
    if (!stampingManager.createStampingFromClient(body, false).isPresent()) {
      JsonResponse.conflict();
    }

    // Success (200)
    JsonResponse.ok();
  }


  /**
   * Risponde 400 se la data della timbratura è troppo nel passato o nel futuro.
   */
  @Util
  private static void checkDateTime(StampingFromClient body) {
    val error = stampingManager.clientDateTimeError(body.dateTime, body.numeroBadge);
    if (error.isPresent()) {
      JsonResponse.badRequest(error.get());
    }
  }
}
