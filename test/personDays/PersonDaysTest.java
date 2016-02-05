package personDays;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import manager.PersonDayManager;

import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.WorkingTimeTypeDay;
import models.enumerate.StampTypes;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.testng.annotations.Test;

import java.util.List;

public class PersonDaysTest {

  public static LocalDateTime startLunch = new LocalDateTime()
      .withHourOfDay(1)
      .withMinuteOfHour(0);
  
  public static LocalDateTime endLunch = new LocalDateTime()
      .withHourOfDay(23)
      .withMinuteOfHour(0);
  
  public static LocalDate first = new LocalDate(2016, 1, 2);
  public static LocalDate second = new LocalDate(2016, 1, 3);
  
  /**
   * Test su un giorno Normale.
   */
  @Test
  public void test() {
    

    PersonDay previousForProgressive = new PersonDay(null, first, 0, 0, 60);
    PersonDay personDay = new PersonDay(null, second);
    
    List<Stamping> stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 9, 30, WayType.in, null));
    stampings.add(stampings(personDay, 16, 30, WayType.out, null));
    
    personDay.setStampings(stampings);
    
    PersonDayManager personDayManager = new PersonDayManager(
        null, null, null, null, null, null, null);
    
    personDayManager.updateTimeAtWork(personDay, normalDay(), false, startLunch, endLunch);
    personDayManager.updateDifference(personDay, normalDay(), false);
    personDayManager.updateProgressive(personDay, Optional.fromNullable(previousForProgressive));
    personDayManager.updateTicketAvailable(personDay, normalDay(), false);
    
    assertThat(personDay.getTimeAtWork()).isEqualTo(390);   //6:30 ore
    assertThat(personDay.getStampingsTime()).isEqualTo(420);//7:00 ore     
    assertThat(personDay.getDecurted()).isEqualTo(30);      //30 minuti
    assertThat(personDay.getDifference()).isEqualTo(-42);
    assertThat(personDay.getProgressive()).isEqualTo(18);
    assertThat(personDay.isTicketAvailable).isEqualTo(true);
    
  }
  
  /**
   * Supporto alla creazione di un WorkingTimeType da non mockare.
   * @return
   */
  public WorkingTimeTypeDay normalDay() {
    WorkingTimeTypeDay wttd = new WorkingTimeTypeDay();
    wttd.breakTicketTime = 30;
    wttd.mealTicketTime = 360;
    wttd.workingTime = 432;
    wttd.ticketAfternoonThreshold = null;
    wttd.holiday = false;
    return wttd;
  }
  
  /**
   * Supporto alla creazione di una stamping da non mockare.
   * @param personDay
   * @param hour
   * @param minute
   * @param way
   * @param stampType
   * @return
   */
  public Stamping stampings(PersonDay personDay, int hour, int minute, 
      WayType way, StampTypes stampType) {
    LocalDateTime time = new LocalDateTime(personDay.getDate().getYear(), 
        personDay.getDate().getMonthOfYear(), personDay.getDate().getDayOfMonth(), hour, minute);
    Stamping stamping = new Stamping(personDay, time);
    stamping.way = way;
    stamping.stampType = stampType;
    return stamping;
  }
  
}
