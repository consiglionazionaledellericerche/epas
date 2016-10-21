package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import dao.PersonDayDao;
import dao.PersonShiftDayDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperPersonDay;

import it.cnr.iit.epas.DateUtility;

import lombok.extern.slf4j.Slf4j;

import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;
import manager.services.PairStamping;

import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.PersonShiftDay;
import models.Stamping;
import models.Stamping.WayType;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.StampTypes;
import models.enumerate.Troubles;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@Slf4j
public class PersonDayManager {

  private final ConfigurationManager configurationManager;
  private final PersonDayInTroubleManager personDayInTroubleManager;
  private final PersonShiftDayDao personShiftDayDao;
  private final PersonDayDao personDayDao;
  private final WorkingTimeTypeDao workingTimeTypeDao;

  /**
   * Costruttore.
   *
   * @param configurationManager      configurationManager
   * @param personDayInTroubleManager personDayInTroubleManager
   * @param personShiftDayDao         personShiftDayDao
   */
  @Inject
  public PersonDayManager(ConfigurationManager configurationManager,
      PersonDayInTroubleManager personDayInTroubleManager, PersonDayDao personDayDao,
      PersonShiftDayDao personShiftDayDao, WorkingTimeTypeDao workingTimeTypeDao) {

    this.configurationManager = configurationManager;
    this.personDayInTroubleManager = personDayInTroubleManager;
    this.personShiftDayDao = personShiftDayDao;
    this.personDayDao = personDayDao;
    this.workingTimeTypeDao = workingTimeTypeDao;
  }

