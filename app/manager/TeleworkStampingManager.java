package manager;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Range;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.Transient;
import org.joda.time.LocalDateTime;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;
import dao.wrapper.IWrapperFactory;
import lombok.extern.slf4j.Slf4j;
import manager.recaps.personstamping.PersonStampingDayRecapFactory;
import models.PersonDay;
import models.TeleworkStamping;
import models.enumerate.StampTypes;

@Slf4j
public class TeleworkStampingManager {

  private final PersonDayDao personDayDao;
  private final PersonDao personDao;
  private final PersonDayManager personDayManager;
  private final PersonStampingDayRecapFactory stampingDayRecapFactory;
  private final ConsistencyManager consistencyManager;
  private final StampingDao stampingDao;
  private final NotificationManager notificationManager;
  private final IWrapperFactory wrapperFactory;
  
  /**
   * Injection.
   * @param personDayDao il dao per cercare i personday
   * @param personDao il dao per cercare le persone
   * @param personDayManager il manager per lavorare sui personday
   * @param stampingDayRecapFactory il factory per lavorare sugli stampingDayRecap
   * @param consistencyManager il costruttore dell'injector.
   */
  @Inject
  public TeleworkStampingManager(PersonDayDao personDayDao,
      PersonDao personDao,
      PersonDayManager personDayManager,
      PersonStampingDayRecapFactory stampingDayRecapFactory,
      ConsistencyManager consistencyManager, StampingDao stampingDao,
      NotificationManager notificationManager, IWrapperFactory wrapperFactory) {

    this.personDayDao = personDayDao;
    this.personDao = personDao;
    this.personDayManager = personDayManager;
    this.stampingDayRecapFactory = stampingDayRecapFactory;
    this.consistencyManager = consistencyManager;
    this.stampingDao = stampingDao;
    this.notificationManager = notificationManager;
    this.wrapperFactory = wrapperFactory;
  }
  
  /**
   * 
   * @param pd
   * @param stampType
   * @param interruption
   * @return
   */
  public List<TeleworkStamping> getSpecificTeleworkStampings(PersonDay pd, 
      Optional<StampTypes> stampType, boolean interruption) {
    if (stampType.isPresent()) {
      return pd.teleworkStampings.stream()
          .filter(st -> st.stampType.equals(StampTypes.PAUSA_PRANZO)).collect(Collectors.toList());
    }
    if (interruption) {
      return pd.teleworkStampings.stream()
          .filter(st -> st.stampType == null && !Strings.isNullOrEmpty(st.note)).collect(Collectors.toList());
    }
    return pd.teleworkStampings.stream()
        .filter(st -> st.stampType == null && Strings.isNullOrEmpty(st.note)).collect(Collectors.toList());
  }
  
  
  /**
   * 
   * @param pd
   * @return
   */
  public boolean hasTeleworkStampingsWellFormed(PersonDay pd) {
    if (pd.teleworkStampings.size() == 0 || pd.teleworkStampings.size() % 2 != 0) {
      return false;
    }
    List<TeleworkStamping> meal = pd.teleworkStampings.stream()
        .filter(st -> st.stampType.equals(StampTypes.PAUSA_PRANZO)).collect(Collectors.toList());
    List<TeleworkStamping> beginEnd = pd.teleworkStampings.stream()
        .filter(st -> st.stampType == null).collect(Collectors.toList());
    List<TeleworkStamping> interruptions = pd.teleworkStampings.stream()
        .filter(st -> st.stampType == null && !Strings.isNullOrEmpty(st.note))
        .collect(Collectors.toList());
    Range<LocalDateTime> beginEndRange = Range.closed(beginEnd.get(0).date, beginEnd.get(1).date);
    Range<LocalDateTime> mealRange = Range.closed(meal.get(0).date, meal.get(1).date);
    for (TeleworkStamping tws : meal) {
      if (!beginEndRange.contains(tws.date)) {
        return false;
      }
    }
    for (TeleworkStamping tws : interruptions) {
      if (!beginEndRange.contains(tws.date) || mealRange.contains(tws.date)) {
        return false;
      }
    }  
    
    return true;
  }
}
