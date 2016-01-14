package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.PersonDayDao;
import dao.PersonShiftDayDao;
import dao.wrapper.IWrapperPersonDay;

import it.cnr.iit.epas.DateUtility;

import lombok.extern.slf4j.Slf4j;

import manager.cache.StampTypeManager;

import models.Absence;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.PersonShiftDay;
import models.StampModificationTypeCode;
import models.Stamping;
import models.Stamping.WayType;
import models.WorkingTimeTypeDay;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.Parameter;
import models.enumerate.StampTypes;
import models.enumerate.Troubles;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@Slf4j
public class PersonDayManager {

  private final PersonDayDao personDayDao;
  private final StampTypeManager stampTypeManager;
  private final ConfGeneralManager confGeneralManager;
  private final PersonDayInTroubleManager personDayInTroubleManager;
  private final ContractMonthRecapManager contractMonthRecapManager;
  private final PersonShiftDayDao personShiftDayDao;

  @Inject
  public PersonDayManager(PersonDayDao personDayDao,
                          StampTypeManager stampTypeManager,
                          AbsenceDao absenceDao,
                          ConfGeneralManager confGeneralManager,
                          PersonDayInTroubleManager personDayInTroubleManager,
                          ContractMonthRecapManager contractMonthRecapManager,
                          PersonShiftDayDao personShiftDayDao) {

    this.personDayDao = personDayDao;
    this.stampTypeManager = stampTypeManager;
    this.confGeneralManager = confGeneralManager;
    this.personDayInTroubleManager = personDayInTroubleManager;
    this.contractMonthRecapManager = contractMonthRecapManager;
    this.personShiftDayDao = personShiftDayDao;
  }

