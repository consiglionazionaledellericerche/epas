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

import cnr.sync.dto.v3.CompetenceCodeGroupShowDto;
import cnr.sync.dto.v3.CompetenceCodeGroupShowTerseDto;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import dao.CompetenceCodeDao;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import play.mvc.Controller;
import play.mvc.With;

/**
 * API Rest per l'esportazione delle informazioni sui gruppi di competenze
 * presenti nel sistema.
 *
 * @author Cristian Lucchesi
 *
 */
@With(Resecure.class)
public class CompetenceGroups extends Controller {


  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static CompetenceCodeDao competenceCodeDao;

  /**
   * Metodo Rest che ritorna il Json con la lista dei gruppi di codici
   * di competenza presenti nel sistema, con alcune informazioni
   * di base per ogni gruppo.
   */
  public static void list() {
    RestUtils.checkMethod(request, HttpMethod.GET);
    renderJSON(gsonBuilder.create().toJson(
        competenceCodeDao.getAllGroups().stream()
          .map(ccg -> CompetenceCodeGroupShowTerseDto.build(ccg))
          .collect(Collectors.toList())
          ));
  }
  
  /**
   * Restituisce un json con le informazioni relative ad un gruppo
   * individuato tramite il suo id. 
   */
  public static void show(Long id) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    if (id == null) {
      JsonResponse.badRequest("Il parametro id è obbligatorio");
    }
    val ccg = competenceCodeDao.getGroupById(id);
    RestUtils.checkIfPresent(ccg);

    renderJSON(gsonBuilder.create().toJson(
        CompetenceCodeGroupShowDto.build(ccg)));
  }
}
