package manager.recaps.personStamping;

import com.google.common.base.Optional;

import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.DateUtility;

import manager.ConfGeneralManager;
import manager.PersonDayManager;
import manager.PersonManager;
import manager.cache.StampTypeManager;

import models.Absence;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampModificationTypeCode;
import models.Stamping;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

/**
 * Oggetto che modella il giorno di una persona nelle viste - personStamping - stampings -
 * dailyPresence - clocks
 *
 * @author alessandro
 */
public class PersonStampingDayRecap {

  private static StampModificationType fixedStampModificationType = null;
  private final StampingTemplateFactory stampingTemplateFactory;
  public PersonDay personDay;
  public Long personDayId;
  public Person person;
  public WorkingTimeTypeDay wttd = null;
  public WorkingTimeType wtt = null;
  public String stampingWorkingTime = "";
  public String workingTime = "";
  public String mealTicketTime = "";
  public String timeMealFrom = "";
  public String timeMealTo = "";
  public String breakTicketTime = "";

  public String mealTicket;

  public LocalDate date;
  public boolean holiday;
  public boolean past;
  public boolean today;
  public boolean future;

  public boolean ignoreDay = false;
  public boolean firstDay = false;

  public List<Absence> absences;

  public List<StampingTemplate> stampingsTemplate;

  public String workTime = "";
  public String todayLunchTimeCode = "";
  public String fixedWorkingTimeCode = "";
  public String exitingNowCode = "";

  public String difference = "";
  public boolean differenceNegative;
  public String progressive = "";
  public boolean progressiveNegative;
  public String workingTimeTypeDescription = "";

  public List<String> note = new ArrayList<String>();

