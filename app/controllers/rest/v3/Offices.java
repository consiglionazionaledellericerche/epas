/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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

import cnr.sync.dto.v3.OfficeShowTerseDto;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Optional;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.OfficeDao;
import helpers.JodaConverters;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import it.cnr.iit.epas.DateUtility;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;

/**
 * Controller con utilità per la ricerca degli uffici.
 */
@Slf4j
@With(Resecure.class)
public class Offices extends Controller {

  @Inject
  static OfficeDao officeDao;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;

  /**
   * Lista JSON delle persone che appartengono alla sede
   * individuata con i parametri passati. 
   */
  @BasicAuth
  public static void all(LocalDate atDate) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    log.debug("rest.Offices::all atDate = {}", atDate);
    List<Office> offices = Lists.newArrayList();
    if (atDate != null) {
      offices = officeDao.allOffices(JodaConverters.javaToJodaLocalDate(atDate));
    } else {
      offices = officeDao.allEnabledOffices();
    }
    val list = 
        offices.stream().map(o -> OfficeShowTerseDto.build(o)).collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(list));
  }

  /**
   * Restituisce il JSON con i dati dell'ufficio individuato con i parametri
   * passati. 
   */
  public static void show(Long id, String code, String codeId) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val office = getOfficeFromRequest(id, code, codeId);
    rules.checkIfPermitted(office);
    renderJSON(gsonBuilder.create().toJson(OfficeShowTerseDto.build(office)));
  }

  /**
   * Cerca l'ufficio in funzione dei parametri passati.
   * La ricerca viene fatta in funzione dei parametri passati
   * che possono essere null, nell'ordine id, code, codeId.
   *
   * @param id identificativo in ePAS dell'ufficio
   * @param code codice cds dell'ufficio
   * @param codeId sedeId di attestati
   * @return l'Ufficio se trovato, altrimenti torna direttamente 
   *     una risposta HTTP 404.
   * 
   */
  @Util
  public static Office getOfficeFromRequest(
      Long id, String code, String codeId) {
    if (id == null && code == null && codeId == null) {
      JsonResponse.badRequest();
    }
    Optional<Office> office = officeDao.byIdOrCodeOrCodeId(id, code, codeId);

    if (!office.isPresent()) {
      log.info("Non trovato l'ufficio in base ai parametri passati: "
          + "id = {}, code = {}, codeId = {}", 
          id, code, codeId);
      JsonResponse.notFound("Non è stato possibile individuare l'ufficio in ePAS con "
          + "i parametri passati.");
    }

    rules.checkIfPermitted(office.get());

    return office.get();
  }
}