package jobs;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDateTime;
import com.google.common.base.Joiner;
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
import play.jobs.On;
import play.jobs.OnApplicationStart;
import play.libs.Mail;

/**
 * Job che verifica le approvazioni preventive delle richieste di straordinario e invia
 * a chi deve fare la seconda approvazione la notifica per farla.
 */
@Slf4j
@On("0 0 7 1 * ?") //il primo giorno di ogni mese alle 7.00
public class OvertimeDoubleApprovalJob extends Job {
  
  private static final String BASE_URL = Play.configuration.getProperty("application.baseUrl");
  private static final String PATH = "competencerequests/show";

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
      java.util.Optional<Configuration> managerConf = configurationManager
          .getConfigurationByOfficeAndType(conf.getOffice(), EpasParam.OVERTIME_REQUEST_MANAGER_APPROVAL_REQUIRED);
      java.util.Optional<Configuration> seatSupervisorConf = configurationManager
          .getConfigurationByOfficeAndType(conf.getOffice(), EpasParam.OVERTIME_REQUEST_OFFICE_HEAD_APPROVAL_REQUIRED);
      if (managerConf.isPresent()  && "true".equals(managerConf.get().getValue())) {
        users = uroDao
            .getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.GROUP_MANAGER), conf.getOffice());
      } else if (seatSupervisorConf.isPresent() && "true".equals(seatSupervisorConf.get().getFieldValue())) {
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
            Optional.absent(), CompetenceRequestType.OVERTIME_REQUEST, user.getPerson());
        list = list.stream().filter(cr -> cr.getMonth().equals(begin.getMonthOfYear())).collect(Collectors.toList());
               
        log.trace("Preparo invio mail per {}", user.getPerson().getFullname());
        SimpleEmail simpleEmail = new SimpleEmail();
        String reply = (String) configurationManager
            .configValue(user.getPerson().getOffice(), EpasParam.EMAIL_TO_CONTACT);

        if (!reply.isEmpty()) {
          try {
            simpleEmail.addReplyTo(reply);
            simpleEmail.addTo(user.getPerson().getEmail());
            simpleEmail.setSubject("ePas Approvazione consuntiva straordinari");
            simpleEmail.setMsg(createBodyMail(list, user));
          } catch (EmailException ex) {
            log.error("Rilevato errore nella creazione della mail da inviare a {} per confermare"
                + " le richieste di straordinario.", user.getPerson().getFullname());
            ex.printStackTrace();
          }
        }
 
        Mail.send(simpleEmail);

        log.info("Inviata mail a {} per approvare consuntivamente {} richieste di straordinario.",
            user.getPerson(), list.size());
      }      
    }
  }
  
  private String createBodyMail(List<CompetenceRequest> requests, User user) {
    
    String msg = "";
    String baseUrl = BASE_URL;
    if (!baseUrl.endsWith("/")) {
      baseUrl = baseUrl + "/";
    }
    for (CompetenceRequest request : requests) {
      baseUrl = baseUrl + PATH + "?id=" + request.id + "&type=" + request.getType();
      msg = msg + baseUrl +" \r\n";
      baseUrl = BASE_URL;
    }
    
    final StringBuilder message = new StringBuilder()
        .append(String.format("Gentile %s,\r\n", user.getPerson().fullName()));
    message.append("\r\nSono state individuate le seguenti richieste di straordinario "
        + "che necessitano di approvazione consuntiva come da configurazione: \r\n")
    .append(msg +"\r\n");
   
    message.append("\r\nLa invitiamo a cliccare sui link presenti nella mail o ad entrare nella "
        + "sua pagina ePAS nella sezione -Flussi di lavoro-\r\n")
        .append("\r\nSaluti,\r\n")
        .append("Il team di ePAS");
    
    return message.toString();
  }

}
