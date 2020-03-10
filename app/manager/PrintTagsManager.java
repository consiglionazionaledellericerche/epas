package manager;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.history.HistoryValue;
import dao.history.PersonDayHistoryDao;
import dao.history.StampingHistoryDao;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.val;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import models.Person;
import models.Stamping;
import models.User;
import models.dto.OffSiteWorkingTemp;
import org.joda.time.LocalDate;
import org.testng.collections.Maps;

public class PrintTagsManager {

  private final StampingHistoryDao stampingHistoryDao;
  private final PersonDayDao personDayDao;
  private final PersonDayHistoryDao personDayHistoryDao;
  private final PersonDao personDao;
  
  @Inject
  public PrintTagsManager(
      StampingHistoryDao stampingHistoryDao, PersonDayHistoryDao personDayHistoryDao,
      PersonDayDao personDayDao, PersonDao personDao) {
    this.stampingHistoryDao = stampingHistoryDao;
    this.personDayHistoryDao = personDayHistoryDao;
    this.personDayDao = personDayDao;
    this.personDao = personDao;
  }

  /**
   * Metodo che ritorna le informazioni sullo storico delle timbrature del dipendente.
   * @param psDto il person stamping recap delle timbrature del dipendente
   * @return la lista di liste contenente le informazioni sullo storico delle timbrature.
   */
  public List<List<HistoryValue<Stamping>>> getHistoricalList(PersonStampingRecap psDto) {
    List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();

    for (PersonStampingDayRecap day : psDto.daysRecap) {
      if (!day.ignoreDay) {
        for (Stamping stamping : day.personDay.stampings) {
          if (stamping.markedByAdmin) {
            historyStampingsList.add(stampingHistoryDao.stampings(stamping.id));
          }
        }
      }
    }
    return historyStampingsList;
  }
  
  /**
   * Metodo che ritorna le informazioni sulle timbrature fuori sede.
   * @param psDto il personStampingRecap contenente il recap delle timbrature del dipendente
   * @return la lista contenente tutte le timbrature fuori sede per la persona.
   */
  public List<OffSiteWorkingTemp> getOffSiteStampings(PersonStampingRecap psDto) {
    List<OffSiteWorkingTemp> list = Lists.newArrayList();
    for (PersonStampingDayRecap day : psDto.daysRecap) {
      if (!day.ignoreDay) {
        for (Stamping stamping : day.personDay.stampings) {
          if (stamping.isOffSiteWork()) {
            OffSiteWorkingTemp temp = new OffSiteWorkingTemp();
            temp.date = stamping.personDay.date;
            temp.stamping = stamping;
            temp.reason = stamping.reason;
            temp.place = stamping.place;
            temp.note = stamping.note;
            list.add(temp);
          }            
        }
      }
    }
    
    return list;
  }
  
  public Map<User, Set<LocalDate>> getStampingOwnerInDays(Person person, YearMonth yearMonth) {
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(person.id, "L'id della persona %s è null", person);
    
    val p = personDao.getPersonById(person.id);
    val personDays = personDayDao.getPersonDayInPeriod(
        p, new LocalDate(yearMonth.getYear(), yearMonth.getMonthValue(), 1), 
        Optional.of(
            new LocalDate(yearMonth.getYear(), yearMonth.getMonthValue(), 1).dayOfMonth().withMaximumValue()));
    Map<User, Set<LocalDate>> badgeReaderMap = Maps.newHashMap();
    personDays.forEach(pd -> {
      personDayHistoryDao.stampingsAtCreation(pd.id).forEach(s -> {
        if (!badgeReaderMap.containsKey(s.revision.owner)) {
          Set<LocalDate> dates = Sets.newHashSet(s.value.date.toLocalDate());
          badgeReaderMap.put(s.revision.owner, dates);
        } else {
          badgeReaderMap.get(s.revision.owner).add(s.value.date.toLocalDate());
        }
      });
    });
    return badgeReaderMap;
  }
}
