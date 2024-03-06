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

package controllers.rest.v3;

import cnr.sync.dto.v3.BadgeReaderShowDto;
import cnr.sync.dto.v3.BadgeReaderShowTerseDto;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import dao.BadgeReaderDao;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import models.BadgeReader;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;

/**
 * API Rest per la gestione dei lettori badge.
 *
 * @author Cristian Lucchesi
 * @since version 3
 */
@With(Resecure.class)
public class BadgeReaders extends Controller {

  @Inject
  static BadgeReaderDao badgeReaderDao;

  @Inject
  static SecurityRules rules;

  @Inject
  static GsonBuilder gsonBuilder;

  /**
   * Metodo Rest che ritorna il Json con la lista dei lettori badge
   * associati ad un ufficio. L'ufficio è individuato con le chiavi dell'ufficio
   * coerentemente con gli altri metodi REST relativi agli uffici.
   */
  public static void byOffice(Long id, String code, String codeId) {
    RestUtils.checkMethod(request, HttpMethod.GET);

    val office = Offices.getOfficeFromRequest(id, code, codeId);
    rules.checkIfPermitted(office);
    
    val badgeReaders = badgeReaderDao.getBadgeReaderByOffice(office);
    
    renderJSON(gsonBuilder.create().toJson(
        badgeReaders.stream().map(BadgeReaderShowTerseDto::build).collect(Collectors.toSet())));
  }
  
  /**
   * Restituisce il JSON con il badgeReader cercato per id. 
   */
  public static void show(Long id) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val badge = getBadgeReaderFromRequest(id);
    renderJSON(gsonBuilder.create().toJson(BadgeReaderShowDto.build(badge)));
  }
  
  /**
   * Cerca il badgeReader in funzione del id passato.
   *
   * @return il badgeReader se trovato, altrimenti torna direttamente 
   *     una risposta HTTP 404.
   */
  @Util
  private static BadgeReader getBadgeReaderFromRequest(Long id) {
    if (id == null) {
      JsonResponse.notFound();
    }

    val badgeReader = badgeReaderDao.byId(id);

    if (badgeReader == null) {
      JsonResponse.notFound();
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //leato al badgeReader
    rules.checkIfPermitted(badgeReader.getUser().getOwner());
    return badgeReader;
  }
}
