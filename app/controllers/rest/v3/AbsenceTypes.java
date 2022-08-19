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

import cnr.sync.dto.v3.AbsenceTypeShowDto;
import cnr.sync.dto.v3.AbsenceTypeShowTerseDto;
import com.beust.jcommander.internal.Lists;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import dao.AbsenceTypeDao;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import models.absences.AbsenceType;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;

/**
 * API Rest per la consultazione dei codici di assenza.
 *
 * @author Cristian Lucchesi
 * @since version 3
 */
@With(Resecure.class)
public class AbsenceTypes extends Controller {

  @Inject
  static AbsenceTypeDao absenceTypeDao;

  @Inject
  static SecurityRules rules;

  @Inject
  static GsonBuilder gsonBuilder;

  /**
   * Metodo Rest che ritorna un Json con la lista dei tipo di assenza
   * presenti nel sistema, con alcune informazioni di base per ogni
   * tipologia di assenza.
   * La lista è filtrabile in funzione del fatto che il codice sia
   * attivo e utilizzato o meno.
   */
  public static void list(Boolean onlyActive, Boolean used) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    List<AbsenceType> absenceTypes = Lists.newArrayList();

    if (onlyActive != null && onlyActive.equals(Boolean.TRUE)) {
      absenceTypes = absenceTypeDao.list(
          Optional.of(LocalDate.now()), Optional.of(LocalDate.now()), Optional.ofNullable(used));
    } else {
      absenceTypes = absenceTypeDao.list(
          Optional.empty(), Optional.empty(), Optional.ofNullable(used));
    }
    renderJSON(gsonBuilder.create().toJson(
        absenceTypes.stream()
          .map(ccg -> AbsenceTypeShowTerseDto.build(ccg))
          .collect(Collectors.toList())
          ));
  }


  /**
   * Restituisce un json con le informazioni relative ad una tipologia
   * di codice di assenza individuata tramite il suo id. 
   */
  public static void show(Long id) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    if (id == null) {
      JsonResponse.badRequest("Il parametro id è obbligatorio");
    }
    val at = absenceTypeDao.getAbsenceTypeById(id);
    RestUtils.checkIfPresent(at);

    renderJSON(gsonBuilder.create().toJson(
        AbsenceTypeShowDto.build(at)));
  }
}
