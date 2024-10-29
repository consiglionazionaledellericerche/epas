package jobs;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.apache.commons.mail.EmailException;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayInTroubleDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayInTroubleManager;
import models.Office;
import models.Person;
import models.PersonDayInTrouble;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.absences.Absence;
import models.enumerate.Troubles;
import play.Play;
import play.jobs.Job;
import play.jobs.On;
import play.jobs.OnApplicationStart;

@SuppressWarnings("rawtypes")
@Slf4j
@On("0 0 7 * * ?") // Ore 7:00
public class AlertNoInfoDaysJob extends Job {

  @Inject
  static OfficeDao officeDao;
  @Inject
  static UsersRolesOfficesDao uroDao;
  @Inject
  static RoleDao roleDao;
  @Inject
  static PersonDayInTroubleDao personDayInTroubleDao;
  @Inject
  static PersonDao personDao;
  @Inject
  static PersonDayInTroubleManager personDayInTroubleManager;

  private static final List<Integer> weekEnd = ImmutableList
      .of(DateTimeConstants.SATURDAY, DateTimeConstants.SUNDAY);

  @Override
  public void doJob() throws EmailException {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    if (!weekEnd.contains(LocalDate.now().getDayOfWeek())) {
      log.debug("Inizia la parte di invio email...");

      List<Office> officeList = officeDao.allEnabledOffices();
      Map<Office, List<User>> map = Maps.newHashMap();
      for (Office o : officeList) {
        List<User> userList = uroDao
            .getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.PERSONNEL_ADMIN), o);
        List<User> list = map.get(o);
        if (list == null || list.isEmpty()) {
          list = Lists.newArrayList();
        }
        list.addAll(userList);
        map.put(o, list);
      }
      LocalDate begin = LocalDate.now().minusMonths(1).dayOfMonth().withMinimumValue();
      LocalDate end = LocalDate.now().minusDays(1);
      log.debug("Inizio a mandare le mail per office...");
      for (Map.Entry<Office, List<User>> entry : map.entrySet()) {
        log.info("Analizzo la sede di {}", entry.getKey().getName());
        List<PersonDayInTrouble> pdList = personDayInTroubleDao.getPersonDayInTroubleByOfficeInPeriod(entry.getKey(),
            begin, end, Troubles.NO_ABS_NO_STAMP);
        log.info("Trovate {} problemi per la sede {}", pdList.size(), entry.getKey().getName());
        if (!pdList.isEmpty()) {
          log.info("Invio mail agli amministratori di {}", entry.getKey().getName());
          personDayInTroubleManager.sendOfficeTroubleEmailsToAdministrators(pdList, entry.getValue(), entry.getKey());
        }        
      }
    }
    log.info("Concluso AlertNoInfoDaysJob");
  }
}
