package jobs;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import dao.OfficeDao;
import lombok.extern.slf4j.Slf4j;
import manager.configurations.EpasParam;
import models.Configuration;
import models.Office;
import models.UsersRolesOffices;
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
  
  @Override
  public void doJob() {
    
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.debug("Overtime double approval job");
    List<Office> activeOffice = officeDao.allEnabledOffices();
    List<Configuration> configurations = activeOffice.stream()
        .flatMap(o -> o.getConfigurations().stream()
        .filter(c -> c.getEpasParam().equals(EpasParam.OVERTIME_ADVANCE_REQUEST_AND_CONFIRMATION) 
            && "true".equals(c.getFieldValue()))).collect(Collectors.toList());
    for (Configuration conf : configurations) {
      List<UsersRolesOffices> uros = conf.getOffice().getUsersRolesOffices();
    }
  }

}