  /**
   * @return true se nel giorno vi e' una assenza giornaliera.
   */
  public boolean isAllDayAbsences(PersonDay pd) {

    for (Absence abs : pd.absences) {
      // TODO: per adesso il telelavoro lo considero come giorno lavorativo
      // normale. Chiedere ai romani.
      if (abs.absenceType.code.equals(AbsenceTypeMapping.TELELAVORO.getCode())) {
        return false;
      } else if (abs.justifiedMinutes == null //eludo PEPE, RITING etc...
          && (abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay)
          || abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AssignAllDay))) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return true se nel giorno c'è un'assenza oraria che giustifica una quantità oraria sufficiente
   *     a decretare la persona "presente" a lavoro.
   */
  public boolean isEnoughHourlyAbsences(PersonDay pd) {

    if (pd.person.qualification.qualification > 3) {
      for (Absence abs : pd.absences) {
        if (abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FourHours)
            || abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FiveHours)
            || abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SixHours)
            || abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SevenHours)) {
          return true;
        }
      }
      return false;
    } else {
      if (pd.absences.size() >= 1) {
        return true;
      } else {
        return false;
      }
    }

  }


  /**
   * True se la persona ha uno dei WorkingTime abilitati al buono pasto.
   */
  private boolean isTicketAvailableForWorkingTime(IWrapperPersonDay pd) {

    Preconditions.checkState(pd.getWorkingTimeTypeDay().isPresent());

    if (pd.getWorkingTimeTypeDay().get().mealTicketEnabled()) {
      return true;
    }
    return false;
  }

  /**
   * Calcola i minuti lavorati nel person day. Assegna il campo isTicketAvailable. //FIXME: questo
   * metodo per motivi di sicurezza non dovrebbe modificare il personDay. Ma dovrebbe esclusivamente
   * fornire risultati. Creare un Message che contenga tutte le decisioni dell'algoritmo e delegare
   * il chiamante alla modifica dei campi del person day.
   *
   * @return il numero di minuti trascorsi a lavoro
   */
  public int getCalculatedTimeAtWork(IWrapperPersonDay pd) {

    Preconditions.checkState(pd.getWorkingTimeTypeDay().isPresent());
    //Perche' in questa funzione si imposta a null lo stampModificationType ??
    pd.getValue().stampModificationType = null;

    int justifiedTimeAtWork = 0;

    //Se hanno il tempo di lavoro fissato non calcolo niente
    if (pd.isFixedTimeAtWork()) {

      if (pd.getValue().isHoliday) {
        return 0;
      }
      return pd.getWorkingTimeTypeDay().get().workingTime;
    }

    for (Absence abs : pd.getValue().absences) {

      if (abs.absenceType.code.equals(AbsenceTypeMapping.TELELAVORO.getCode())) {
        return pd.getWorkingTimeTypeDay().get().workingTime;
      }

      //Questo e' il caso del codice 105BP che garantisce sia l'orario di lavoro che il buono pasto
      if (abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AssignAllDay)) {
        setIsTickeAvailable(pd, true);
        return pd.getWorkingTimeTypeDay().get().workingTime;
      }

      // Caso di assenza giornaliera.
      if (abs.justifiedMinutes == null && //evito i PEPE, RITING etc...
          abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay)) {

        setIsTickeAvailable(pd, false);
        return 0;
      }

      // Giustificativi grana minuti (priorità sugli altri casi)
      if (abs.justifiedMinutes != null) {
        justifiedTimeAtWork += abs.justifiedMinutes;
        continue;
      }

      if (abs.absenceType.justifiedTimeAtWork.minutes != null) {

        justifiedTimeAtWork += abs.absenceType.justifiedTimeAtWork.minutes;
        continue;
      }
      if (abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.HalfDay) {

        justifiedTimeAtWork += pd.getWorkingTimeTypeDay().get().workingTime / 2;
        continue;
      }
    }

    //se non c'è almeno una coppia di timbrature considero il justifiedTimeAtWork
    //(che però non contribuisce all'attribuzione del buono che quindi è certamente non assegnato)
    if (pd.getValue().stampings.size() < 2) {
      setIsTickeAvailable(pd, false);
      return justifiedTimeAtWork;
    }

    //Minuti derivanti dalle timbrature
    int stampingMinutes = stampingMinutes(pd.getValue());

    if (pd.getValue().isHoliday) {
      setIsTickeAvailable(pd, false);
      return justifiedTimeAtWork + stampingMinutes;
    }

    //IL PRANZO E' SERVITOOOOO????

    // Ticket non previsto dall'orario di lavoro.
    WorkingTimeTypeDay wttd = pd.getWorkingTimeTypeDay().get();
    if (!wttd.mealTicketEnabled()) {

      setIsTickeAvailable(pd, false);
      return stampingMinutes + justifiedTimeAtWork;
    }

    // Calcolo ...

    int minBreakTicketTime = wttd.breakTicketTime;    //30 minuti

    List<PairStamping> gapLunchPairs = getGapLunchPairs(pd.getValue());
    int effectiveTimeSpent = 0;

    // 1) Calcolo del tempo passato in pausa pranzo dalle timbrature.

    // DA CONFIGURARE /////////////////////////////////////////////////////
    boolean baessoAlgorithm = true;
    for (PairStamping lunchPair : gapLunchPairs) {
      if (lunchPair.prPair) {
        baessoAlgorithm = false;
      }
    }

    if (baessoAlgorithm) {
      if (gapLunchPairs.size() > 0) {
        // recupero la durata della pausa pranzo fatta
        effectiveTimeSpent = gapLunchPairs.get(0).timeInPair;
      }
    } else {
      // sommo tutte le pause marcate come pr
      for (PairStamping lunchPair : gapLunchPairs) {
        if (lunchPair.prPair) {
          effectiveTimeSpent += lunchPair.timeInPair;
        }
      }
    }
    ///////////////////////////////////////////////////////////////////////

    //2) Calcolo l'eventuale differenza tra la pausa fatta e la pausa minima
    int missingTime = minBreakTicketTime - effectiveTimeSpent;
    if (missingTime < 0) {
      missingTime = 0;
    }

    //3) Decisioni

    int mealTicketTime = wttd.mealTicketTime;            //6 ore

    // Non ho eseguito il tempo minimo per buono pasto.
    if (stampingMinutes - missingTime < mealTicketTime) {
      setIsTickeAvailable(pd, false);
      return stampingMinutes + justifiedTimeAtWork;
    }

    // Calcolo tempo decurtato per pausa troppo breve.
    int workingTimeDecurted = stampingMinutes;
    if (missingTime > 0) {
      workingTimeDecurted = stampingMinutes - missingTime;
    }

    // Controllo pausa pomeridiana (solo se la soglia è definita)
    if (!isAfternoonThresholdConditionSatisfied(computeValidPairStampings(pd.getValue()),
        pd.getWorkingTimeTypeDay().get())) {

      setIsTickeAvailable(pd, false);
      return stampingMinutes + justifiedTimeAtWork;
    }

    // Decidere quando verrà il momento di fare i conti con gianvito...
    //if( !isGianvitoConditionSatisfied(workingTimeDecurted, justifiedTimeAtWork,
    //  pd.getValue().date, pd.getPersonDayContract().get(),
    //  pd.getWorkingTimeTypeDay().get()) ){
    //
    //  setIsTickeAvailable(pd, false);
    //  return workTime + justifiedTimeAtWork;
    //}


    // IL BUONO PASTO E' STATO ATTRIBUITO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    setIsTickeAvailable(pd, true);

    // marcatori: versione p ed e
    ////e
    //if(effectiveTimeSpent > 0 && workingTimeDecurted < workTime) {
    //  pd.getValue().stampModificationType = stampTypeManager.getStampMofificationType(
    //    StampModificationTypeCode.FOR_MIN_LUNCH_TIME);
    //}
    ////p
    //if(effectiveTimeSpent == 0) {
    //  pd.getValue().stampModificationType = stampTypeManager.getStampMofificationType(
    //    StampModificationTypeCode.FOR_DAILY_LUNCH_TIME);
    //}

    // marcatori: versione con solo e salvataggio del tempo decurtato.
    //e
    pd.getValue().decurted = null;
    if (workingTimeDecurted < stampingMinutes) {

      pd.getValue().decurted = missingTime;
    }

    return workingTimeDecurted + justifiedTimeAtWork;

  }

  /**
   * La condizione del lavoro minimo pomeridiano è soddisfatta?.
   */
  private boolean isAfternoonThresholdConditionSatisfied(List<PairStamping> validPairs,
                                                         WorkingTimeTypeDay wttd) {

    if (wttd.ticketAfternoonThreshold == null) {
      return true;
    }

    int threshold = wttd.ticketAfternoonThreshold;
    int workingTimeInThreshold = 0;

    for (PairStamping pair : validPairs) {

      int inMinute = DateUtility.toMinute(pair.in.date);
      int outMinute = DateUtility.toMinute(pair.out.date);

      if (inMinute <= threshold && outMinute >= threshold) {
        workingTimeInThreshold += outMinute - threshold;
      }

      if (inMinute >= threshold) {
        workingTimeInThreshold += outMinute - inMinute;
      }
    }
    if (workingTimeInThreshold >= wttd.ticketAfternoonWorkingTime) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * La condizione Gianvito è una condizione che deteriorerà l'efficienza! Per attribuire il buono
   * pasto oltre a soddisfare le soglie di tempo di lavoro minimo per buono pasto e il tempo di
   * lavoro minimo pomeridiano, devo anche avere giustificativi e/o flessibilità sufficiente a
   * raggiungere il tempo di orario giornaliero.
   *
   *<p>Per fare questo conteggio devo calcolarmi la situazione residua al giorno precedente che non
   * è persistita. Per tornare ad essere efficienti si deve injettare nella populate person day la
   * situazione calcolata al giorno precedente.</p>
   *
   * <p>Secondo me nascono delle problematiche significative nel caso in cui volessi inserire dei
   * riposi compensativi nel passato. Questo comporterebbe il ricalcolo dei giorni e potrebbe
   * risultare cambiata in negativo una decisione passata di attribuzione del buono pasto. Per
   * ovviare a questa evenienza si dovrebbe effettuare un check di ogni giorno successivo al riposo
   * compensativo da inserire e verificare che con tale assenza inserita non si va mai in negativo.
   * Ma questo fatto potrebbe essere intercettato dal dipendente che si vede attribuire il riposo
   * compensativo e cancellare il buono pasto e quindi potrebbe contattare la segreteria.</p>
   *
   * <p>Una soluzione a ogni problema potrebbe essere quella di persistere nel db in ogni personDay
   * la situazione di flessibilità raggiunta - residui anno passato, anno corrente e buoni pasto.
   * In questo modo l'utente anche visivamente avrebbe la percezione di quello che sta succedendo
   * nel giorno.</p>
   */
  public boolean isGianvitoConditionSatisfied(
      int workingTimeDecurted, int justifiedTimeAtWork, LocalDate date, Contract contract,
      WorkingTimeTypeDay wttd) {

    // - Ho il tempo di lavoro (eventualmente decurtato) che raggiunge il tempo di lavoro
    //   giornaliero.
    // ok buono pasto
    if (workingTimeDecurted >= wttd.workingTime) {
      return true;
    }

    // - Ho il tempo di lavoro (eventualmente decurtato) che non raggiunge il tempo di lavoro
    //   giornaliero.
    // - Aggiungo PEPE
    //     - livello raggiunto ok buono pasto
    //TODO: capire se in justifiedTimeAtWork posso considerare tutte le assenze
    // orarie o solo PEPE e gravi motivi personali.
    if (workingTimeDecurted + justifiedTimeAtWork >= wttd.workingTime) {
      return true;
    }

    log.info(contract.person.toString() + date);
    //Ultimo tentativo con flessibilità
    int flexibility = contractMonthRecapManager
        .getMinutesForCompensatoryRest(contract, date.minusDays(1), new ArrayList<Absence>());
    if (workingTimeDecurted + justifiedTimeAtWork + flexibility >= wttd.workingTime) {
      return true;
    }

    return false;
  }


  /**
   * FIXME: metodo usato in days per protime. Rimuoverlo e trovare una soluzione furba perchè non è
   * più mantenuto. Calcola i minuti lavorati nel person day. Assegna il campo isTicketAvailable.
   *
   * @return il numero di minuti trascorsi a lavoro
   */
  @Deprecated
  public int workingMinutes(IWrapperPersonDay pd) {

    Preconditions.checkState(pd.getWorkingTimeTypeDay().isPresent());

    int workTime = 0;

    // Se hanno il tempo di lavoro fissato non calcolo niente
    if (pd.isFixedTimeAtWork()) {
      return pd.getWorkingTimeTypeDay().get().workingTime;
    }

    if (!pd.getValue().isHoliday && pd.getValue().stampings.size() >= 2) {

      Collections.sort(pd.getValue().stampings);

      List<PairStamping> validPairs = computeValidPairStampings(pd.getValue());

      for (PairStamping validPair : validPairs) {

        workTime = workTime - DateUtility.toMinute(validPair.in.date);
        workTime = workTime + DateUtility.toMinute(validPair.out.date);
      }

      // Il pranzo e' servito??
      WorkingTimeTypeDay wttd = pd.getWorkingTimeTypeDay().get();

      // se mealTicketTime è zero significa che il dipendente nel giorno
      // non ha diritto al calcolo del buono pasto
      if (!wttd.mealTicketEnabled()) {

        setIsTickeAvailable(pd, false);
        return workTime;
      }

      int mealTicketTime = wttd.mealTicketTime; // 6 ore
      int breakTicketTime = wttd.breakTicketTime; // 30 minuti
      int breakTimeDiff = breakTicketTime;
      pd.getValue().stampModificationType = null;
      List<PairStamping> gapLunchPairs = getGapLunchPairs(pd.getValue());

      if (gapLunchPairs.size() > 0) {
        // recupero la durata della pausa pranzo fatta
        int minTimeForLunch = gapLunchPairs.get(0).timeInPair;
        // Calcolo l'eventuale differenza tra la pausa fatta e la pausa
        // minima
        breakTimeDiff = (breakTicketTime - minTimeForLunch <= 0) ? 0
            : (breakTicketTime - minTimeForLunch);
      }

      if (workTime - breakTimeDiff >= mealTicketTime) {

        if (!pd.getValue().isTicketForcedByAdmin
            || pd.getValue().isTicketForcedByAdmin
            && pd.getValue().isTicketAvailable) {
          workTime = workTime - breakTimeDiff;
        }

        // caso in cui non sia stata effettuata una pausa pranzo
        if (breakTimeDiff == breakTicketTime) {

          pd.getValue().stampModificationType =
              stampTypeManager.getStampMofificationType(
                  StampModificationTypeCode.FOR_DAILY_LUNCH_TIME);
        } else if (breakTimeDiff > 0 && breakTimeDiff != breakTicketTime) {
          // Caso in cui la pausa pranzo fatta è inferiore a quella minima

          pd.getValue().stampModificationType =
              stampTypeManager.getStampMofificationType(
                  StampModificationTypeCode.FOR_MIN_LUNCH_TIME);
        }
      }

      setIsTickeAvailable(pd, false);

    }

    return workTime;
  }

  /**
   * 1) Setta il campo valid per ciascuna stamping del personDay (sulla base del loro valore al
   * momento della call) <br> 2) Associa ogni stamping alla coppia valida individuata se presente
   * (campo stamping.pairId) <br>
   *
   * @param pd il PersonDay.
   */
  public void setValidPairStampings(PersonDay pd) {

    if (!pd.stampings.isEmpty()) {

      computeValidPairStampings(pd);
    }
  }

  /**
   * Costruisce la lista di coppie di timbrature (uscita/entrata) che rappresentano le potenziali
   * pause pranzo.<br> L'algoritmo filtra le coppie che appartengono alla fascia pranzo in
   * configurazione. <br> Nel caso in cui una sola timbratura appartenga alla fascia pranzo,
   * l'algoritmo provvede a ricomputare il timeInPair della coppia assumendo la timbratura al di
   * fuori della fascia uguale al limite di tale fascia. (Le timbrature vengono tuttavia mantenute
   * originali per garantire l'usabilità anche ai controller che gestiscono reperibilità e turni)
   *
   * @param pd personDay.
   */
  public List<PairStamping> getGapLunchPairs(PersonDay pd) {

    List<PairStamping> validPairs = computeValidPairStampings(pd);
    Integer mealTimeStartHour = confGeneralManager
        .getIntegerFieldValue(Parameter.MEAL_TIME_START_HOUR, pd.person.office);
    Integer mealTimeStartMinute = confGeneralManager
        .getIntegerFieldValue(Parameter.MEAL_TIME_START_MINUTE, pd.person.office);
    Integer mealTimeEndHour = confGeneralManager
        .getIntegerFieldValue(Parameter.MEAL_TIME_END_HOUR, pd.person.office);
    Integer mealTimeEndMinute = confGeneralManager
        .getIntegerFieldValue(Parameter.MEAL_TIME_END_MINUTE, pd.person.office);
    LocalDateTime startLunch = new LocalDateTime()
        .withYear(pd.date.getYear())
        .withMonthOfYear(pd.date.getMonthOfYear())
        .withDayOfMonth(pd.date.getDayOfMonth())
        .withHourOfDay(mealTimeStartHour)
        .withMinuteOfHour(mealTimeStartMinute);

    LocalDateTime endLunch = new LocalDateTime()
        .withYear(pd.date.getYear())
        .withMonthOfYear(pd.date.getMonthOfYear())
        .withDayOfMonth(pd.date.getDayOfMonth())
        .withHourOfDay(mealTimeEndHour)
        .withMinuteOfHour(mealTimeEndMinute);

    List<PairStamping> allGapPairs = new ArrayList<PairStamping>();

    //1) Calcolare tutte le gapPair
    Stamping outForLunch = null;
    for (PairStamping validPair : validPairs) {
      if (outForLunch == null) {
        outForLunch = validPair.out;
      } else {
        allGapPairs.add(new PairStamping(outForLunch, validPair.in));
        outForLunch = validPair.out;
      }
    }

    //2) selezionare quelle che appartengono alla fascia pranzo,
    // nel calcolo del tempo limare gli estremi a tale fascia se necessario
    List<PairStamping> gapPairs = new ArrayList<PairStamping>();
    for (PairStamping gapPair : allGapPairs) {
      LocalDateTime out = gapPair.out.date;
      LocalDateTime in = gapPair.in.date;
      boolean isInIntoMealTime = in.isAfter(startLunch.minusMinutes(1))
          && in.isBefore(endLunch.plusMinutes(1));
      boolean isOutIntoMealTime = out.isAfter(startLunch.minusMinutes(1))
          && out.isBefore(endLunch.plusMinutes(1));

      if (isInIntoMealTime || isOutIntoMealTime) {
        LocalDateTime inForCompute = gapPair.in.date;
        LocalDateTime outForCompute = gapPair.out.date;
        if (!isInIntoMealTime) {
          inForCompute = startLunch;
        }
        if (!isOutIntoMealTime) {
          outForCompute = endLunch;
        }
        int timeInPair = 0;
        timeInPair = timeInPair - DateUtility.toMinute(inForCompute);
        timeInPair = timeInPair + DateUtility.toMinute(outForCompute);
        gapPair.timeInPair = timeInPair;
        gapPairs.add(gapPair);
      }

    }

    return gapPairs;
  }

  /**
   * Assegna il numero di minuti in cui una persona è stata a lavoro in quella data.
   */
  public void updateTimeAtWork(IWrapperPersonDay pd) {

    pd.getValue().timeAtWork = getCalculatedTimeAtWork(pd);
  }

  /**
   * Popola il campo difference del PersonDay.
   */
  public void updateDifference(IWrapperPersonDay pd) {

    Preconditions.checkState(pd.getWorkingTimeTypeDay().isPresent());

    //persona fixed
    if (pd.isFixedTimeAtWork() && pd.getValue().timeAtWork == 0) {
      pd.getValue().difference = 0;
      return;
    }

    //festivo
    if (pd.getValue().isHoliday) {
      if (pd.getValue().acceptedHolidayWorkingTime) {
        pd.getValue().difference = pd.getValue().timeAtWork;
      } else {
        pd.getValue().difference = 0;
      }
      return;
    }

    //assenze giornaliere
    if (isAllDayAbsences(pd.getValue())) {
      pd.getValue().difference = 0;
      return;
    }

    //feriale
    pd.getValue().difference = pd.getValue().timeAtWork
        - pd.getWorkingTimeTypeDay().get().workingTime;
  }


  /**
   * Popola il campo progressive del PersonDay.
   */
  public void updateProgressive(IWrapperPersonDay pd) {

    //primo giorno del mese o del contratto
    if (!pd.getPreviousForProgressive().isPresent()) {

      pd.getValue().progressive = pd.getValue().difference;
      return;
    }

    //caso generale
    pd.getValue().progressive = pd.getValue().difference
        + pd.getPreviousForProgressive().get().progressive;

  }

  /**
   * Popola il campo isTicketAvailable.
   */
  public void updateTicketAvailable(IWrapperPersonDay pd) {

    //caso forced by admin
    if (pd.getValue().isTicketForcedByAdmin) {
      return;
    }

    //caso persone fixed
    if (pd.isFixedTimeAtWork()) {
      if (pd.getValue().isHoliday) {
        pd.getValue().isTicketAvailable = false;
      } else if (!pd.getValue().isHoliday && !isAllDayAbsences(pd.getValue())) {
        pd.getValue().isTicketAvailable = true;
      } else if (!pd.getValue().isHoliday && isAllDayAbsences(pd.getValue())) {
        pd.getValue().isTicketAvailable = false;
      }
      return;
    }

    //caso persone normali
    pd.getValue().isTicketAvailable =
        pd.getValue().isTicketAvailable && isTicketAvailableForWorkingTime(pd);
    return;
  }

  /**
   * Setta il valore della variabile isTicketAvailable solo se isTicketForcedByAdmin è false.
   */
  public void setIsTickeAvailable(IWrapperPersonDay pd, boolean isTicketAvailable) {

    if (!pd.getValue().isTicketForcedByAdmin) {

      pd.getValue().isTicketAvailable = isTicketAvailable;
    }
  }

  /**
   * Stessa logica di populatePersonDay ma senza persistere i calcoli (usato per il giorno di
   * oggi).
   */
  public void queSeraSera(IWrapperPersonDay pd) {
    //Strutture dati transienti necessarie al calcolo
    if (!pd.getPersonDayContract().isPresent()) {
      return;
    }

    updateTimeAtWork(pd);
    updateDifference(pd);
    updateProgressive(pd);
    updateTicketAvailable(pd);

  }


  /**
   * Verifica che nel person day vi sia una situazione coerente di timbrature. Situazioni errate si
   * verificano nei casi: (1) che vi sia almeno una timbratura non accoppiata logicamente con
   * nessun'altra timbratura (2) che le persone not fixed non presentino ne' assenze AllDay ne'
   * timbrature. In caso di situazione errata viene aggiunto un record nella tabella
   * PersonDayInTrouble. Se il PersonDay era presente nella tabella PersonDayInTroubled ed è stato
   * fixato, viene settato a true il campo fixed.
   */
  public void checkForPersonDayInTrouble(IWrapperPersonDay pd) {

    Preconditions.checkState(pd.getPersonDayContract().isPresent());

    // Una mappa contenente tutti i problemi del giorno da inserire o rimuovere.
    // Il booleano associato al Trouble e' settato a TRUE se il problema e' presente,
    // a FALSE se invece non e' presente
    final Map<Troubles, Boolean> troubles = Maps.newHashMap();

    //se prima o uguale a source contract il problema è fixato
    if (pd.getPersonDayContract().get().sourceDateResidual != null) {

      if (!pd.getValue().date.isAfter(pd.getPersonDayContract().get().sourceDateResidual)) {

        for (PersonDayInTrouble pdt : pd.getValue().troubles) {
          pd.getValue().troubles.remove(pdt);
          pdt.delete();
          log.info("Fixato {} perchè precedente a sourceContract({})",
              pd.getValue().date, pd.getPersonDayContract().get().sourceDateResidual);
        }
        return;
      }
    }

    //persona fixed
    if (pd.isFixedTimeAtWork()) {

      setValidPairStampings(pd.getValue());

      if (allValidStampings(pd.getValue())) {
        troubles.put(Troubles.UNCOUPLED_FIXED, Boolean.FALSE);
      } else {
        troubles.put(Troubles.UNCOUPLED_FIXED, Boolean.TRUE);
      }
    } else {

      //persona not fixed

      //caso no festa, no assenze, no timbrature
      if (!isAllDayAbsences(pd.getValue()) && pd.getValue().stampings.isEmpty()
          && !pd.getValue().isHoliday && !isEnoughHourlyAbsences(pd.getValue())) {

        troubles.put(Troubles.NO_ABS_NO_STAMP, Boolean.TRUE);
      } else {
        troubles.put(Troubles.NO_ABS_NO_STAMP, Boolean.FALSE);
      }

      //caso no festa, no assenze, timbrature disaccoppiate
      if (!pd.getValue().isHoliday && !isAllDayAbsences(pd.getValue())) {

        setValidPairStampings(pd.getValue());

        if (allValidStampings(pd.getValue())) {
          troubles.put(Troubles.UNCOUPLED_WORKING, Boolean.FALSE);
        } else {
          troubles.put(Troubles.UNCOUPLED_WORKING, Boolean.TRUE);
        }
      } else if (!isAllDayAbsences(pd.getValue()) && pd.getValue().isHoliday) {
        //caso festa, no assenze, timbrature disaccoppiate

        setValidPairStampings(pd.getValue());

        if (allValidStampings(pd.getValue())) {
          troubles.put(Troubles.UNCOUPLED_HOLIDAY, Boolean.FALSE);
        } else {
          troubles.put(Troubles.UNCOUPLED_HOLIDAY, Boolean.TRUE);
        }
      }
    }

    //INSERIMENTO/RIMOZIONE dei personDayInTrouble
    for (Troubles t : troubles.keySet()) {
      //Se valore e' true e quindi inserisco il personDayinTrouble
      if (troubles.get(t)) {
        personDayInTroubleManager.setTrouble(
            pd.getValue(), t);
      } else {
        personDayInTroubleManager.fixTrouble(pd.getValue(), t);
      }
    }

  }

  /**
   * Verifica se il giorno è in trouble.
   *
   * @return true se il person day è in trouble
   */
  public boolean isInTrouble(PersonDay pd) {

    return !pd.troubles.isEmpty();
  }

  /**
   * La lista delle timbrature del personDay modificata con: <br> (1) l'inserimento di una
   * timbratura null nel caso in cui esistano due timbrature consecutive di ingresso o di uscita,
   * mettendo tale timbratura nulla in mezzo alle due <br> (2) l'inserimento di una timbratura di
   * uscita fittizia nel caso di today se la persona risulta dentro il CNR non di servizio per
   * calcolare il tempo di lavoro provvisorio <br> (3) l'inserimento di timbrature null per arrivare
   * alla dimensione del numberOfInOut
   *
   * @param wrPersonDay   personDay
   * @param numberOfInOut numero minimo di coppie  da visualizzare.
   * @return lista di stampings per il template.
   */
  public List<Stamping> getStampingsForTemplate(IWrapperPersonDay wrPersonDay,
                                                int numberOfInOut) {

    PersonDay personDay = wrPersonDay.getValue();

    if (personDay.isToday()) {
      //aggiungo l'uscita fittizia 'now' nel caso risulti dentro il cnr non di servizio
      boolean lastStampingIsIn = false;

      Collections.sort(personDay.stampings);

      for (Stamping stamping : personDay.stampings) {
        if (!StampTypes.MOTIVI_DI_SERVIZIO.equals(stamping.stampType)) {
          if (stamping.isOut()) {
            lastStampingIsIn = false;
          } else {

            lastStampingIsIn = true;
          }
        }
      }
      if (lastStampingIsIn) {
        Stamping stampingExitingNow = new Stamping(personDay, LocalDateTime.now());
        stampingExitingNow.way = WayType.out;
        stampingExitingNow.markedByAdmin = false;
        stampingExitingNow.exitingNow = true;
        personDay.stampings.add(stampingExitingNow);
        queSeraSera(wrPersonDay);
        personDay.stampings.remove(stampingExitingNow);
      }
    }
    List<Stamping> stampingsForTemplate = new ArrayList<Stamping>();
    boolean isLastIn = false;

    for (Stamping s : personDay.stampings) {
      //sono dentro e trovo una uscita
      if (isLastIn && s.way == WayType.out) {
        //salvo l'uscita
        stampingsForTemplate.add(s);
        isLastIn = false;
        continue;
      }
      //sono dentro e trovo una entrata
      if (isLastIn && s.way == WayType.in) {
        //creo l'uscita fittizia
        Stamping stamping = new Stamping(personDay, null);
        stamping.way = WayType.out;
        stampingsForTemplate.add(stamping);
        //salvo l'entrata
        stampingsForTemplate.add(s);
        isLastIn = true;
        continue;
      }

      //sono fuori e trovo una entrata
      if (!isLastIn && s.way == WayType.in) {
        //salvo l'entrata
        stampingsForTemplate.add(s);
        isLastIn = true;
        continue;
      }

      //sono fuori e trovo una uscita
      if (!isLastIn && s.way == WayType.out) {
        //creo l'entrata fittizia
        Stamping stamping = new Stamping(personDay, null);
        stamping.way = WayType.in;
        stampingsForTemplate.add(stamping);
        //salvo l'uscita
        stampingsForTemplate.add(s);
        isLastIn = false;
        continue;
      }
    }
    while (stampingsForTemplate.size() < numberOfInOut * 2) {
      if (isLastIn) {
        //creo l'uscita fittizia
        Stamping stamping = new Stamping(personDay, null);
        stamping.way = WayType.out;
        stampingsForTemplate.add(stamping);
        isLastIn = false;
        continue;
      }
      if (!isLastIn) {
        //creo l'entrata fittizia
        Stamping stamping = new Stamping(personDay, null);
        stamping.way = WayType.in;
        stampingsForTemplate.add(stamping);
        isLastIn = true;
        continue;
      }
    }

    return stampingsForTemplate;
  }

  /**
   * Calcola le coppie di stampings valide al fine del calcolo del time at work. <br>
   *
   * @modify setta il campo stamping.valid di ciascuna stampings contenuta nel personDay.<br>
   * @modify setta il campo stamping.pairId con il valore dalla coppia a cui appartengono.
   */

  public List<PairStamping> computeValidPairStampings(PersonDay personDay) {

    //Lavoro su una copia ordinata.
    List<Stamping> stampings = Lists.newArrayList();
    for (Stamping stamping : personDay.stampings) {
      stampings.add(stamping);
    }
    Collections.sort(stampings);

    //(1)Costruisco le coppie valide per calcolare il worktime
    List<PairStamping> validPairs = new ArrayList<PairStamping>();
    List<Stamping> serviceStampings = new ArrayList<Stamping>();
    Stamping stampEnter = null;

    for (Stamping stamping : stampings) {
      //le stampings di servizio non entrano a far parte del calcolo del work time
      //ma le controllo successivamente
      //per segnalare eventuali errori di accoppiamento e appartenenza a orario di lavoro valido
      if (StampTypes.MOTIVI_DI_SERVIZIO.equals(stamping.stampType)) {
        serviceStampings.add(stamping);
        continue;
      }
      //cerca l'entrata
      if (stampEnter == null) {
        if (stamping.isIn()) {
          stampEnter = stamping;
          continue;
        }
        if (stamping.isOut()) {
          //una uscita prima di una entrata e' come se non esistesse
          stamping.valid = false;
          continue;
        }

      }
      //cerca l'uscita
      if (stampEnter != null) {
        if (stamping.isOut()) {

          PairStamping pair = new PairStamping(stampEnter, stamping);
          validPairs.add(pair);

          stampEnter.valid = true;
          stamping.valid = true;
          stampEnter = null;
          continue;
        }
        //trovo un secondo ingresso, butto via il primo
        if (stamping.isIn()) {
          stampEnter.valid = false;
          stampEnter = stamping;
          continue;
        }
      }
    }
    //(2) scarto le stamping di servizio che non appartengono ad alcuna coppia valida
    List<Stamping> serviceStampingsInValidPair = new ArrayList<Stamping>();
    for (Stamping stamping : serviceStampings) {
      boolean belongToValidPair = false;
      for (PairStamping validPair : validPairs) {
        LocalDateTime outTime = validPair.out.date;
        LocalDateTime inTime = validPair.in.date;
        if (stamping.date.isAfter(inTime) && stamping.date.isBefore(outTime)) {
          belongToValidPair = true;
          break;
        }
      }
      if (belongToValidPair) {
        serviceStampingsInValidPair.add(stamping);
      } else {
        stamping.valid = false;
      }
    }

    //(3)aggrego le stamping di servizio per coppie valide ed eseguo il check di sequenza valida
    for (PairStamping validPair : validPairs) {
      LocalDateTime outTime = validPair.out.date;
      LocalDateTime inTime = validPair.in.date;
      List<Stamping> serviceStampingsInSinglePair = new ArrayList<Stamping>();
      for (Stamping stamping : serviceStampingsInValidPair) {
        if (stamping.date.isAfter(inTime) && stamping.date.isBefore(outTime)) {
          serviceStampingsInSinglePair.add(stamping);
        }
      }
      //check
      Stamping serviceExit = null;
      for (Stamping stamping : serviceStampingsInSinglePair) {
        //cerca l'uscita di servizio
        if (serviceExit == null) {
          if (stamping.isOut()) {
            serviceExit = stamping;
            continue;
          }
          if (stamping.isIn()) {
            //una entrata di servizio prima di una uscita di servizio e' come se non esistesse
            stamping.valid = false;
            continue;
          }
        }
        //cerca l'entrata di servizio
        if (serviceExit != null) {
          if (stamping.isIn()) {
            stamping.valid = true;
            serviceExit.valid = true;
            serviceExit = null;
            continue;
          }
          //trovo una seconda uscita di servizio, butto via la prima
          if (stamping.isOut()) {
            serviceExit.valid = false;
            serviceExit = stamping;
            continue;
          }
        }
      }
    }

    return validPairs;
  }


  /**
   * Utilizzata nel metodo delete del controller Persons per cancellare tutti i personDays relativi.
   * alla persona person
   */
  public void deletePersonDays(Person person) {

    List<PersonDay> helpPdList = personDayDao.getAllPersonDay(person);
    for (PersonDay pd : helpPdList) {

      pd.delete();
      person.personDays.remove(pd);
      person.save();
    }
  }

  /**
   * Calcola il numero massimo di coppie di colonne ingresso/uscita.
   */
  public int getMaximumCoupleOfStampings(List<PersonDay> personDays) {
    int max = 0;
    for (PersonDay pd : personDays) {
      int coupleOfStampings = numberOfInOutInPersonDay(pd);
      if (max < coupleOfStampings) {
        max = coupleOfStampings;
      }
    }
    return max;
  }

  /**
   * Genera una lista di PersonDay aggiungendo elementi fittizzi per coprire ogni giorno del mese.
   */
  public List<PersonDay> getTotalPersonDayInMonth(List<PersonDay> personDays,
                                                  Person person, int year, int month) {

    LocalDate beginMonth = new LocalDate(year, month, 1);
    LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();

    List<PersonDay> totalDays = new ArrayList<PersonDay>();

    int currentWorkingDays = 0;
    LocalDate currentDate = beginMonth;
    while (!currentDate.isAfter(endMonth)) {
      if (currentWorkingDays < personDays.size()
          && personDays.get(currentWorkingDays).date.isEqual(currentDate)) {
        totalDays.add(personDays.get(currentWorkingDays));
        currentWorkingDays++;
      } else {
        PersonDay previusPersonDay = null;
        if (totalDays.size() > 0) {
          previusPersonDay = totalDays.get(totalDays.size() - 1);
        }

        PersonDay newPersonDay;
        //primo giorno del mese festivo
        if (previusPersonDay == null) {
          newPersonDay =
              new PersonDay(
                  person, new LocalDate(year, month, currentDate.getDayOfMonth()), 0, 0, 0);
        } else {
          newPersonDay =
              new PersonDay(
                  person,
                  new LocalDate(year, month, currentDate.getDayOfMonth()), 0, 0,
                  previusPersonDay.progressive);
        }

        totalDays.add(newPersonDay);

      }
      currentDate = currentDate.plusDays(1);
    }
    return totalDays;
  }

  /**
   * Il numero di buoni pasto usabili all'interno della lista di person day passata come parametro.
   */
  public int numberOfMealTicketToUse(List<PersonDay> personDays) {
    int number = 0;
    for (PersonDay pd : personDays) {
      if (pd.isTicketAvailable && !pd.isHoliday) {
        number++;
      }
    }
    return number;
  }


  /**
   * Il numero di buoni pasto da restituire all'interno della lista di person day passata come
   * parametro.
   */
  public int numberOfMealTicketToRender(List<PersonDay> personDays) {
    int ticketTorender = 0;
    for (PersonDay pd : personDays) {

      if (!pd.isTicketAvailable) {
        //i giorni festivi e oggi
        if (pd.isHoliday || pd.isToday()) {
          continue;
        }
        //i giorni futuri in cui non ho assenze
        if (pd.date.isAfter(LocalDate.now()) && pd.absences.isEmpty()) {
          continue;
        }
        ticketTorender++;
      }
    }

    return ticketTorender;
  }

  public boolean isOnMission(PersonDay personDay) {
    return !FluentIterable.from(personDay.absences).filter(
        new Predicate<Absence>() {
          @Override
          public boolean apply(Absence absence) {
            return absence.absenceType.code.equals(AbsenceTypeMapping.MISSIONE.getCode());
          }
        }).isEmpty();
  }

  /**
   * Il numero di coppie ingresso/uscita da stampare per il personday.
   */
  public int numberOfInOutInPersonDay(PersonDay pd) {
    if (pd == null) {
      return 0;
    }
    Collections.sort(pd.stampings);

    int coupleOfStampings = 0;

    String lastWay = null;
    for (Stamping s : pd.stampings) {
      if (lastWay == null) {
        //trovo out chiudo una coppia
        if (s.way.description.equals("out")) {
          coupleOfStampings++;
          lastWay = null;
          continue;
        }
        //trovo in lastWay diventa in
        if (s.way.description.equals("in")) {
          lastWay = s.way.description;
          continue;
        }

      }
      //lastWay in
      if (lastWay.equals("in")) {
        //trovo out chiudo una coppia
        if (s.way.description.equals("out")) {
          coupleOfStampings++;
          lastWay = null;
          continue;
        }
        //trovo in chiudo una coppia e lastWay resta in
        if (s.way.description.equals("in")) {
          coupleOfStampings++;
          continue;
        }
      }
    }
    //l'ultima stampings e' in chiudo una coppia
    if (lastWay != null) {
      coupleOfStampings++;
    }

    return coupleOfStampings;
  }

  /**
   * @return la quantità in eccesso, se c'è, nei giorni in cui una persona è in turno.
   */
  public int getExceedInShift(PersonDay pd) {
    Optional<PersonShiftDay> psd = personShiftDayDao.getPersonShiftDay(pd.person, pd.date);
    if (psd.isPresent()) {
      return pd.difference;
    }
    return 0;
  }


  /**
   * Restituisce la quantita' in minuti del'orario dovuto alle timbrature valide in un giono.
   *
   * @return minuti
   */
  private int stampingMinutes(PersonDay personDay) {

    Preconditions.checkNotNull(personDay);
    Preconditions.checkState(personDay.isPersistent());

    List<PairStamping> validPairs = computeValidPairStampings(personDay);

    int stampingMinutes = 0;
    for (PairStamping validPair : validPairs) {
      stampingMinutes += validPair.timeInPair;
    }

    return stampingMinutes;
  }


  private boolean allValidStampings(PersonDay personDay) {

    return FluentIterable.from(personDay.stampings).filter(
        new Predicate<Stamping>() {
          @Override
          public boolean apply(Stamping input) {
            return !input.valid;
          }
        }).isEmpty();
  }

}
