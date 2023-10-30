package controllers;

import com.google.common.collect.Maps;
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

public class GroupOvertimes extends Controller {
  
  @Inject
  private static GroupOvertimeManager groupOvertimeManager;
  @Inject
  private static GroupDao groupDao;

  public static void save(int year, GroupOvertime groupOvertime, Long groupId) {
    /*TODO: Prima di procedere con l'assegnamento delle ore occorre controllare che quella quantità 
     * non ecceda la disponibilità.
     */
    Group group = groupDao.byId(groupId).get();
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
