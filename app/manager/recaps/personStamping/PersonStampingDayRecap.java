package manager.recaps.personStamping;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;

import it.cnr.iit.epas.DateUtility;

import lombok.extern.slf4j.Slf4j;

import manager.ConfigurationManager;
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
import models.enumerate.EpasParam;
import models.enumerate.Parameter;
import models.enumerate.EpasParam.EpasParamValueType;
import models.enumerate.EpasParam.EpasParamValueType.LocalTimeInterval;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.List;

/**
 * Oggetto che modella il giorno di una persona nelle viste - personStamping - stampings -.
 * dailyPresence - clocks
 *
 * @author alessandro
 */
@Slf4j
public class PersonStampingDayRecap {

  private static final String MEALTICKET_NOT_YET = "NOT_YET";
  private static final String MEALTICKET_YES = "YES";
  private static final String MEALTICKET_NO = "NO";
  private static final String MEALTICKET_EMPTY = "";

  private final StampingTemplateFactory stampingTemplateFactory;
  private final PersonDayManager personDayManager;

  private static StampModificationType fixedStampModificationType = null;

  public PersonDay personDay;
  public Long personDayId;
  public Person person;
  public WorkingTimeTypeDay wttd = null;
  public WorkingTimeType wtt = null;

  public String workingTime = "";
  public String workTimeExplanation = "";

  public String mealTicketTime = "";
  public String timeMealFromTo = "";
  public String breakTicketTime = "";
  
  public String workTimeFromTo = "";

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

  public List<String> note = Lists.newArrayList();

