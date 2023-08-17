package jobs;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.joda.time.LocalDateTime;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import dao.CompetenceRequestDao;
import dao.OfficeDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import lombok.extern.slf4j.Slf4j;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.Configuration;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.flows.CompetenceRequest;
import models.flows.enumerate.CompetenceRequestType;
import play.Play;
import play.jobs.Job;

/**
 * Job che verifica le approvazioni preventive delle richieste di straordinario e invia
 * a chi deve fare la seconda approvazione la notifica per farla.
 */
@Slf4j
public class OvertimeDoubleApprovalJob extends Job {

  @Inject
  static OfficeDao officeDao;
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static CompetenceRequestDao competenceRequestDao;
  @Inject
  static UsersRolesOfficesDao uroDao;
  @Inject
  static RoleDao roleDao;

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.debug("Overtime double approval job");
    LocalDateTime dateTime = LocalDateTime.now();
    LocalDateTime begin = dateTime.minusMonths(1).withDayOfMonth(1);
    LocalDateTime end = begin.dayOfMonth().withMaximumValue();
    List<UsersRolesOffices> uros = Lists.newArrayList();
    List<Office> activeOffice = officeDao.allEnabledOffices();
    List<Configuration> configurations = activeOffice.stream()
        .flatMap(o -> o.getConfigurations().stream()
            .filter(c -> c.getEpasParam().equals(EpasParam.OVERTIME_ADVANCE_REQUEST_AND_CONFIRMATION) 
                && "true".equals(c.getFieldValue()))).collect(Collectors.toList());
    List<User> users = Lists.newArrayList();
    for (Configuration conf : configurations) {
      Configuration managerConf = (Configuration) configurationManager
          .configValue(conf.getOffice(), EpasParam.OVERTIME_REQUEST_MANAGER_APPROVAL_REQUIRED);
      Configuration seatSupervisorConf = (Configuration) configurationManager
          .configValue(conf.getOffice(), EpasParam.OVERTIME_REQUEST_OFFICE_HEAD_APPROVAL_REQUIRED);
      if ("true".equals(managerConf.getFieldValue())) {
        users = uroDao
            .getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.GROUP_MANAGER), conf.getOffice());
      } else if ("true".equals(seatSupervisorConf.getFieldValue())) {
        users = uroDao
            .getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.SEAT_SUPERVISOR), conf.getOffice());
      } else {
        log.warn("Per la sede {} non sono stati configurati nè responsabili di sede nè "
            + "responsabili di gruppo per l'approvazione di richieste di straordinario", conf.getOffice());
        return;
        
      }
      for (User user : users) {
        List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(user);
        uros.addAll(roleList);
        List<CompetenceRequest> list = competenceRequestDao.toApproveResults(uros, begin, 
            Optional.of(end), CompetenceRequestType.OVERTIME_REQUEST, user.getPerson());
        //TODO: invio di mail per ogni utente contenente la lista di richieste di straordinario da approvare
      }      

    }
  }

}
