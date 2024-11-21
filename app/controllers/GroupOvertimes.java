/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

import com.google.common.collect.Maps;
import common.security.SecurityRules;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.GroupDao;
import manager.GroupOvertimeManager;
import models.Competence;
import models.CompetenceCode;
import models.GroupOvertime;
import models.Person;
import models.User;
import models.flows.Group;
import play.data.validation.Equals;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;

@With({Resecure.class})
public class GroupOvertimes extends Controller {
  
  @Inject
  private static GroupOvertimeManager groupOvertimeManager;
  @Inject
  private static GroupDao groupDao;
  @Inject
  private static SecurityRules rules;

  public static void save(int year, GroupOvertime groupOvertime, Long groupId) {
    
    Group group = groupDao.byId(groupId).get();
    notFoundIfNull(group);
    rules.checkIfPermitted(group.getOffice());
    if (!groupOvertime.getNumberOfHours().toString().matches("\\d+")) {
      flash.error("Inserire una cifra e non delle lettere");
      Groups.handleOvertimeGroup(group.getId());
    }
    groupOvertime.setGroup(group);
    if (groupOvertimeManager.checkOvertimeAvailability(groupOvertime, year)) {
      groupOvertime.setDateOfUpdate(LocalDate.now());
      groupOvertime.setYear(year);      
      groupOvertime.save();
      flash.success("Aggiunta quantità di ore di straordinario per il gruppo %s", group.getName());
    } else {
      flash.error("La quantità che si intende inserire fa superare il limite di ore disponibili!!");
    }

    Groups.handleOvertimeGroup(group.getId());
  }
  
}
