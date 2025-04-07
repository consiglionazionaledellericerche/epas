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

import cnr.sync.dto.v2.ContractWorkingTimeTypeShowTerseDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import dao.WorkingTimeTypeDao;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.io.IOException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import play.mvc.Controller;
import play.mvc.With;

/**
 * API Rest per la gestione dell'associazione tra contratto e 
 * tipologie di orario di lavoro.
 *
 * @author Cristian Lucchesi
 *
 */
@Slf4j
@With(Resecure.class)
public class ContractWorkingTimeTypes extends Controller {

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
    val cwtt = wttDao.getContractWorkingTimeType(id);

    if (cwtt == null) {
      log.info("Associazione tra contratto e tipologia orario di lavoro non trovato per id = {}",
          id);
      JsonResponse.notFound(
          "Non è stato possibile individuare l'associazione tra il contratto ed il tipo di orario"
          + " di lavoro in ePAS.");
    }

    //Controlla i permessi nel caso si tratti di un orario per un singolo ufficio

    if (cwtt.getContract().getPerson().getCurrentOffice().get() != null) {
      rules.checkIfPermitted(cwtt.getContract().getPerson().getCurrentOffice().get());

    }

    renderJSON(gsonBuilder.create().toJson(ContractWorkingTimeTypeShowTerseDto.build(cwtt)));
  }

  /**
   * Aggiorna il campo externalId di un ContractWorkingTimeType (l'associazione tra contratto
   * e tipologia orario di lavoro).
   * Questo metodo può essere chiamato solo via HTTP PUT.
   */
  public static void updateExternalId(Long id, String externalId) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtils.checkMethod(request, HttpMethod.PUT);
    
    if (id == null) {
      JsonResponse.badRequest("Il campo id è obbligatorio");
    }
    
    log.debug("Update contractWorkingTimeType -> id = {}, externalId = {}", 
        id, externalId);
    
    val cwtt = wttDao.getContractWorkingTimeType(id);

    notFoundIfNull(cwtt);

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //della persona

    rules.checkIfPermitted(cwtt.getContract().getPerson().getCurrentOffice().get());
    
    cwtt.setExternalId(externalId);
    cwtt.save();

    log.info("Updated ContractWorkingTimeType (externalId) {} via REST", cwtt);
    renderJSON(gsonBuilder.create().toJson(ContractWorkingTimeTypeShowTerseDto.build(cwtt)));
  }

}