  /**
   * Se nel giorno vi è una assenza giornaliera.
   *
   * @return esito
   */
  public boolean isAllDayAbsences(PersonDay pd) {
    
    for (Absence abs : pd.absences) {
      
      if (abs.justifiedType != null) {
        if (abs.justifiedType.name.equals(JustifiedTypeName.all_day) 
            || abs.justifiedType.name.equals(JustifiedTypeName.assign_all_day)) {
          return true;
          
        }
        continue;
      }
          
      // TODO: per adesso il telelavoro lo considero come giorno lavorativo
      // normale. Chiedere ai romani.
      // TODO: il telelavoro è giorno di lavoro da casa, quindi non giustifica 
      // ma assegna tempo a lavoro, ma non assegna buono pasto.
      //if (abs.absenceType.code.equals(AbsenceTypeMapping.TELELAVORO.getCode())) {
      //  return false;
      //} else 
      if (abs.justifiedMinutes == null //eludo PEPE, RITING etc...
          && (abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay)
          || abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AssignAllDay))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Se le assenze orarie giustificano abbanstanza per ritenere il dipendente a lavoro.
   *
   * @return esito.
   */
  public boolean isEnoughHourlyAbsences(IWrapperPersonDay pd) {

    Preconditions.checkState(pd.getWorkingTimeTypeDay().isPresent());

    // Calcolo i minuti giustificati dalle assenze.
    int justifiedTime = 0;
    for (Absence abs : pd.getValue().absences) {

      if (abs.justifiedType != null) {
        if (abs.justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
          justifiedTime = justifiedTime + abs.justifiedMinutes;
          continue;
        }
        if (abs.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
          justifiedTime = justifiedTime + abs.absenceType.justifiedTime;
          continue;
        }
        continue;
      }
      
      if (abs.absenceType.justifiedTimeAtWork.minutes != null) {
        justifiedTime = justifiedTime + abs.absenceType.justifiedTimeAtWork.minutes;
      }
    }

    // Livelli VI - VIII
    if (pd.getValue().person.qualification.qualification > 3) {
      return pd.getWorkingTimeTypeDay().get().workingTime / 2 < justifiedTime;
    } else {
      // Livelli I - III (decidere meglio)
      if (pd.getValue().absences.size() >= 1) {
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * Pulisce la parte di calcolo del tempo al lavoro.
   */
  private void cleanTimeAtWork(PersonDay pd) {

    setTicketStatusIfNotForced(pd, false);

    pd.setStampModificationType(null);
    pd.setDecurted(0);
    pd.setStampingsTime(0);
    pd.setJustifiedTimeMeal(0);
    pd.setJustifiedTimeNoMeal(0);
    pd.setTimeAtWork(0);
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

      int inMinute = DateUtility.toMinute(pair.first.date);
      int outMinute = DateUtility.toMinute(pair.second.date);

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
   * Costruisce la lista di coppie di timbrature (uscita/entrata) che rappresentano le potenziali
   * pause pranzo.<br>
   * L'algoritmo filtra le coppie che appartengono alla fascia pranzo passata come parametro.<br>
   * Nel caso in cui una sola timbratura appartenga alla fascia pranzo, l'algoritmo provvede a
   * ricomputare il timeInPair della coppia assumendo la timbratura al di fuori della fascia uguale
   * al limite di tale fascia. (Le timbrature vengono tuttavia mantenute originali per garantire
   * l'usabilità anche ai controller che gestiscono reperibilità e turni).<br>
   *
   * @param personDay  personDay
   * @param startLunch inizio fascia pranzo istituto
   * @param endLunch   fine fascia pranzo istituto
   * @return la lista delle pause pranzo
   */
  public List<PairStamping> getGapLunchPairs(PersonDay personDay, LocalTime startLunch,
      LocalTime endLunch) {

    List<PairStamping> validPairs = computeValidPairStampings(personDay);

    List<PairStamping> allGapPairs = Lists.newArrayList();

    //1) Calcolare tutte le gapPair
    PairStamping previous = null;
    for (PairStamping validPair : validPairs) {
      if (previous != null) {
        if ((previous.second.stampType == null
            || StampTypes.PAUSA_PRANZO.equals(previous.second.stampType))
            && (validPair.first.stampType == null
            || StampTypes.PAUSA_PRANZO.equals(validPair.first.stampType))) {

          allGapPairs.add(new PairStamping(previous.second, validPair.first));
        }
      }
      previous = validPair;
    }

    //2) selezionare quelle che appartengono alla fascia pranzo,
    // nel calcolo del tempo limare gli estremi a tale fascia se necessario
    List<PairStamping> gapPairs = Lists.newArrayList();
    for (PairStamping gapPair : allGapPairs) {
      LocalTime first = gapPair.first.date.toLocalTime();
      LocalTime second = gapPair.second.date.toLocalTime();

      boolean isInIntoMealTime = !first.isBefore(startLunch) && !first.isAfter(endLunch);
      boolean isOutIntoMealTime = !second.isBefore(startLunch) && !second.isAfter(endLunch);

      if (!isInIntoMealTime && !isOutIntoMealTime) {
        if (second.isBefore(startLunch) || first.isAfter(endLunch)) {
          continue;
        }
      }

      LocalTime inForCompute = gapPair.first.date.toLocalTime();
      LocalTime outForCompute = gapPair.second.date.toLocalTime();
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

    return gapPairs;
  }
  
  public int shortPermissionTime(PersonDay personDay) {
    
    if (!StampTypes.PERMESSO_BREVE.isActive()) {
      return 0;
    }
    
    List<PairStamping> validPairs = computeValidPairStampings(personDay);
    
    List<PairStamping> allGapPairs = Lists.newArrayList();
    
    //1) Calcolare tutte le gapPair (fattorizzare col metodo del pranzo)
    PairStamping previous = null;
    for (PairStamping validPair : validPairs) {
      if (previous != null) {
        if ((previous.second.stampType == null
            || StampTypes.PERMESSO_BREVE.equals(previous.second.stampType))
            && (validPair.first.stampType == null
            || StampTypes.PERMESSO_BREVE.equals(validPair.first.stampType))) {

          allGapPairs.add(new PairStamping(previous.second, validPair.first));
        }
      }
      previous = validPair;
    }
    
    int gapTime = 0;
    for (PairStamping gapPair : allGapPairs) {
      gapTime+= gapPair.timeInPair;
    }
    
    return gapTime;
  }

  /**
   * Calcolo del tempo a lavoro e del buono pasto.
   *
   * @param personDay personDay.
   * @param wttd      tipo orario personDay.
   * @return oggetto modificato.
   */
  public PersonDay updateTimeAtWork(PersonDay personDay, WorkingTimeTypeDay wttd,
      boolean fixedTimeAtWork, LocalTime startLunch, LocalTime endLunch,
      LocalTime startWork, LocalTime endWork) {

    // Pulizia stato personDay.
    cleanTimeAtWork(personDay);

    for (Absence abs : personDay.getAbsences()) {

      if (abs.justifiedType != null) {

        if (abs.absenceType.code.equals(AbsenceTypeMapping.TELELAVORO.getCode())) {
          cleanTimeAtWork(personDay);
          personDay.setTimeAtWork(wttd.workingTime);
          return personDay;
        }
        
        //Questo e' il caso del codice 105BP che garantisce sia l'orario di lavoro che il buono pasto
        // TODO: se è il 105BP perchè non controllo direttamente il codice? Mistero della fede.
        if (abs.justifiedType.name.equals(JustifiedTypeName.assign_all_day)) {
          cleanTimeAtWork(personDay);
          setTicketStatusIfNotForced(personDay, true);
          personDay.setTimeAtWork(wttd.workingTime);
          return personDay;
        }
        
        // Caso di assenza giornaliera.
        if (abs.justifiedType.name.equals(JustifiedTypeName.all_day)) {
          cleanTimeAtWork(personDay);
          setTicketStatusIfNotForced(personDay, false);
          personDay.setTimeAtWork(0);
          return personDay;
        }
        
        // Mezza giornata giustificata.
        if (abs.justifiedType.name.equals(JustifiedTypeName.half_day)) {
          personDay.setJustifiedTimeNoMeal(personDay.getJustifiedTimeNoMeal()
              + (wttd.workingTime / 2));
          continue;
        }
        
        // #######
        //  Assenze non giornaliere da cumulare ....

        // Giustificativi grana minuti
        if (abs.justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
          personDay.setJustifiedTimeNoMeal(personDay.getJustifiedTimeNoMeal() + abs.justifiedMinutes);
          continue;
        }
        
        // Giustificativi grana ore (discriminare per calcolo buono o no)
        if (abs.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
          if (abs.absenceType.timeForMealTicket) {
            personDay.setJustifiedTimeMeal(personDay.getJustifiedTimeMeal()
                + abs.absenceType.justifiedTime);
          } else {
            personDay.setJustifiedTimeNoMeal(personDay.getJustifiedTimeNoMeal()
                + abs.absenceType.justifiedTime);
          }
          continue;
        }
        
        
        continue;
      }
      
      // #######
      // Assenze che interrompono il ciclo e azzerano quanto calcolato nelle precedenti.

      if (abs.absenceType.code.equals(AbsenceTypeMapping.TELELAVORO.getCode())) {
        cleanTimeAtWork(personDay);
        personDay.setTimeAtWork(wttd.workingTime);
        return personDay;
      }

      //Questo e' il caso del codice 105BP che garantisce sia l'orario di lavoro che il buono pasto
      if (abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AssignAllDay)) {
        cleanTimeAtWork(personDay);
        setTicketStatusIfNotForced(personDay, true);
        personDay.setTimeAtWork(wttd.workingTime);
        return personDay;
      }

      // Caso di assenza giornaliera.
      if (abs.justifiedMinutes == null && //evito i PEPE, RITING etc...
          abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay)) {
        cleanTimeAtWork(personDay);
        setTicketStatusIfNotForced(personDay, false);
        personDay.setTimeAtWork(0);
        return personDay;
      }

      // #######
      //  Assenze non giornaliere da cumulare ....

      // Giustificativi grana minuti (priorità sugli altri casi) Ex. PEPE
      if (abs.justifiedMinutes != null) {
        personDay.setJustifiedTimeNoMeal(personDay.getJustifiedTimeNoMeal() + abs.justifiedMinutes);
        continue;
      }

      // Giustificativi grana ore (discriminare per calcolo buono o no)
      if (abs.absenceType.justifiedTimeAtWork.minutes != null) {
        if (abs.absenceType.justifiedTimeAtWork.mealTimeCounting) {
          personDay.setJustifiedTimeMeal(personDay.getJustifiedTimeMeal()
              + abs.absenceType.justifiedTimeAtWork.minutes);
        } else {
          personDay.setJustifiedTimeNoMeal(personDay.getJustifiedTimeNoMeal()
              + abs.absenceType.justifiedTimeAtWork.minutes);
        }
        continue;
      }

      // Mezza giornata giustificata.
      if (abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.HalfDay) {
        personDay.setJustifiedTimeNoMeal(personDay.getJustifiedTimeNoMeal()
            + (wttd.workingTime / 2));
        continue;
      }
    }

    // Se hanno il tempo di lavoro fissato non calcolo niente
    if (fixedTimeAtWork) {

      if (personDay.isHoliday()) {
        return personDay;
      }
      personDay.setTimeAtWork(wttd.workingTime);
      return personDay;
    }

    //Le coppie valide
    List<PairStamping> validPairs = computeValidPairStampings(personDay);

    // Minuti derivanti dalle timbrature
    personDay.setStampingsTime(stampingMinutes(validPairs));

    // Minuti effettivi decurtati della fascia istituto
    int workingMinutesInValidPairs = (workingMinutes(validPairs, startWork, endWork));

    // Imposto il tempo a lavoro come somma di tutte le poste calcolate.
    personDay.setTimeAtWork(workingMinutesInValidPairs + personDay.getJustifiedTimeMeal()
        + personDay.getJustifiedTimeNoMeal());

    // Se è festa ho finito ...
    if (personDay.isHoliday()) {
      setTicketStatusIfNotForced(personDay, false);
      return personDay;
    }

    // Se il buono pasto non è previsto dall'orario ho finito ...
    if (!wttd.mealTicketEnabled()) {
      setTicketStatusIfNotForced(personDay, false);
      return personDay;
    }

    // Se il buono pasto è forzato a no non effettuo ulteriori calcoli e ho finito ...
    if (personDay.isTicketForcedByAdmin && !personDay.isTicketAvailable) {
      return personDay;
    }

    // #######################################################################################
    // IL PRANZO E' SERVITOOOOO????
    // Questa parte determina se il buono pasto è ottenuto e la eventuale quantità decurtata
    // dal tempo a lavoro.

    // 1) Calcolo del tempo passato in pausa pranzo dalle timbrature.
    List<PairStamping> gapLunchPairs = getGapLunchPairs(personDay, startLunch, endLunch);
    int effectiveTimeSpent = 0;
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

    //2) Calcolo l'eventuale differenza tra la pausa fatta e la pausa minima
    int minBreakTicketTime = wttd.breakTicketTime;    //30 minuti
    int missingTime = minBreakTicketTime - effectiveTimeSpent;
    if (missingTime < 0) {
      missingTime = 0;
    }

    //3) Decisioni

    int mealTicketTime = wttd.mealTicketTime;            //6 ore
    int mealTicketsMinutes = workingMinutesInValidPairs + personDay.getJustifiedTimeMeal();

    // Non ho eseguito il tempo minimo per buono pasto.
    if (mealTicketsMinutes - missingTime < mealTicketTime) {
      setTicketStatusIfNotForced(personDay, false);
      return personDay;
    }

    // Controllo pausa pomeridiana (solo se la soglia è definita)
    if (!isAfternoonThresholdConditionSatisfied(computeValidPairStampings(personDay), wttd)) {
      setTicketStatusIfNotForced(personDay, false);
      return personDay;
    }

    // Calcolo tempo decurtato per pausa troppo breve.
    int workingTimeDecurted = mealTicketsMinutes;
    if (missingTime > 0) {
      workingTimeDecurted = mealTicketsMinutes - missingTime;
    }

    // Decidere quando verrà il momento di fare i conti con gianvito...
    //if( !isGianvitoConditionSatisfied(workingTimeDecurted, justifiedTimeAtWork,
    //  pd.getValue().date, pd.getPersonDayContract().get(),
    //  pd.getWorkingTimeTypeDay().get()) ){
    //
    //  setIsTickeAvailable(pd, false);
    //  return workTime + justifiedTimeAtWork;
    //}

    // #########
    // IL BUONO PASTO E' STATO ATTRIBUITO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    setTicketStatusIfNotForced(personDay, true);
    personDay.setDecurted(null);
    if (workingTimeDecurted < mealTicketsMinutes) {

      personDay.setDecurted(missingTime);
    }

    personDay.setTimeAtWork(workingTimeDecurted + personDay.getJustifiedTimeNoMeal());
    return personDay;
  }

  /**
   * Popola il campo difference del PersonDay.
   *
   * @param personDay       personDay
   * @param wttd            wttd
   * @param fixedTimeAtWork fixedTimeAtWork
   */
  public void updateDifference(PersonDay personDay, WorkingTimeTypeDay wttd,
      boolean fixedTimeAtWork) {

    //persona fixed
    if (fixedTimeAtWork && personDay.getTimeAtWork() == 0) {
      personDay.setDifference(0);
      return;
    }

    //festivo
    if (personDay.isHoliday()) {
      if (personDay.isAcceptedHolidayWorkingTime()) {
        personDay.setDifference(personDay.getTimeAtWork());
      } else {
        personDay.setDifference(0);
      }
      return;
    }

    //assenze giornaliere
    if (isAllDayAbsences(personDay)) {
      personDay.setDifference(0);
      return;
    }

    //feriale
    personDay.setDifference(personDay.getTimeAtWork() - wttd.workingTime);
  }


  /**
   * Popola il campo progressive del PersonDay.
   */
  public void updateProgressive(PersonDay personDay, Optional<PersonDay> previousForProgressive) {

    //primo giorno del mese o del contratto
    if (!previousForProgressive.isPresent()) {

      personDay.setProgressive(personDay.getDifference());
      return;
    }

    //caso generale
    personDay.setProgressive(personDay.getDifference()
        + previousForProgressive.get().getProgressive());

  }

  /**
   * Popola il campo isTicketAvailable.
   *
   * @param personDay       personDay
   * @param wttd            wttd
   * @param fixedTimeAtWork fixedTimeAtWork
   * @return personDay
   */
  public PersonDay updateTicketAvailable(PersonDay personDay, WorkingTimeTypeDay wttd,
      boolean fixedTimeAtWork) {

    //caso forced by admin
    if (personDay.isTicketForcedByAdmin()) {
      return personDay;
    }

    //caso persone fixed
    if (fixedTimeAtWork) {
      if (personDay.isHoliday()) {
        personDay.setTicketAvailable(false);
      } else if (!personDay.isHoliday() && !isAllDayAbsences(personDay)) {
        personDay.setTicketAvailable(true);
      } else if (!personDay.isHoliday() && isAllDayAbsences(personDay)) {
        personDay.setTicketAvailable(false);
      }
      return personDay;
    }

    //caso persone normali
    personDay.setTicketAvailable(personDay.isTicketAvailable() && wttd.mealTicketEnabled());
    return personDay;
  }

  /**
   * Setta il valore della variabile isTicketAvailable solo se isTicketForcedByAdmin è false.
   *
   * @param pd                personDay
   * @param isTicketAvailable value.
   */
  public void setTicketStatusIfNotForced(PersonDay pd, boolean isTicketAvailable) {

    if (!pd.isTicketForcedByAdmin()) {
      pd.setTicketAvailable(isTicketAvailable);
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

    Preconditions.checkArgument(pd.getWorkingTimeTypeDay().isPresent());

    LocalTimeInterval lunchInterval = (LocalTimeInterval) configurationManager.configValue(
        pd.getValue().person.office, EpasParam.LUNCH_INTERVAL, pd.getValue().getDate());

    LocalTimeInterval workInterval = (LocalTimeInterval) configurationManager.configValue(
        pd.getValue().person.office, EpasParam.WORK_INTERVAL, pd.getValue().getDate());

    updateTimeAtWork(pd.getValue(), pd.getWorkingTimeTypeDay().get(),
        pd.isFixedTimeAtWork(), lunchInterval.from, lunchInterval.to, workInterval.from,
        workInterval.to);

    updateDifference(pd.getValue(), pd.getWorkingTimeTypeDay().get(), pd.isFixedTimeAtWork());

    updateProgressive(pd.getValue(), pd.getPreviousForProgressive());

    updateTicketAvailable(pd.getValue(), pd.getWorkingTimeTypeDay().get(), pd.isFixedTimeAtWork());

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

    setValidPairStampings(pd.getValue());

    if (pd.isFixedTimeAtWork()) {
      //persona fixed
      if (allValidStampings(pd.getValue())) {
        troubles.put(Troubles.UNCOUPLED_FIXED, Boolean.FALSE);
      } else {
        troubles.put(Troubles.UNCOUPLED_FIXED, Boolean.TRUE);
      }
    } else {

      //persona not fixed

      // ### CASO 1
      //caso no festa + no assenze giornaliere + no timbrature + (qualcosa capire cosa) 
      if (!pd.getValue().isHoliday
          && !isAllDayAbsences(pd.getValue())
          && pd.getValue().stampings.isEmpty()
          && !isEnoughHourlyAbsences(pd)) {

        troubles.put(Troubles.NO_ABS_NO_STAMP, Boolean.TRUE);
      } else {
        troubles.put(Troubles.NO_ABS_NO_STAMP, Boolean.FALSE);
      }

      // ### CASO 2
      //caso no festa + no assenze giornaliere + timbrature disaccoppiate
      if (!pd.getValue().isHoliday
          && !isAllDayAbsences(pd.getValue())
          && !allValidStampings(pd.getValue())) {

        troubles.put(Troubles.UNCOUPLED_WORKING, Boolean.TRUE);
      } else {
        troubles.put(Troubles.UNCOUPLED_WORKING, Boolean.FALSE);
      }

      // ### CASO 3
      //caso è festa + no assenze giornaliere + timbrature disaccoppiate
      if (pd.getValue().isHoliday
          && !isAllDayAbsences(pd.getValue())
          && !allValidStampings(pd.getValue())) {

        troubles.put(Troubles.UNCOUPLED_HOLIDAY, Boolean.TRUE);
      } else {
        troubles.put(Troubles.UNCOUPLED_HOLIDAY, Boolean.FALSE);
      }
    }

    //INSERIMENTO/RIMOZIONE dei personDayInTrouble
    for (Troubles trouble : troubles.keySet()) {
      //Se valore e' true e quindi inserisco il personDayinTrouble
      if (troubles.get(trouble)) {
        personDayInTroubleManager.setTrouble(pd.getValue(), trouble);
      } else {
        personDayInTroubleManager.fixTrouble(pd.getValue(), trouble);
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
   * @param wrPersonDay        personDay
   * @param numberOfInOut      numero minimo di coppie  da visualizzare.
   * @param considerExitingNow se voglio considerare la timbratura uscendo adesso.
   * @return lista di stampings per il template.
   */
  public List<Stamping> getStampingsForTemplate(IWrapperPersonDay wrPersonDay, int numberOfInOut,
      boolean considerExitingNow) {

    PersonDay personDay = wrPersonDay.getValue();

    if (personDay.isToday() && considerExitingNow) {
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
        personDay.isConsideredExitingNow = true;
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
    stampings.addAll(personDay.getStampings());
    Collections.sort(stampings);

    //(1)Costruisco le coppie valide per calcolare il worktime
    List<PairStamping> validPairs = Lists.newArrayList();
    List<Stamping> serviceStampings = Lists.newArrayList();
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
    List<Stamping> serviceStampingsInValidPair = Lists.newArrayList();
    for (Stamping stamping : serviceStampings) {
      boolean belongToValidPair = false;
      for (PairStamping validPair : validPairs) {
        LocalDateTime outTime = validPair.second.date;
        LocalDateTime inTime = validPair.first.date;
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
      LocalDateTime outTime = validPair.second.date;
      LocalDateTime inTime = validPair.first.date;
      List<Stamping> serviceStampingsInSinglePair = Lists.newArrayList();
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
   * 1) Setta il campo valid per ciascuna stamping del personDay (sulla base del loro valore al
   * momento della call) <br>
   * 2) Associa ogni stamping alla coppia valida individuata se presente
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

  /**
   * Se la persona è in missione nel giorno.
   *
   * @param personDay giorno
   * @return esito
   */
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
   * @param validPairs coppie valide di timbrature per il tempo a lavoro.
   * @return minuti
   */
  private int stampingMinutes(List<PairStamping> validPairs) {

    // TODO: considerare startWork ed endWork nei minuti a lavoro
    int stampingMinutes = 0;
    for (PairStamping validPair : validPairs) {
      stampingMinutes += validPair.timeInPair;
    }

    return stampingMinutes;
  }

  /**
   * Restituisce la quantita' in minuti del'orario dovuto alle timbrature valide in un giono,
   * che facciano parte della finestra temporale specificata.
   *
   * @param validPairs coppie valide di timbrature per il tempo a lavoro
   * @param startWork  inizio finestra
   * @param endWork    fine finestra
   * @return minuti
   */
  private int workingMinutes(List<PairStamping> validPairs,
      LocalTime startWork, LocalTime endWork) {

    int workingMinutes = 0;

    //Per ogni coppia valida butto via il tempo oltre la fascia.
    for (PairStamping validPair : validPairs) {
      LocalTime consideredStart = new LocalTime(validPair.first.date);
      LocalTime consideredEnd = new LocalTime(validPair.second.date);
      if (consideredEnd.isBefore(startWork)) {
        continue;
      }
      if (consideredStart.isAfter(endWork)) {
        continue;
      }
      if (consideredStart.isBefore(startWork)) {
        consideredStart = startWork;
      }
      if (consideredEnd.isAfter(endWork)) {
        consideredEnd = endWork;
      }
      workingMinutes += DateUtility.toMinute(consideredEnd) - DateUtility.toMinute(consideredStart);
    }

    return workingMinutes;

  }

  /**
   * Se le stampings nel giorno sono tutte valide.
   *
   * @param personDay personDay
   * @return esito
   */
  private boolean allValidStampings(PersonDay personDay) {

    return !personDay.stampings.stream().filter(stamping -> !stamping.isValid()).findAny()
        .isPresent();
  }

  /**
   * Cerca il personDay se non esiste lo crea e lo persiste.
   */
  public PersonDay getOrCreateAndPersistPersonDay(Person person, LocalDate date) {

    Optional<PersonDay> optPersonDay = personDayDao.getPersonDay(person, date);
    if (optPersonDay.isPresent()) {
      return optPersonDay.get();
    }
    PersonDay personDay = new PersonDay(person, date);
    personDay.isHoliday = isHoliday(person, date);
    personDay.create();
    return personDay;
  }

  /**
   * Se il giorno è festivo per la persona.
   * @param person
   * @param date
   * @return
   */
  public boolean isHoliday(Person person, LocalDate date) {

    //Festività generale
    MonthDay patron = (MonthDay) configurationManager
        .configValue(person.office, EpasParam.DAY_OF_PATRON, date);
    if (DateUtility.isGeneralHoliday(Optional.fromNullable(patron), date)) {
      return true;
    }

    Optional<WorkingTimeType> workingTimeType = workingTimeTypeDao.getWorkingTimeType(date, person);
    
    //persona fuori contratto
    if (!workingTimeType.isPresent()) {
      return false;
    }

    //tempo a lavoro
    return workingTimeTypeDao.isWorkingTypeTypeHoliday(date, workingTimeType.get());
  }
  
  /**
   * I giorni festivi della persona nella finestra specificata.
   * @param person
   * @param from
   * @param to
   * @return
   */
  public List<LocalDate> holidays(Person person, LocalDate from, LocalDate to) {

    List<LocalDate> holidays = Lists.newArrayList();

    LocalDate date = from;
    while (!date.isAfter(to)) {
      if (isHoliday(person, date)) {
        holidays.add(date);
        date = date.plusDays(1);
      }
    }
    return holidays;
  }

}