  /**
   * Costruisce l'oggetto contenente un giorno lavorativo da visualizzare nel tabellone timbrature.
   *
   * @param personDayManager        injected
   * @param personManager           injected
   * @param stampingTemplateFactory injected
   * @param stampTypeManager        injected
   * @param wrapperFactory          injected
   * @param workingTimeTypeDao      injected
   * @param confGeneralManager      injected
   * @param pd                      personDay
   * @param numberOfInOut           numero di colonne del tabellone a livello mensile.
   * @param monthContracts          il riepiloghi del mese
   */
  public PersonStampingDayRecap(PersonDayManager personDayManager, PersonManager personManager,
                                StampingTemplateFactory stampingTemplateFactory,
                                StampTypeManager stampTypeManager, IWrapperFactory wrapperFactory,
                                WorkingTimeTypeDao workingTimeTypeDao,
                                ConfigurationManager configurationManager, PersonDay pd,
                                int numberOfInOut, Optional<List<Contract>> monthContracts) {

    this.stampingTemplateFactory = stampingTemplateFactory;
    this.personDayManager = personDayManager;

    this.personDay = pd;
    this.personDayId = pd.id;

    if (pd.isToday()) {
      log.debug("Instanziato PersonStampingDayRecap relativo al giorno corrente.");
    }

    if (pd.isPersistent()) {
      this.holiday = pd.isHoliday;
    } else {
      this.holiday = personManager.isHoliday(pd.person, pd.date);
    }

    this.person = pd.person;
    setDateInfo(pd.date);
    this.absences = pd.absences;

    IWrapperPersonDay wrPersonDay = wrapperFactory.create(pd);

    this.stampingsTemplate = getStampingsTemplate(wrPersonDay, numberOfInOut);
    this.note.addAll(getStampingsNote(this.stampingsTemplate));

    Optional<WorkingTimeType> wtt = workingTimeTypeDao.getWorkingTimeType(pd.date, pd.person);

    if (wtt.isPresent()) {

      this.wtt = wtt.get();

      this.wttd = this.wtt.workingTimeTypeDays.get(pd.date.getDayOfWeek() - 1);

      this.workingTimeTypeDescription = this.wtt.description;

      this.setWorkingTime(this.wttd.workingTime);
      this.setMealTicketTime(this.wttd.mealTicketTime);
      this.setBreakTicketTime(this.wttd.breakTicketTime);
    }

    LocalTimeInterval lunchInterval = (LocalTimeInterval)configurationManager.configValue(
        pd.person.office, EpasParam.LUNCH_INTERVAL, pd.getDate());
    LocalTimeInterval workInterval = (LocalTimeInterval)configurationManager.configValue(
        pd.person.office, EpasParam.WORK_INTERVAL, pd.getDate());
    this.timeMealFromTo = EpasParamValueType.formatValue(lunchInterval);
    this.workTimeFromTo = EpasParamValueType.formatValue(workInterval);

    if (wrPersonDay.isFixedTimeAtWork()) {

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

      //personDayManager.queSeraSera(wrapperFactory.create(pd));
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

    this.setMealTicket(pd.isTicketAvailable);

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
        /**
         * Se il giorno è:
         * Precedente all'inizio del contratto
         * Oppure precedente a un'inizializzazione definita
         * Oppure precedente alla data di inserimento della persona
         * Viene Ignorato
         */
        if (contract.beginDate.isAfter(pd.date) ||
            (contract.sourceDateResidual != null && !pd.date.isAfter(contract.sourceDateResidual)) ||
            pd.date.isBefore(person.createdAt.toLocalDate())) {
          this.ignoreDay = true;
        }

        if (contract.beginDate.isEqual(pd.date)) {
          this.firstDay = true;
        }
      }
      if (ignoreDay) {
        this.setWorkTime(0);
        this.setDifference(0);
        this.setProgressive(0);
        this.setMealTicket(true);
      }
    }
  }


  /**
   * Imposta il valore della colonna buono pasto nel tabellone timbrature.
   *
   * @param mealTicket ottenuto si/no
   */
  private void setMealTicket(boolean mealTicket) {

    if (this.ignoreDay || !this.personDay.isPersistent()) {
      this.mealTicket = MEALTICKET_EMPTY;
      return;
    }
    if (this.holiday) {
      this.mealTicket = MEALTICKET_EMPTY;
      return;
    }
    // GIORNI FUTURI
    if (this.future) {
      if (!this.absences.isEmpty()) {
        this.mealTicket = MEALTICKET_NO;
      } else {
        this.mealTicket = MEALTICKET_EMPTY;
      }
      return;
    }
    // Giorni Passati e giorno attuale
    if (!mealTicket) {
      if (this.today) {
        this.mealTicket = MEALTICKET_NOT_YET;
      } else {
        this.mealTicket = MEALTICKET_NO;
      }
    } else {
      this.mealTicket = MEALTICKET_YES;
    }
  }

  /**
   * Imposta il valore dei campi date, past, today, future.
   *
   * @param date la data.
   */
  private void setDateInfo(LocalDate date) {

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
   * Crea le timbrature da visualizzare nel tabellone timbrature. <br> 1) Riempita di timbrature
   * fittizie nelle celle vuote, fino ad arrivare alla dimensione di numberOfInOut. <br> 2) Con
   * associato il colore e il tipo di bordatura da visualizzare nel tabellone.
   *
   * @param wrPersonDay   il personDay
   * @param numberOfInOut numero di timbrature.
   * @return la lista di timbrature per il template.
   */
  private List<StampingTemplate> getStampingsTemplate(IWrapperPersonDay wrPersonDay,
                                                      int numberOfInOut) {

    List<Stamping> stampings = personDayManager
        .getStampingsForTemplate(wrPersonDay, numberOfInOut);

    List<StampingTemplate> stampingsTemplate = Lists.newArrayList();

    boolean samePair = false;
    for (Stamping stamping : stampings) {

      //La posizione della timbratura all'interno della sua coppia.
      String position = "none";
      if (stamping.pairId != 0 && stamping.isIn()) {
        position = "left";
        samePair = true;
      } else if (stamping.pairId != 0 && stamping.isOut()) {
        position = "right";
        samePair = false;
      } else if (samePair) {
        position = "center";
      }

      StampingTemplate stampingTemplate = stampingTemplateFactory.create(stamping, position);

      stampingsTemplate.add(stampingTemplate);
    }
    return stampingsTemplate;
  }

  /**
   * La lista delle note in stampingsTemplate.
   *
   * @param stampingsTemplate le timbrature del giorno.
   * @return la lista di note
   */
  private List<String> getStampingsNote(List<StampingTemplate> stampingsTemplate) {
    List<String> note = Lists.newArrayList();
    for (StampingTemplate stampingTemplate : stampingsTemplate) {
      if (stampingTemplate.stamping.note != null && !stampingTemplate.stamping.note.equals("")) {
        note.add(stampingTemplate.hour + ": " + stampingTemplate.stamping.note);
      }
    }
    return note;
  }


  /**
   * Formatta il valore del tempo minimo per il buono pasto. 0 string vuota.
   *
   * @param mealTicketTime minuti pausa pranzo.
   */
  private void setMealTicketTime(int mealTicketTime) {
    if (mealTicketTime == 0) {
      this.mealTicketTime = "";
    } else {
      this.mealTicketTime = DateUtility.fromMinuteToHourMinute(mealTicketTime);
    }
  }

  /**
   * Formatta il valore della pausa minima. 0 Stringa vuota.
   *
   * @param breakTicketTime minuti pausa miniama
   */
  private void setBreakTicketTime(int breakTicketTime) {
    if (breakTicketTime == 0) {
      this.breakTicketTime = "";
    } else {
      this.breakTicketTime = DateUtility.fromMinuteToHourMinute(breakTicketTime);
    }
  }

  /**
   * Formatta il valore del tempo a lavoro previsto dal tipo orario. 0 stringa vuota.
   *
   * @param workingTime minuti lavorati
   */
  private void setWorkingTime(int workingTime) {
    if (workingTime == 0) {
      this.workingTime = "";
    } else {
      this.workingTime = DateUtility.fromMinuteToHourMinute(workingTime);
    }
  }

  /**
   * Formatta il valore del tempo lavorato nel giorno (lordo comprensivo di tempo decurtato).
   *
   * @param workTime minuti lavorati
   */
  private void setWorkTime(int workTime) {
    this.workTime = DateUtility.fromMinuteToHourMinute(workTime);

    if (personDay.stampingsTime != null && personDay.stampingsTime > 0) {
      this.workTimeExplanation = this.workTimeExplanation
          + "<br>Da timbrature: " +
          stronger(DateUtility.fromMinuteToHourMinute(personDay.stampingsTime));
    }
    
    if (personDay.getDecurtedWork() > 0) {
      this.workTimeExplanation = this.workTimeExplanation
          + "<br>Sottratto perchè al di fuori della finestra orario: " +
          stronger(DateUtility.fromMinuteToHourMinute(personDay.getDecurtedWork()));
    }

    if (personDay.justifiedTimeNoMeal != null && personDay.justifiedTimeNoMeal > 0) {
      this.workTimeExplanation = this.workTimeExplanation
          + "<br>Giustificato da assenze: " +
          stronger(DateUtility.fromMinuteToHourMinute(personDay.justifiedTimeNoMeal));
    }

    if (personDay.justifiedTimeMeal != null && personDay.justifiedTimeMeal > 0) {
      this.workTimeExplanation = this.workTimeExplanation
          + "<br>Giustificato da assenze che concorrono al calcolo del buono pasto: " +
          stronger(DateUtility.fromMinuteToHourMinute(personDay.justifiedTimeMeal));
    }

    if (personDay.decurted != null && personDay.decurted > 0) {
      this.workTimeExplanation = this.workTimeExplanation
          + "<br>Sottratto per pausa pranzo assente o troppo breve: " +
          stronger(DateUtility.fromMinuteToHourMinute(personDay.decurted));
    }
    if (!this.workTimeExplanation.isEmpty()) {
      this.workTimeExplanation =
          "Tempo a lavoro totale: " + stronger(this.workTime) + this.workTimeExplanation;
    }
  }

  private String stronger(String string) {
    return "<strong>" + string + "</strong>";
  }

  /**
   * Formatta il valore della differenza. Imposta i campi this.difference e this.differenceNegative
   *
   * @param difference minuti di differenza
   */
  private void setDifference(int difference) {
    if (difference < 0) {
      this.differenceNegative = true;
    } else {
      this.differenceNegative = false;
    }
    this.difference = DateUtility.fromMinuteToHourMinute(difference);
  }

  /**
   * Formatta il valore del progressivo. Imposta i campi this.progressive e
   * this.progressiveNegative
   *
   * @param progressive minuti di progressivo
   */
  private void setProgressive(int progressive) {
    if (progressive < 0) {
      this.progressiveNegative = true;
    } else {
      this.progressiveNegative = false;
    }
    this.progressive = DateUtility.fromMinuteToHourMinute(progressive);
  }

}
