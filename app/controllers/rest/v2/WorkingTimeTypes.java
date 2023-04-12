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

package controllers.rest.v2;

import cnr.sync.dto.v2.WorkingTimeTypeShowDto;
import cnr.sync.dto.v2.WorkingTimeTypeShowTerseDto;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import dao.WorkingTimeTypeDao;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import play.mvc.Controller;
import play.mvc.With;

/**
 * API Rest per la gestione delle tipologie di orario di lavoro.
 *
 * @author Cristian Lucchesi
 *
 */
@Slf4j
@With(Resecure.class)
public class WorkingTimeTypes extends Controller {

  @Inject
  static WorkingTimeTypeDao wttDao;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;

  /**
   * Metodo REST che mostra il WorkingTimeType in funzione del id passato,
   * se non trovata ritorna una risposta HTTP 404.
   *
   * @param id identificativo in ePAS della tipologia di orario di lavoro 
   */
  public static void show(Long id) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    if (id == null) {
      JsonResponse.badRequest("Il campo id è obbligatorio");
    }
    val wtt = wttDao.getWorkingTimeTypeById(id);

    if (wtt == null) {
      log.info("Tipologia di orario di lavoro non trovato per id = {}", id);
      JsonResponse.notFound(
          "Non è stato possibile individuare il tipo di orario di lavoro in ePAS.");
    }
    
    //Controlla i permessi nel caso si tratti di un orario per un singolo ufficio
    if (wtt.getOffice() != null) {
      rules.checkIfPermitted(wtt.getOffice());
    }

    renderJSON(gsonBuilder.create().toJson(WorkingTimeTypeShowDto.build(wtt)));
  }

  /**
   * Lista JSON delle tipologie di orario di lavoro che appartengono alla sede
   * individuata con i parametri passati. 
   */
  public static void list(Long id, String code, String codeId) {
    val office = Offices.getOfficeFromRequest(id, code, codeId);
    val list = 
        wttDao.getEnabledWorkingTimeTypeForOffice(office).stream()
          .map(WorkingTimeTypeShowTerseDto::build).collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(list));
  }

}