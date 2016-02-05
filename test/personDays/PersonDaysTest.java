package personDays;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;

import manager.PersonDayManager;

import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.WorkingTimeTypeDay;

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
  
  @Test
  public void test() {
    
    //LocalDate date1 = new LocalDate(2016, 1, 1);
    //PersonDay previousForProgressive = new PersonDay(null, date1, 0, 0, 60);
    
    LocalDate date2 = new LocalDate(2016, 1, 2);
    PersonDay personDay = new PersonDay(null, date2);
    
    List<Stamping> stampings = Lists.newArrayList();
    stampings.add(new Stamping(personDay, WayType.in,
        new LocalDateTime().withHourOfDay(9).withMinuteOfHour(30)));
    stampings.add(new Stamping(personDay, WayType.out,
        new LocalDateTime().withHourOfDay(16).withMinuteOfHour(30)));
    
    personDay.setStampings(stampings);
    
    PersonDayManager personDayManager = new PersonDayManager(
        null, null, null, null, null, null, null);
    
    personDayManager.updateTimeAtWork(personDay, normalDay(), false, startLunch, endLunch);
    
    assertThat(personDay.getTimeAtWork()).isEqualTo(390);   //6:30 ore
    assertThat(personDay.getStampingsTime()).isEqualTo(420);//7:00 ore     
    assertThat(personDay.getDecurted()).isEqualTo(30);      //30 minuti
    
  }
  
  public WorkingTimeTypeDay normalDay() {
    WorkingTimeTypeDay wttd = new WorkingTimeTypeDay();
    wttd.breakTicketTime = 30;
    wttd.mealTicketTime = 360;
    wttd.workingTime = 432;
    wttd.ticketAfternoonThreshold = 0;
    wttd.holiday = false;
    return wttd;
  }
  
}
