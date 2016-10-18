package manager.recaps.personstamping;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;

import lombok.extern.slf4j.Slf4j;

import manager.PersonDayManager;
import manager.cache.StampTypeManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;

import models.Contract;
import models.PersonDay;
import models.StampModificationType;
import models.StampModificationTypeCode;
import models.Stamping;
import models.WorkingTimeTypeDay;

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
  private static final String MEALTICKET_YES_IF_EXIT_NOW = "YES_IF_EXIT_NOW";
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
  public String mealTicket;

  public StampModificationType fixedWorkingTimeCode = null;
  public String exitingNowCode = "";

  public List<String> note = Lists.newArrayList();

  /**
   * Costruisce l'oggetto contenente un giorno lavorativo da visualizzare nel tabellone timbrature.
   *
   * @param personDayManager     injected
   * @param stampTypeManager     injected
   * @param wrapperFactory       injected
   * @param configurationManager injected
   * @param personDay            personDay
   * @param numberOfInOut        numero di colonne del tabellone a livello mensile.
   * @param considerExitingNow   se considerare nel calcolo l'uscita in questo momento
   * @param monthContracts       il riepiloghi del mese
   */
  public PersonStampingDayRecap(PersonDayManager personDayManager,
      StampingTemplateFactory stampingTemplateFactory,
      StampTypeManager stampTypeManager, IWrapperFactory wrapperFactory,
      ConfigurationManager configurationManager, PersonDay personDay,
      int numberOfInOut, boolean considerExitingNow,
      Optional<List<Contract>> monthContracts) {

    this.personDay = personDay;

    if (personDay.isToday()) {
      log.debug("Istanziato PersonStampingDayRecap relativo al giorno corrente.");
    }

    //FIXME è stato rimossa la condizione che comportava il calcolo dei festivi solo sui
    // personDay non persistenti per rattoppare la visualizzazione in quei casi in cui i personDay
    // sono stati persistiti senza calcolare questa informazione.
    // Appena l'informazione persistita diventerà affidabile si può reinserire la condizione.
    this.personDay.setHoliday(personDayManager
        .isHoliday(personDay.getPerson(), personDay.getDate()));

    this.wrPersonDay = wrapperFactory.create(personDay);

    this.stampingsTemplate = getStampingsTemplate(wrPersonDay, stampingTemplateFactory,
        personDayManager, numberOfInOut, considerExitingNow);

    this.note.addAll(getStampingsNote(this.stampingsTemplate));

    this.wttd = this.wrPersonDay.getWorkingTimeTypeDay();

    this.lunchInterval = (LocalTimeInterval) configurationManager.configValue(
        personDay.getPerson().office, EpasParam.LUNCH_INTERVAL, personDay.getDate());
    this.workInterval = (LocalTimeInterval) configurationManager.configValue(
        personDay.getPerson().office, EpasParam.WORK_INTERVAL, personDay.getDate());

    boolean thereAreAllDayAbsences = personDayManager.isAllDayAbsences(personDay);

    if (wrPersonDay.isFixedTimeAtWork() && !personDay.isHoliday && !thereAreAllDayAbsences) {
      if (fixedStampModificationType == null) {
        fixedStampModificationType =
            stampTypeManager.getStampMofificationType(
                StampModificationTypeCode.FIXED_WORKINGTIME);
      }
      this.fixedWorkingTimeCode = fixedStampModificationType;
    }

//    this.computeWorkTime(personDay.getTimeAtWork());

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
            || personDay.getDate().isBefore(personDay.getPerson().beginDate)) {
          this.ignoreDay = true;
        }

        if (contract.getBeginDate().isEqual(personDay.getDate())) {
          this.firstDay = true;
        }
      }
    }

    this.computeMealTicket(personDay, thereAreAllDayAbsences);
  }


  /**
   * Imposta il valore della colonna buono pasto nel tabellone timbrature.
   *
   * @param personDay              giorno sul quale impostare il valore relativo al buono
   * @param thereAreAllDayAbsences specifica se sono presenti assenze giornaliere
   */
  private void computeMealTicket(PersonDay personDay, boolean thereAreAllDayAbsences) {

    // ##### Giorno ignorato (fuori contratto)

    if (this.ignoreDay || !this.personDay.isPersistent()) {
      this.mealTicket = MEALTICKET_EMPTY;
      return;
    }

    // ##### Giorno festivo

    if (personDay.isHoliday() && !personDay.acceptedHolidayWorkingTime 
        && !personDay.isTicketForcedByAdmin) {
      this.mealTicket = MEALTICKET_EMPTY;
      return;
    }

    // ##### Giorni futuri

    if (personDay.isFuture()) {
      if (thereAreAllDayAbsences) {
        this.mealTicket = MEALTICKET_NO;
      } else {
        this.mealTicket = MEALTICKET_EMPTY;
      }
      return;
    }

    // ##### Giorni Passati e giorno attuale
    // ##### Available

    if (personDay.isTicketAvailable()) {
      if (personDay.isTicketForcedByAdmin) {
        // si e forzato
        this.mealTicket = MEALTICKET_YES;
      } else if (personDay.isToday()) {
        if (thereAreAllDayAbsences) {
          // si non forzato oggi con assenze giornalire FIXME: perchè decido qua no?
          this.mealTicket = MEALTICKET_NO;
        } else {
          if (personDay.isConsideredExitingNow()) {
            // si non forzato oggi considerando l'uscita in questo momento
            this.mealTicket = MEALTICKET_YES_IF_EXIT_NOW;
          } else {
            // si non forzato oggi senza considerare l'uscita in questo momento
            this.mealTicket = MEALTICKET_YES;
          }
        }
      } else {
        // si non forzato giorni passati
        this.mealTicket = MEALTICKET_YES;
      }
      return;
    }

    // ##### Giorni Passati e giorno attuale
    // ##### Not Available

    if (!personDay.isTicketAvailable) {
      if (personDay.isTicketForcedByAdmin) {
        // no forzato
        this.mealTicket = MEALTICKET_NO;
      } else {
        if (personDay.isPast()) {
          // no non forzato giorni passati
          this.mealTicket = MEALTICKET_NO;
        } else if (personDay.isToday() || !thereAreAllDayAbsences) {
          // no non forzato oggi senza assenze giornaliere
          this.mealTicket = MEALTICKET_NOT_YET;
        } else {
          // no non forzato oggi con assenze giornaliere
          this.mealTicket = MEALTICKET_NO;
        }
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
      int numberOfInOut, boolean considerExitingNow) {

    List<Stamping> stampings = personDayManager
        .getStampingsForTemplate(wrPersonDay, numberOfInOut, considerExitingNow);

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

}
