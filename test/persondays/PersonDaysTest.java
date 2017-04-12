package persondays;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import java.util.List;

import manager.PersonDayManager;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;
import manager.services.PairStamping;

import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.WorkingTimeTypeDay;
import models.enumerate.StampTypes;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.Test;

import play.test.UnitTest;

public class PersonDaysTest extends UnitTest {

  public static LocalTime startLunch = new LocalTime(1,0,0);
  public static LocalTime endLunch = new LocalTime(23,0,0);
  
  public static LocalTime startWork = new LocalTime(0,0,0);
  public static LocalTime endWork = new LocalTime(23,59,0);
 
  public static LocalDate first = new LocalDate(2016, 1, 2);
  public static LocalDate second = new LocalDate(2016, 1, 3);
  
  public static StampTypes lunchST = StampTypes.PAUSA_PRANZO;
  public static StampTypes serviceST = StampTypes.MOTIVI_DI_SERVIZIO;
  
  public static PersonDayManager personDayManager = new PersonDayManager(
      null, null, null, null, null);
  
  /**
   * Test su un giorno Normale.
   */
  @Test
  public void test() {

    PersonDay personDay = new PersonDay(null, second);
    
    List<Stamping> stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 9, 30, WayType.in, null));
    stampings.add(stampings(personDay, 16, 30, WayType.out, null));
    
    personDay.setStampings(stampings);
    
    PersonDay previousForProgressive = new PersonDay(null, first, 0, 0, 60);
    
    personDayManager.updateTimeAtWork(personDay, normalDay(), false, 
        startLunch, endLunch, startWork, endWork);
    personDayManager.updateDifference(personDay, normalDay(), false);
    personDayManager.updateProgressive(personDay, Optional.fromNullable(previousForProgressive));
    personDayManager.updateTicketAvailable(personDay, normalDay(), false);
    
    org.assertj.core.api.Assertions.assertThat(
        personDay.getTimeAtWork()).isEqualTo(390);   //6:30 ore
    org.assertj.core.api.Assertions.assertThat(
        personDay.getStampingsTime()).isEqualTo(420);//7:00 ore     
    org.assertj.core.api.Assertions.assertThat(
        personDay.getDecurted()).isEqualTo(30);      //30 minuti
    org.assertj.core.api.Assertions.assertThat(personDay.getDifference()).isEqualTo(-42);
    org.assertj.core.api.Assertions.assertThat(personDay.getProgressive()).isEqualTo(18);
    org.assertj.core.api.Assertions.assertThat(personDay.isTicketAvailable).isEqualTo(true);
    
  }
  
  /**
   * Quando la pausa pranzo contiene interamente la fascia pranzo dell'istituto va conteggiata.
   */
  @Test
  public void tagliaferriIsHungry() {
        
    PersonDay personDay = new PersonDay(null, second);
    List<Stamping> stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 30, WayType.in, null));
    stampings.add(stampings(personDay, 11, 30, WayType.out, null));
    
    stampings.add(stampings(personDay, 15, 30, WayType.in, null));
    stampings.add(stampings(personDay, 19, 30, WayType.out, null));
    
    personDay.setStampings(stampings);
    
    personDayManager.updateTimeAtWork(personDay, normalDay(), false, 
        startLunch, endLunch, startWork, endWork);
    personDayManager.updateTicketAvailable(personDay, normalDay(), false);
    
    org.assertj.core.api.Assertions.assertThat(
        personDay.getTimeAtWork()).isEqualTo(420);   //7:00 ore
    org.assertj.core.api.Assertions.assertThat(
        personDay.getStampingsTime()).isEqualTo(420);//7:00 ore     
    org.assertj.core.api.Assertions.assertThat(
        personDay.getDecurted()).isEqualTo(null);      //00 minuti
    org.assertj.core.api.Assertions.assertThat(
        personDay.isTicketAvailable).isEqualTo(true);
    
  }
  
  /**
   * Quando una persona dispone di una coppia di timbrature valide <br> 
   * (cioè che contribuiscono a calcolare il tempo a lavoro)<br> 
   * in cui almeno una delle due timbrature è taggata con 
   * StampTypes.MOTIVI_DI_SERVIZIO_FUORI_SEDE... <br>
   * <u>Allora:</u><br>
   * All'interno di tale coppia possono esserci tutte
   * e sole timbrature di servizio. 
   * L'ordine delle timbrature di servizio in questo caso non è più vincolante. 
   * Esse contribuiscono esclusivamente a segnalare la presenza in sede o meno della persona. 
   */
  @Test
  public void mazzantiIsInServiceOutSite() {

    //coppia valida con dentro una timbratura di servizio ok
    PersonDay personDay = new PersonDay(null, second);
    List<Stamping> stamps = Lists.newArrayList();
    stamps.add(stampings(personDay, 8, 30, WayType.in, StampTypes.MOTIVI_DI_SERVIZIO_FUORI_SEDE));
    stamps.add(stampings(personDay, 15, 30, WayType.in, StampTypes.MOTIVI_DI_SERVIZIO));
    stamps.add(stampings(personDay, 19, 30, WayType.out, null));
    personDayManager.setValidPairStampings(personDay.stampings);
    org.assertj.core.api.Assertions.assertThat(personDayManager.allValidStampings(personDay));

    //coppia valida con dentro timbrature di servizio con ordine sparso ok 
    personDay = new PersonDay(null, second);
    stamps = Lists.newArrayList();
    stamps.add(stampings(personDay, 8, 30, WayType.in, StampTypes.MOTIVI_DI_SERVIZIO_FUORI_SEDE));
    stamps.add(stampings(personDay, 14, 30, WayType.out, StampTypes.MOTIVI_DI_SERVIZIO));
    stamps.add(stampings(personDay, 15, 30, WayType.in, StampTypes.MOTIVI_DI_SERVIZIO));
    stamps.add(stampings(personDay, 16, 30, WayType.in, StampTypes.MOTIVI_DI_SERVIZIO));
    stamps.add(stampings(personDay, 19, 30, WayType.out, null));
    personDayManager.setValidPairStampings(personDay.stampings);
    org.assertj.core.api.Assertions.assertThat(personDayManager.allValidStampings(personDay));

    //coppia non valida 
    personDay = new PersonDay(null, second);
    stamps = Lists.newArrayList();
    stamps.add(stampings(personDay, 8, 30, WayType.in, StampTypes.MOTIVI_DI_SERVIZIO_FUORI_SEDE));
    stamps.add(stampings(personDay, 15, 30, WayType.in, null));
    stamps.add(stampings(personDay, 19, 30, WayType.out, null));
    personDayManager.setValidPairStampings(personDay.stampings);
    org.assertj.core.api.Assertions.assertThat(!personDayManager.allValidStampings(personDay));

  }
  
  @Test
  public void consideredGapLunchPairsOutOfSite() {
    
    org.assertj.core.api.Assertions.assertThat(
        StampTypes.LAVORO_FUORI_SEDE.isGapLunchPairs()).isEqualTo(true);
    org.assertj.core.api.Assertions.assertThat(
        StampTypes.PAUSA_PRANZO.isGapLunchPairs()).isEqualTo(true);
    
    PersonDay personDay = new PersonDay(null, second);
    List<Stamping> stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 30, WayType.in, null));
    stampings.add(stampings(personDay, 11, 30, WayType.out, null));
    
    stampings.add(stampings(personDay, 15, 30, WayType.in, StampTypes.LAVORO_FUORI_SEDE));
    stampings.add(stampings(personDay, 19, 30, WayType.out, null));
    
    personDay.setStampings(stampings);
    
    personDayManager.updateTimeAtWork(personDay, normalDay(), false, 
        startLunch, endLunch, startWork, endWork);
    personDayManager.updateTicketAvailable(personDay, normalDay(), false);
    
    org.assertj.core.api.Assertions.assertThat(
        personDay.getTimeAtWork()).isEqualTo(420);     //7:00 ore
    org.assertj.core.api.Assertions.assertThat(
        personDay.getStampingsTime()).isEqualTo(420);  //7:00 ore     
    org.assertj.core.api.Assertions.assertThat(
        personDay.getDecurted()).isEqualTo(null);      //00 minuti
    org.assertj.core.api.Assertions.assertThat(
        personDay.isTicketAvailable).isEqualTo(true);
    
    // # anche le coppie che hanno due causali diverse ma che hanno il parametro gapLunchPairs true
    
    personDay = new PersonDay(null, second);
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 30, WayType.in, null));
    stampings.add(stampings(personDay, 11, 30, WayType.out, StampTypes.PAUSA_PRANZO));
        
    stampings.add(stampings(personDay, 15, 30, WayType.in, StampTypes.LAVORO_FUORI_SEDE));
    stampings.add(stampings(personDay, 19, 30, WayType.out, null));
    
    personDay.setStampings(stampings);
    
    personDayManager.updateTimeAtWork(personDay, normalDay(), false, 
        startLunch, endLunch, startWork, endWork);
    personDayManager.updateTicketAvailable(personDay, normalDay(), false);
    
    org.assertj.core.api.Assertions.assertThat(
        personDay.getTimeAtWork()).isEqualTo(420);     //7:00 ore
    org.assertj.core.api.Assertions.assertThat(
        personDay.getStampingsTime()).isEqualTo(420);  //7:00 ore     
    org.assertj.core.api.Assertions.assertThat(
        personDay.getDecurted()).isEqualTo(null);      //00 minuti
    org.assertj.core.api.Assertions.assertThat(
        personDay.isTicketAvailable).isEqualTo(true);
    
  }
  
  /**
   * Le pause pranzo da considerare sono tutte quelle che hanno:
   * #1 Uscita pr Ingresso pr
   * Uscita pr Ingresso 
   * Uscita    Ingrssso pr
   * Uscita    Ingresso    (e sono in istituto non di servizio). 
   */
  @Test
  public void consideredGapLunchPairs() { 

    PersonDay personDay = new PersonDay(null, second);

    org.assertj.core.api.Assertions.assertThat(
        lunchST.isGapLunchPairs()).isEqualTo(true);
    org.assertj.core.api.Assertions.assertThat(
        StampTypes.MOTIVI_PERSONALI.isGapLunchPairs()).isEqualTo(false);

    // #1
    List<Stamping> stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 00, WayType.in, null));
    stampings.add(stampings(personDay, 13, 00, WayType.out, lunchST));
    stampings.add(stampings(personDay, 14, 00, WayType.in, lunchST));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    
    personDay.setStampings(stampings);
    List<PairStamping> gapLunchPair = 
        personDayManager.getGapLunchPairs(personDay, startLunch, endLunch, Optional.absent());

    org.assertj.core.api.Assertions.assertThat(gapLunchPair.size()).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(gapLunchPair.get(0).timeInPair).isEqualTo(60);

    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 13, 00, WayType.out, lunchST));
    stampings.add(stampings(personDay, 14, 00, WayType.in, lunchST));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    
    List<PairStamping> validPairs = personDayManager.getValidPairStampings(personDay.stampings);
    
    gapLunchPair = personDayManager
        .getGapLunchPairs(personDay, startLunch, endLunch, Optional.absent());
    
    org.assertj.core.api.Assertions.assertThat(validPairs.size()).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(validPairs.get(0).timeInPair).isEqualTo(180);
    org.assertj.core.api.Assertions.assertThat(gapLunchPair.size()).isEqualTo(0);


    // #2
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 00, WayType.in, null));
    stampings.add(stampings(personDay, 13, 00, WayType.out, lunchST));
    stampings.add(stampings(personDay, 14, 00, WayType.in, null));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    gapLunchPair = personDayManager
        .getGapLunchPairs(personDay, startLunch, endLunch, Optional.absent());
    
    org.assertj.core.api.Assertions.assertThat(gapLunchPair.size()).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(gapLunchPair.get(0).timeInPair).isEqualTo(60);
    
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 13, 00, WayType.out, lunchST));
    stampings.add(stampings(personDay, 14, 00, WayType.in, null));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    
    validPairs = personDayManager.getValidPairStampings(personDay.stampings);
    gapLunchPair = personDayManager
        .getGapLunchPairs(personDay, startLunch, endLunch, Optional.absent());
    
    org.assertj.core.api.Assertions.assertThat(validPairs.size()).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(validPairs.get(0).timeInPair).isEqualTo(180);
    org.assertj.core.api.Assertions.assertThat(gapLunchPair.size()).isEqualTo(0);
     
    // #3
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 00, WayType.in, null));
    stampings.add(stampings(personDay, 13, 00, WayType.out, null));
    stampings.add(stampings(personDay, 14, 00, WayType.in, lunchST));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    
    gapLunchPair = personDayManager
        .getGapLunchPairs(personDay, startLunch, endLunch, Optional.absent());
    
    org.assertj.core.api.Assertions.assertThat(gapLunchPair.size()).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(gapLunchPair.get(0).timeInPair).isEqualTo(60);
    
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 13, 00, WayType.out, null));
    stampings.add(stampings(personDay, 14, 00, WayType.in, lunchST));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    
    validPairs = personDayManager.getValidPairStampings(personDay.stampings);
    gapLunchPair = personDayManager
        .getGapLunchPairs(personDay, startLunch, endLunch, Optional.absent());
    
    org.assertj.core.api.Assertions.assertThat(validPairs.size()).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(validPairs.get(0).timeInPair).isEqualTo(180);
    org.assertj.core.api.Assertions.assertThat(gapLunchPair.size()).isEqualTo(0);
    
    // # L'ingresso post pranzo deve essere coerente.
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 00, WayType.in, null));
    stampings.add(stampings(personDay, 13, 00, WayType.out, lunchST));
    stampings.add(stampings(personDay, 14, 00, WayType.in, StampTypes.MOTIVI_PERSONALI));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    
    validPairs = personDayManager.getValidPairStampings(personDay.stampings);
    gapLunchPair = personDayManager
        .getGapLunchPairs(personDay, startLunch, endLunch, Optional.absent());
    
    org.assertj.core.api.Assertions.assertThat(validPairs.size()).isEqualTo(2);
    org.assertj.core.api.Assertions.assertThat(gapLunchPair.size()).isEqualTo(0);
       
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 00, WayType.in, null));
    stampings.add(stampings(personDay, 12, 30, WayType.out, lunchST));
    stampings.add(stampings(personDay, 13, 00, WayType.in, StampTypes.MOTIVI_PERSONALI));
    stampings.add(stampings(personDay, 13, 30, WayType.out, StampTypes.MOTIVI_PERSONALI));
    stampings.add(stampings(personDay, 14, 00, WayType.in, null));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    
    // # Il test che secondo Daniele fallisce
    LocalTime startLunch = new LocalTime(12,0,0);
    LocalTime endLunch = new LocalTime(15,0,0);

    validPairs = personDayManager.getValidPairStampings(personDay.stampings);
    gapLunchPair = personDayManager
        .getGapLunchPairs(personDay, startLunch, endLunch, Optional.absent());
    
    org.assertj.core.api.Assertions.assertThat(gapLunchPair.size()).isEqualTo(0);
    
    
  }
  
  /**
   * Il test verifica il funzionamento del meccanismo di stima del tempo al
   * lavoro uscendo in questo momento.
   */
  @Test
  public void estimatedTimeAtWorkToday() {
    
    PersonDay previousForProgressive = new PersonDay(null, first, 0, 0, 60);

    //Caso base una timbratura di ingresso
    PersonDay personDay = new PersonDay(null, second);
    
    List<Stamping> stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 9, 30, WayType.in, null));
    
    LocalDateTime exitingTime = new LocalDateTime(second.getYear(), second.getMonthOfYear(), 
        second.getDayOfMonth(), 16, 30);
    //final LocalDateTime time18 = new LocalDateTime(second).withHourOfDay(18);
    
    personDayManager.queSeraSera(personDay, exitingTime, 
        Optional.fromNullable(previousForProgressive), normalDay(), false,
        new LocalTimeInterval(startLunch, endLunch), new LocalTimeInterval(startWork, endWork));
    
    org.assertj.core.api.Assertions.assertThat(
        personDay.getTimeAtWork()).isEqualTo(390);   //6:30 ore
    org.assertj.core.api.Assertions.assertThat(
        personDay.getStampingsTime()).isEqualTo(420);//7:00 ore     
    org.assertj.core.api.Assertions.assertThat(
        personDay.getDecurted()).isEqualTo(30);      //30 minuti
    org.assertj.core.api.Assertions.assertThat(
        personDay.getDifference()).isEqualTo(-42);
    org.assertj.core.api.Assertions.assertThat(
        personDay.getProgressive()).isEqualTo(18);
    org.assertj.core.api.Assertions.assertThat(
        personDay.isTicketAvailable).isEqualTo(true);
    
    //Caso con uscita per pranzo
    personDay = new PersonDay(null, second);
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 9, 00, WayType.in, null));       //4 ore mattina
    stampings.add(stampings(personDay, 13, 00, WayType.out, null));     //pausa pranzo 1 ora
    stampings.add(stampings(personDay, 14, 00, WayType.in, null));
    
    exitingTime = new LocalDateTime(second.getYear(), second.getMonthOfYear(),  //4 ore pom. 
        second.getDayOfMonth(), 18, 00);
    
    LocalTime startLunch12 = new LocalTime(12,0,0);
    LocalTime endLunch15 = new LocalTime(15,0,0);
    personDayManager.queSeraSera(personDay, exitingTime, 
        Optional.fromNullable(previousForProgressive), normalDay(), false,
        new LocalTimeInterval(startLunch12, endLunch15), new LocalTimeInterval(startWork, endWork));

    org.assertj.core.api.Assertions.assertThat(personDay.getTimeAtWork()).isEqualTo(480);   //8 ore
    org.assertj.core.api.Assertions.assertThat(personDay.isTicketAvailable).isEqualTo(true);
    
  }

  /**
   * Supporto alla creazione di un WorkingTimeType da non mockare.
   * @return WorkingTimeTypeDay di default (quelle Normale).
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