  public PersonStampingDayRecap(PersonDayManager personDayManager,
                                PersonManager personManager,
                                StampingTemplateFactory stampingTemplateFactory,
                                StampTypeManager stampTypeManager, IWrapperFactory wrapperFactory,
                                WorkingTimeTypeDao workingTimeTypeDao, ConfGeneralManager confGeneralManager,
                                PersonDay pd, int numberOfInOut, Optional<List<Contract>> monthContracts) {

    this.stampingTemplateFactory = stampingTemplateFactory;
    this.personDay = pd;
    this.personDayId = pd.id;

    if (pd.isPersistent()) {
      this.holiday = pd.isHoliday;
    } else {
      this.holiday = personManager.isHoliday(pd.person, pd.date);
    }

    this.person = pd.person;
    setDate(pd.date);
    this.absences = pd.absences;

    List<Stamping> stampingsForTemplate = personDayManager
            .getStampingsForTemplate(pd, numberOfInOut, today);

    this.setStampingTemplate(stampingsForTemplate, pd);

    Optional<WorkingTimeType> wtt = workingTimeTypeDao.getWorkingTimeType(pd.date, pd.person);

    if (wtt.isPresent()) {

      this.wtt = wtt.get();

      this.wttd = this.wtt.workingTimeTypeDays.get(pd.date.getDayOfWeek() - 1);

      this.workingTimeTypeDescription = this.wtt.description;

      this.setWorkingTime(this.wttd.workingTime);
      this.setMealTicketTime(this.wttd.mealTicketTime);
      this.setBreakTicketTime(this.wttd.breakTicketTime);
    }

    Integer mealTimeStartHour = confGeneralManager
            .getIntegerFieldValue(Parameter.MEAL_TIME_START_HOUR, pd.person.office);
    Integer mealTimeStartMinute = confGeneralManager
            .getIntegerFieldValue(Parameter.MEAL_TIME_START_MINUTE, pd.person.office);
    Integer mealTimeEndHour = confGeneralManager
            .getIntegerFieldValue(Parameter.MEAL_TIME_END_HOUR, pd.person.office);
    Integer mealTimeEndMinute = confGeneralManager
            .getIntegerFieldValue(Parameter.MEAL_TIME_END_MINUTE, pd.person.office);

    this.setTimeMealFrom(mealTimeStartHour, mealTimeStartMinute);
    this.setTimeMealTo(mealTimeEndHour, mealTimeEndMinute);

    if (wrapperFactory.create(pd).isFixedTimeAtWork()) {

      // fixed:  worktime, difference, progressive, p

      if (this.future) {
        this.fixedWorkingTimeCode = "";
      } else {
        this.setWorkTime(pd.timeAtWork);
        this.setDifference(pd.difference);
        this.setProgressive(pd.progressive);
        if (pd.timeAtWork != 0) {
          if (fixedStampModificationType == null) {
            fixedStampModificationType =
                    stampTypeManager.getStampMofificationType(
                            StampModificationTypeCode.FIXED_WORKINGTIME);
          }
          this.fixedWorkingTimeCode = fixedStampModificationType.code;
        }
      }
    } else if (this.past) {

      // not fixed:  worktime, difference, progressive for past

      this.setWorkTime(pd.timeAtWork);
      this.setDifference(pd.difference);
      this.setProgressive(pd.progressive);
    } else if (this.today) {

      // not fixed:  worktime, difference, progressive for today

      personDayManager.queSeraSera(wrapperFactory.create(pd));
      this.setWorkTime(pd.timeAtWork);
      this.setDifference(pd.difference);
      this.setProgressive(pd.progressive);
    }
    // worktime, difference, progressive for future
    if (this.future) {
      this.difference = "";
      this.workTime = "";
      this.progressive = "";
    }

    // meal ticket (NO)
    if (this.today && !personDayManager.isAllDayAbsences(pd)) {
      this.setMealTicket(pd.isTicketAvailable, true);

    } else if (this.today && personDayManager.isAllDayAbsences(pd)) {
      //c'è una assenza giornaliera, la decisione è già presa
      this.setMealTicket(pd.isTicketAvailable, false);

    } else if (!this.holiday) {
      this.setMealTicket(pd.isTicketAvailable, false);

    } else {
      this.setMealTicket(true, false);

    }

    // lunch (p,e)
    if (pd.stampModificationType != null && !this.future) {
      this.todayLunchTimeCode = pd.stampModificationType.code;
    }
    // uscita adesso f
    if (this.today && !this.holiday && !personDayManager.isAllDayAbsences(pd)) {
      StampModificationType smt =
              stampTypeManager.getStampMofificationType(
                      StampModificationTypeCode.ACTUAL_TIME_AT_WORK);
      this.exitingNowCode = smt.code;
    }

    // is sourceContract (solo se monthContracts presente)
    if (monthContracts.isPresent()) {
      for (Contract contract : monthContracts.get()) {
        // se è precedente all'inizio del contratto lo ignoro
        if (contract.beginContract.isAfter(pd.date)) {
          this.ignoreDay = true;
        }

        // se è precedente a source lo ignoro
        if (contract.sourceDateResidual != null
                && (contract.sourceDateResidual.equals(pd.date) ||
                contract.sourceDateResidual.isAfter(pd.date))) {
          this.ignoreDay = true;
        }

        if (contract.beginContract.isEqual(pd.date)) {
          this.firstDay = true;
        }
      }
      if (ignoreDay) {
        this.setWorkTime(0);
        this.setDifference(0);
        this.setProgressive(0);
        this.setMealTicket(true, false);
      }
    }
  }


  /**
   *
   * @param mealTicket
   * @param todayInProgress
   */
  private void setMealTicket(boolean mealTicket, boolean todayInProgress) {

    //Caso di oggi
    if (todayInProgress) {
      if (!mealTicket) {
        this.mealTicket = "NOT_YET";
      } else {
        this.mealTicket = "YES";
      }
      return;
    }
    //Casi assenze future (create o cancellate)
    if (this.future && !mealTicket && !this.absences.isEmpty()) {
      this.mealTicket = "NO";
      return;
    }

    if (this.future && !mealTicket && this.absences.isEmpty()) {
      this.mealTicket = "";
      return;
    }
    //Casi generali
    if (!mealTicket) {
      this.mealTicket = "NO";
      return;
    }
    this.mealTicket = "";
  }

  /**
   *
   * @param date
   */
  private void setDate(LocalDate date) {

    LocalDate today = new LocalDate();
    this.date = date;
    if (date.equals(today)) {
      this.today = true;
      this.past = false;
      this.future = false;
      return;
    }
    if (date.isBefore(today)) {
      this.today = false;
      this.past = true;
      this.future = false;
      return;

    } else {
      this.today = false;
      this.past = false;
      this.future = true;
      return;
    }
  }

