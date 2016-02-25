package manager.recaps.personStamping;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;

import it.cnr.iit.epas.DateUtility;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import manager.ConfigurationManager;
import manager.PersonDayManager;
import manager.PersonManager;
import manager.cache.StampTypeManager;

import models.Absence;
import models.Contract;
import models.PersonDay;
import models.StampModificationType;
import models.StampModificationTypeCode;
import models.Stamping;
import models.WorkingTimeTypeDay;
import models.enumerate.EpasParam;
import models.enumerate.EpasParam.EpasParamValueType.LocalTimeInterval;

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

  private static StampModificationType fixedStampModificationType = null;

  public PersonDay personDay;
  public IWrapperPersonDay wrPersonDay;
  public Optional<WorkingTimeTypeDay> wttd;
  public LocalTimeInterval lunchInterval;
  public LocalTimeInterval workInterval;
  public boolean ignoreDay = false;
  public boolean firstDay = false;
  public List<StampingTemplate> stampingsTemplate;
  
  // visualizzazioni particolari da spostare
  public String workTimeExplanation = "";
  public String mealTicket;

  public String todayLunchTimeCode = "";
  public String fixedWorkingTimeCode = "";
  public String exitingNowCode = "";

  public List<String> note = Lists.newArrayList();

  /**
   * Costruisce l'oggetto contenente un giorno lavorativo da visualizzare nel tabellone timbrature.
   *
   * @param personDayManager        injected
   * @param personManager           injected
   * @param stampTypeManager        injected
   * @param wrapperFactory          injected
   * @param workingTimeTypeDao      injected
   * @param configurationManager    injected
   * @param personDay               personDay
   * @param numberOfInOut           numero di colonne del tabellone a livello mensile.
   * @param monthContracts          il riepiloghi del mese
   */
  public PersonStampingDayRecap(PersonDayManager personDayManager, PersonManager personManager,
                                StampingTemplateFactory stampingTemplateFactory,
                                StampTypeManager stampTypeManager, IWrapperFactory wrapperFactory,
                                WorkingTimeTypeDao workingTimeTypeDao,
                                ConfigurationManager configurationManager, PersonDay personDay,
                                int numberOfInOut, Optional<List<Contract>> monthContracts) {

    this.personDay = personDay;

    if (personDay.isToday()) {
      log.debug("Instanziato PersonStampingDayRecap relativo al giorno corrente.");
    }

    if (!personDay.isPersistent()) {
      this.personDay.setHoliday(personManager
          .isHoliday(personDay.getPerson(), personDay.getDate()));
    }
   
    this.wrPersonDay = wrapperFactory.create(personDay);

    this.stampingsTemplate = getStampingsTemplate(wrPersonDay, stampingTemplateFactory, 
        personDayManager, numberOfInOut);
    
    this.note.addAll(getStampingsNote(this.stampingsTemplate));

    this.wttd = this.wrPersonDay.getWorkingTimeTypeDay();

    this.lunchInterval = (LocalTimeInterval)configurationManager.configValue(
        personDay.getPerson().office, EpasParam.LUNCH_INTERVAL, personDay.getDate());
    this.workInterval = (LocalTimeInterval)configurationManager.configValue(
        personDay.getPerson().office, EpasParam.WORK_INTERVAL, personDay.getDate());
    
    boolean thereAreAllDayAbsences = personDayManager.isAllDayAbsences(personDay);
    
    if (wrPersonDay.isFixedTimeAtWork() && !personDay.isHoliday && !thereAreAllDayAbsences) {
      if (fixedStampModificationType == null) {
        fixedStampModificationType =
            stampTypeManager.getStampMofificationType(
                StampModificationTypeCode.FIXED_WORKINGTIME);
      }
      this.fixedWorkingTimeCode = fixedStampModificationType.code;
    }
    
    this.computeWorkTime(personDay.getTimeAtWork());

    // lunch (p,e) (parte obsoleta che serve per i mesi antichi di IIT)
    if (personDay.getStampModificationType() != null && !personDay.isFuture()) {
      this.todayLunchTimeCode = personDay.getStampModificationType().code;
    }
    
    // uscita adesso f
    if (personDay.isToday() && !personDay.isHoliday && thereAreAllDayAbsences) {
      StampModificationType smt =
          stampTypeManager.getStampMofificationType(
              StampModificationTypeCode.ACTUAL_TIME_AT_WORK);
      this.exitingNowCode = smt.code;
    }

    // is sourceContract (solo se monthContracts presente)
    if (monthContracts.isPresent()) {
      for (Contract contract : monthContracts.get()) {
        
        // Se il giorno è:
        // Precedente all'inizio del contratto
        // Oppure precedente a un'inizializzazione definita
        // Oppure precedente alla data di inserimento della persona
        // v.iene Ignorato
         
        if (contract.getBeginDate().isAfter(personDay.getDate()) 
            || (contract.getSourceDateResidual() != null 
            && !personDay.getDate().isAfter(contract.getSourceDateResidual())) 
            || personDay.getDate().isBefore(personDay.getPerson().createdAt.toLocalDate())) {
          this.ignoreDay = true;
        }

        if (contract.getBeginDate().isEqual(personDay.getDate())) {
          this.firstDay = true;
        }
      }
    }
    
    this.computeMealTicket(personDay, personDayManager, thereAreAllDayAbsences);
  }


  /**
   * Imposta il valore della colonna buono pasto nel tabellone timbrature.
   *
   * @param mealTicket ottenuto si/no
   */
  private void computeMealTicket(PersonDay personDay, PersonDayManager personDayManager, 
      boolean thereAreAllDayAbsences) {

    if (this.ignoreDay || !this.personDay.isPersistent()) {
      this.mealTicket = MEALTICKET_EMPTY;
      return;
    }
    if (personDay.isHoliday()) {
      this.mealTicket = MEALTICKET_EMPTY;
      return;
    }
    // GIORNI FUTURI
    if (personDay.isFuture()) {
      if (thereAreAllDayAbsences) {
        this.mealTicket = MEALTICKET_NO;
      } else {
        this.mealTicket = MEALTICKET_EMPTY;
      }
      return;
    }
    // Giorni Passati e giorno attuale
    if (personDay.isTicketAvailable()) {
      if (personDay.isToday()) {
        if (thereAreAllDayAbsences) {
          this.mealTicket = MEALTICKET_NO;
        } else {
          this.mealTicket = MEALTICKET_NOT_YET;
        }
      }
      this.mealTicket = MEALTICKET_YES;
      return;
    }
    if (!personDay.isTicketAvailable) {
      if (personDay.isPast()) {
        this.mealTicket = MEALTICKET_NO;
      }
      else if (personDay.isToday() || !thereAreAllDayAbsences) {
        this.mealTicket = MEALTICKET_NOT_YET;
      } else {
        this.mealTicket = MEALTICKET_NO;
      }
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
      StampingTemplateFactory stampingTemplateFactory, PersonDayManager personDayManager, 
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
   * Formatta il valore del tempo lavorato nel giorno (lordo comprensivo di tempo decurtato).
   *
   * @param workTime minuti lavorati
   */
  private void computeWorkTime(int workTime) {

    if (personDay.stampingsTime != null && personDay.stampingsTime > 0) {
      this.workTimeExplanation = this.workTimeExplanation
          + "<br>Da timbrature: " 
          + stronger(DateUtility.fromMinuteToHourMinute(personDay.stampingsTime));
    }
    
    if (personDay.getDecurtedWork() > 0) {
      this.workTimeExplanation = this.workTimeExplanation
          + "<br>Sottratto perchè al di fuori della finestra orario: " 
          + stronger(DateUtility.fromMinuteToHourMinute(personDay.getDecurtedWork()));
    }

    if (personDay.justifiedTimeNoMeal != null && personDay.justifiedTimeNoMeal > 0) {
      this.workTimeExplanation = this.workTimeExplanation
          + "<br>Giustificato da assenze: " 
          + stronger(DateUtility.fromMinuteToHourMinute(personDay.justifiedTimeNoMeal));
    }

    if (personDay.justifiedTimeMeal != null && personDay.justifiedTimeMeal > 0) {
      this.workTimeExplanation = this.workTimeExplanation
          + "<br>Giustificato da assenze che concorrono al calcolo del buono pasto: " 
          + stronger(DateUtility.fromMinuteToHourMinute(personDay.justifiedTimeMeal));
    }

    if (personDay.decurted != null && personDay.decurted > 0) {
      this.workTimeExplanation = this.workTimeExplanation
          + "<br>Sottratto per pausa pranzo assente o troppo breve: " 
          + stronger(DateUtility.fromMinuteToHourMinute(personDay.decurted));
    }
    if (!this.workTimeExplanation.isEmpty()) {
      this.workTimeExplanation =
          "Tempo a lavoro totale: " + stronger(DateUtility.fromMinuteToHourMinute(workTime)) 
          + this.workTimeExplanation;
    }
  }

  private String stronger(String string) {
    return "<strong>" + string + "</strong>";
  }
 
}
