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

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import com.google.gson.GsonBuilder;
import cnr.sync.dto.v3.OfficeShowTerseDto;
import common.security.SecurityRules;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.OfficeDao;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import play.mvc.Controller;
import play.mvc.With;

@Slf4j
@With(Resecure.class)
public class Instances extends Controller {
  
  @Inject
  static OfficeDao officeDao;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;

  @BasicAuth
  public static void list() {
    RestUtils.checkMethod(request, HttpMethod.GET);
    log.debug("Richiesta la lista delle sede di questa istanza");
    List<Office> offices = officeDao.allEnabledOffices();
    val list = 
        offices.stream().map(o -> OfficeShowTerseDto.build(o)).collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(list));
  }
}
