package controllers;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gdata.util.common.base.Preconditions;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import models.Office;
import models.Person;
import models.PersonDay;
import models.absences.AbsenceType;
import play.mvc.Controller;
import play.mvc.With;

@With({Resecure.class, RequestInit.class})
public class Monitoring extends Controller{
  @Inject
  static PersonDao personDao;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static AbsenceDao absenceDao;
  @Inject
  static AbsenceTypeDao absenceTypeDao;
  
  public static void agileMonitoring(Integer year, Integer month, Long officeId) {
    if (officeId == null) {
      officeId = Long.getLong(session.get("officeSelected"));
    }
    final Office office = Office.findById(officeId);
    notFoundIfNull(office);
    Optional<AbsenceType> abt = absenceTypeDao.getAbsenceTypeByCode("LAGILE");
    LocalDate begin = new LocalDate(year, month, 1);
    LocalDate end = begin.dayOfMonth().withMaximumValue();
    Long count = personDayDao.countDaysWithAbsenceAndStampings(Optional.absent(), begin, end, abt.get());
    Comparator<Person> customComparator = (k1, k2) -> k1.getFullname().compareTo(k2.getFullname());
    Map<Person, List<PersonDay>> map = Maps.newTreeMap(customComparator);
    List<PersonDay> list = personDayDao.getPersonDaysByOfficeInPeriod(office, begin, end);
    for (PersonDay pd : list) {
      if (map.containsKey(pd.getPerson())) {
        list = map.get(pd.getPerson());   
      } else {
        list = Lists.newArrayList();
      }
      if (pd.getStampings().size() > 1 && pd.getAbsences().stream()
          .anyMatch(abs -> abs.getAbsenceType().equals(abt.get()))) {
        list.add(pd);
        map.put(pd.getPerson(), list);
      }      
    }
    
    render(office, year, month, map, count);
  }

}