  /**
   *
   * @param stampings
   * @param pd
   */
  private void setStampingTemplate(List<Stamping> stampings, PersonDay pd) {
    StampingTemplate st;
    int actualPair = 0;
    this.stampingsTemplate = new ArrayList<StampingTemplate>();

    for (int i = 0; i < stampings.size(); i++) {
      Stamping stamping = stampings.get(i);

      //Setto pairId e type
      if (stamping.pairId != 0 && stamping.isIn()) {
        st = stampingTemplateFactory.create(stamping, i, pd, stamping.pairId, "left");
        actualPair = stamping.pairId;
      } else if (stamping.pairId != 0 && stamping.isOut()) {
        st = stampingTemplateFactory.create(stamping, i, pd, stamping.pairId, "right");
        actualPair = 0;
      } else if (actualPair != 0) {
        st = stampingTemplateFactory.create(stamping, i, pd, actualPair, "center");
      } else {
        st = stampingTemplateFactory.create(stamping, i, pd, 0, "none");
      }

      //nuova stamping for template
      //st = new StampingTemplate(stamping, i, pd, actualPair, actualPosition);
      this.stampingsTemplate.add(st);
      if (stamping.note != null && !stamping.note.equals("")) {
        note.add(st.hour + ": " + stamping.note);
      }
    }
  }

  /**
   *
   * @param workingTime
   */
  private void setWorkingTime(int workingTime) {
    if (workingTime == 0) {
      this.workingTime = "";
    } else {
      this.workingTime = DateUtility.fromMinuteToHourMinute(workingTime);
    }
  }

  /**
   *
   * @param mealTicketTime
   */
  private void setMealTicketTime(int mealTicketTime) {
    if (mealTicketTime == 0) {
      this.mealTicketTime = "";
    } else {
      this.mealTicketTime = DateUtility.fromMinuteToHourMinute(mealTicketTime);
    }
  }

  /**
   *
   * @param breakTicketTime
   */
  private void setBreakTicketTime(int breakTicketTime) {
    if (breakTicketTime == 0) {
      this.breakTicketTime = "";
    } else {
      this.breakTicketTime = DateUtility.fromMinuteToHourMinute(breakTicketTime);
    }
  }

  /**
   *
   * @param timeMealFromHour
   * @param timeMealFromMinute
   */
  private void setTimeMealFrom(int timeMealFromHour, int timeMealFromMinute) {
    String hour = timeMealFromHour + "";
    if (hour.length() == 1) {
      hour = "0" + hour;
    }
    String minute = timeMealFromMinute + "";
    if (minute.length() == 1) {
      minute = "0" + minute;
    }
    this.timeMealFrom = hour + ":" + minute;
  }

  /**
   *
   * @param timeMealToHour
   * @param timeMealToMinute
   */
  private void setTimeMealTo(int timeMealToHour, int timeMealToMinute) {
    String hour = timeMealToHour + "";
    if (hour.length() == 1) {
      hour = "0" + hour;
    }
    String minute = timeMealToMinute + "";
    if (minute.length() == 1) {
      minute = "0" + minute;
    }
    this.timeMealTo = hour + ":" + minute;
  }

  /**
   *
   * @param workTime
   */
  private void setWorkTime(int workTime) {
    this.workTime = DateUtility.fromMinuteToHourMinute(workTime);
    if (personDay.decurted != null) {
      this.stampingWorkingTime =
              DateUtility.fromMinuteToHourMinute(workTime + personDay.decurted);
    }
  }

  /**
   *
   * @param difference
   */
  private void setDifference(int difference) {
    if (difference < 0) {
      differenceNegative = true;
      //difference = difference * -1;
    } else {
      differenceNegative = false;
    }
    this.difference = DateUtility.fromMinuteToHourMinute(difference);
  }

  /**
   *
   * @param progressive
   */
  private void setProgressive(int progressive) {
    if (progressive < 0) {
      progressiveNegative = true;
      //progressive = progressive * -1;
    } else {
      progressiveNegative = false;
    }
    this.progressive = DateUtility.fromMinuteToHourMinute(progressive);
  }

}


