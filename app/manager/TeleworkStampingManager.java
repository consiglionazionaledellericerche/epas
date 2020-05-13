package manager;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.Transient;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;
import dao.wrapper.IWrapperFactory;
import it.cnr.iit.epas.DateUtility;
import lombok.extern.slf4j.Slf4j;
import manager.recaps.personstamping.PersonStampingDayRecapFactory;
import manager.services.telework.errors.Errors;
import manager.services.telework.errors.TeleworkStampingError;
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
   * Ritorna la lista di timbrature in telelavoro nel giorno pd con causale appartenente a quelle
   * riportate nella lista di causali da ricercare.
   * @param pd il personDay di riferimento
   * @param stampTypes la lista di causali da cercare
   * @return la lista di timbrature di lavoro in telelavoro con causale quelle passate come 
   *     parametro.
   */
  public List<TeleworkStamping> getSpecificTeleworkStampings(PersonDay pd, 
      List<StampTypes> stampTypes) {
    List<TeleworkStamping> list = Lists.newArrayList();
    for (TeleworkStamping tws : pd.teleworkStampings) {
      for (StampTypes st : stampTypes) {
        if (tws.stampType.equals(st)) {
          list.add(tws);
        }
      }
    }
    return list;
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

  /**
   * 
   * @param stamping
   * @param pd
   * @return
   */
  public Optional<Errors> checkTeleworkStamping(TeleworkStamping stamping, PersonDay pd) {
    List<TeleworkStamping> meal = getSpecificTeleworkStampings(pd, StampTypes.beginEndTelework());
    List<TeleworkStamping> beginEnd = 
        getSpecificTeleworkStampings(pd, StampTypes.beginEndMealInTelework());
    List<TeleworkStamping> interruptions = 
        getSpecificTeleworkStampings(pd, StampTypes.beginEndInterruptionInTelework());
    if (stamping.stampType.equals(StampTypes.INIZIO_TELELAVORO)) {
      if (beginEnd.isEmpty()) {
        return Optional.absent();
      }
      java.util.Optional<TeleworkStamping> stamp = beginEnd.stream()
          .filter(tws -> tws.stampType.equals(StampTypes.INIZIO_TELELAVORO)).findFirst();
      if (stamp.isPresent()) {
        Errors error = new Errors();
        error.error = TeleworkStampingError.BEGIN_STAMPING_PRESENT;
        error.personDay = pd;
        error.advice = "Inizio telelavoro già presente";
        return Optional.of(error);
      }
      for (TeleworkStamping tws : pd.teleworkStampings) {
        if (tws.date.isBefore(stamping.date)) {
          Errors error = new Errors();
          error.error = TeleworkStampingError.BEGIN_STAMPING_PRESENT;
          error.personDay = pd;
          error.advice = "Inizio telelavoro già presente";
          return Optional.of(error);
        }
      }
    }
    if (stamping.stampType.equals(StampTypes.FINE_TELELAVORO)) {

    }
    if (stamping.stampType.equals(StampTypes.INIZIO_PRANZO_TELELAVORO)) {

    }
    if (stamping.stampType.equals(StampTypes.FINE_PRANZO_TELELAVORO)) {

    }
    if (stamping.stampType.equals(StampTypes.INIZIO_INTERRUZIONE)) {

    }
    if (stamping.stampType.equals(StampTypes.FINE_INTERRUZIONE)) {

    }
    return Optional.<Errors>absent();
  }
  
//  private Range<LocalDateTime> getStampingRange(List<TeleworkStamping> list, LocalDate date) {
//    
//    if (list.isEmpty()) {      
//      return Range.closed(setBeginOfTheDay(date), setEndOfTheDay(date));
//    }
//    
//  }
  
  private LocalDateTime setBeginOfTheDay(LocalDate date) {
    return new LocalDateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 0, 0);
  }
  
  private LocalDateTime setEndOfTheDay(LocalDate date) {
    return new LocalDateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 0, 0)
        .hourOfDay().withMaximumValue().minuteOfHour().withMaximumValue();
  }
}
