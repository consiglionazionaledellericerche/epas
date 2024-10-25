package jobs;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dao.OfficeDao;
import dao.PersonDayInTroubleDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
import models.PersonDayInTrouble;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.absences.Absence;
import models.enumerate.Troubles;
import play.jobs.Job;

@SuppressWarnings("rawtypes")
@Slf4j
public class AlertNoInfoDaysJob extends Job {
  
  @Inject
  static OfficeDao officeDao;
  @Inject
  static UsersRolesOfficesDao uroDao;
  @Inject
  static RoleDao roleDao;
  @Inject
  static PersonDayInTroubleDao personDayInTroubleDao;
  
  private static final List<Integer> weekEnd = ImmutableList
      .of(DateTimeConstants.SATURDAY, DateTimeConstants.SUNDAY);
  
  @Override
  public void doJob() {
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
      LocalDate begin = LocalDate.now().minusWeeks(1).dayOfMonth().withMinimumValue();
      LocalDate end = LocalDate.now().minusDays(1);
      log.debug("Inizio a mandare le mail per office...");
      for (Map.Entry<Office, List<User>> entry : map.entrySet()) {
//        List<PersonDayInTrouble> pdList = personDayInTroubleDao.getPersonDayInTroubleInPeriod(person,
//            begin, end, Optional.of(troubleCausesToSend));
      }
    }

  }
}
