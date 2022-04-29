/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.inject.Inject;
import dao.ContractDao;
import dao.PersonDayDao;
import dao.PersonShiftDayDao;
import dao.WorkingTimeTypeDao;
import dao.ZoneDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperPersonDay;
import it.cnr.iit.epas.DateUtility;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
import models.TimeSlot;
import models.WorkingTimeTypeDay;
import models.ZoneToZones;
import models.absences.Absence;
import models.absences.JustifiedBehaviour.JustifiedBehaviourName;
import models.absences.JustifiedType.JustifiedTypeName;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.MealTicketBehaviour;
import models.enumerate.StampTypes;
import models.enumerate.Troubles;
import org.assertj.core.util.Strings;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import play.jobs.Job;

/**
 * Manager per la gestione dei PersonDay.
 */
@Slf4j
public class PersonDayManager {

  private final ConfigurationManager configurationManager;
  private final PersonDayInTroubleManager personDayInTroubleManager;
  private final PersonShiftDayDao personShiftDayDao;
  private final PersonDayDao personDayDao;
  private final WorkingTimeTypeDao workingTimeTypeDao;
  private final ZoneDao zoneDao;
  private final ContractDao contractDao;
  private final AbsenceComponentDao absenceComponentDao;


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
      PersonShiftDayDao personShiftDayDao, WorkingTimeTypeDao workingTimeTypeDao, ZoneDao zoneDao,
      AbsenceComponentDao absenceComponentDao, ContractDao contractDao) {

    this.configurationManager = configurationManager;
    this.personDayInTroubleManager = personDayInTroubleManager;
    this.personShiftDayDao = personShiftDayDao;
    this.personDayDao = personDayDao;
    this.workingTimeTypeDao = workingTimeTypeDao;
    this.zoneDao = zoneDao;
    this.absenceComponentDao = absenceComponentDao;
    this.contractDao = contractDao;
  }
  
  /**
   * Assenza che impedisce la maturazione del buono pasto.
   */
  public Optional<Absence> getPreventMealTicket(PersonDay personDay) {
    for (Absence absence : personDay.absences) {
      if (absence.absenceType.mealTicketBehaviour.equals(MealTicketBehaviour.preventMealTicket)) { 
        return Optional.of(absence);
      }
    }
    return Optional.absent();
  }

  /**
   * Assenza che assegna l'intera giornata.
   */
  public Optional<Absence> getAssignAllDay(PersonDay personDay) {
    for (Absence absence : personDay.absences) {
      if (absence.justifiedType.name.equals(JustifiedTypeName.assign_all_day)) { 
        return Optional.of(absence);
      }
    }
    return Optional.absent();
  }

  /**
   * Assenza che giustifica l'intera giornata.
   */
  public Optional<Absence> getAllDay(PersonDay personDay) {
    for (Absence absence : personDay.absences) {
      if (absence.justifiedType.name.equals(JustifiedTypeName.all_day)
          || absence.justifiedType.name.equals(JustifiedTypeName.recover_time)) { 
        return Optional.of(absence);
      }
    }
    return Optional.absent();
  }


  /**
   * Metodo che controlla se esiste un'assenza a completamento giornaliero nella giornata.
   *
   * @param personDay il personDay in cui cercare l'assenza
   * @return Assenza che giustifica il tempo che manca al raggiungimento dell'orario di lavoro.
   */
  public Optional<Absence> getCompleteDayAndAddOvertime(PersonDay personDay) {
    for (Absence absence : personDay.absences) {
      if (absence.justifiedType.name.equals(JustifiedTypeName.complete_day_and_add_overtime)) {
        return Optional.of(absence);
      }
    }
    return Optional.absent();
  }

  /**
   * Se nel giorno vi è una che assegna o giustifica l'intera giornata.
   */
  public boolean isAllDayAbsences(PersonDay personDay) {
    return getAllDay(personDay).isPresent() || getAssignAllDay(personDay).isPresent();

  }

  /**
   * Metodo che ritorna true se l'assenza è compatibile con la reperibilità, 
   * false altrimenti.
   *
   * @param personDay il personDay su cui cercare le assenze
   * @return true se l'assenza è compatibile con la reperibilità, false altrimenti.
   */
  public boolean isAbsenceCompatibleWithReperibility(PersonDay personDay) {

    return personDay.absences.stream()
        .noneMatch(abs -> !abs.absenceType.reperibilityCompatible);

  }

  /**
   * Metodo che ritorna true se nel giorno c'è un assenza a completamento giornaliero,
   * false altrimeni.
   *
   * @param personDay il personday da verificare
   * @return se nel giorno vi è un'assenza a completamento giornaliero.
   */
  public boolean isCompleteDayAndAddOvertimeAbsence(PersonDay personDay) {
    return getCompleteDayAndAddOvertime(personDay).isPresent();
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
        if (abs.justifiedType.name == JustifiedTypeName.specified_minutes) {
          justifiedTime += abs.justifiedMinutes;
          continue;
        }
        if (abs.justifiedType.name == JustifiedTypeName.absence_type_minutes) {
          justifiedTime += abs.absenceType.justifiedTime;
          continue;
        }
        //TODO: quando ci sarà considerare il quello che manca.
        continue;
      }
    }

    // Livelli VI - VIII
    if (pd.getValue().person.qualification.qualification > 3) {
      return pd.getWorkingTimeTypeDay().get().workingTime / 2 < justifiedTime;
    } else {
      // Livelli I - III (decidere meglio)
      return pd.getValue().absences.size() >= 1;
    }
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
    return workingTimeInThreshold >= wttd.ticketAfternoonWorkingTime;
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
   * @param exitingNow timbratura fittizia uscendo in questo momento
   * @return la lista delle pause pranzo
   */
  public List<PairStamping> getGapLunchPairs(PersonDay personDay, LocalTime startLunch,
      LocalTime endLunch, Optional<Stamping> exitingNow) {

    List<PairStamping> validPairs = getValidPairStampings(personDay.stampings, exitingNow);

    List<PairStamping> allGapPairs = Lists.newArrayList();

    //1) Calcolare tutte le gapPair
    PairStamping previous = null;
    //Verifico che esistano timbrature appartenenti a zone differenti per non conteggiarle ai 
    //fini delle coppie valide per la pausa pranzo

    for (PairStamping valid : validPairs) {
      if (previous != null) {
        if ((previous.second.stampType == null
            || previous.second.stampType.isGapLunchPairs())
            && (valid.first.stampType == null
            || valid.first.stampType.isGapLunchPairs())) {
          Optional<ZoneToZones> zoneToZones = 
              zoneDao.getByLinkNames(previous.second.stampingZone, valid.first.stampingZone);
          if (zoneToZones.isPresent()) {
            if (!isTimeInDelay(previous, valid, zoneToZones)) {
              allGapPairs.add(new PairStamping(previous.second, valid.first));
            }
          } else {
            allGapPairs.add(new PairStamping(previous.second, valid.first));
          }             
        }
      }
      previous = valid;
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
      timeInPair -= DateUtility.toMinute(inForCompute);
      timeInPair += DateUtility.toMinute(outForCompute);
      gapPair.timeInPair = timeInPair;
      gapPairs.add(gapPair);
    }

    return gapPairs;
  }

  /**
   * Calcola il tempo di permesso breve nel giorno.
   *
   * @param personDay personDay
   * @return minuti
   */
  public int shortPermissionTime(PersonDay personDay) {

    if (!StampTypes.PERMESSO_BREVE.isActive()) {
      return 0;
    }
    List<PairStamping> validPairs = computeValidPairStampings(personDay.stampings);

    int gapTime = 0;
    //1) Calcolare tutte le gapPair (fattorizzare col metodo del pranzo)
    PairStamping previous = null;
    for (PairStamping validPair : validPairs) {
      if (previous != null) {
        Stamping first = previous.second;
        Stamping second = validPair.first;
        //almeno una delle due permesso breve
        if ((first.stampType != null && first.stampType == StampTypes.PERMESSO_BREVE) 
            || (second.stampType != null && second.stampType == StampTypes.PERMESSO_BREVE)) {
          //solo permessi brevi
          if ((first.stampType == null || first.stampType == StampTypes.PERMESSO_BREVE)
              && (second.stampType == null || second.stampType == StampTypes.PERMESSO_BREVE)) {
            gapTime += new PairStamping(first, second).timeInPair;
          }
        }
      }
      previous = validPair;
    }
    return gapTime;
  }

  /**
   * Calcolo del tempo a lavoro e del buono pasto.
   *
   * @param personDay personDay
   * @param wttd tipo orario
   * @param fixedTimeAtWork se la persona ha la presenza automatica
   * @param startLunch inizio fascia pranzo
   * @param endLunch fine fascia pranzo
   * @param startWork inizio apertura sede
   * @param endWork fine apertura sede
   * @param exitingNow timbratura fittizia uscendo in questo momento 
   * @return personDay modificato
   */
  public PersonDay updateTimeAtWork(PersonDay personDay, WorkingTimeTypeDay wttd,
      boolean fixedTimeAtWork, LocalTime startLunch, LocalTime endLunch,
      LocalTime startWork, LocalTime endWork, Optional<Stamping> exitingNow) {

    //Preconditions
    for (Absence abs : personDay.getAbsences()) {
      Preconditions.checkNotNull(abs.justifiedType);
    }

    // Pulizia stato personDay.
    setTicketStatusIfNotForced(personDay, MealTicketBehaviour.notAllowMealTicket);
    personDay.setStampModificationType(null);     //Target personDay: p(-30), e(<30), f(now), d(fix)
    personDay.setOnHoliday(0);
    personDay.setOutOpening(0);
    personDay.setStampingsTime(0);
    personDay.setJustifiedTimeMeal(0);
    personDay.setJustifiedTimeNoMeal(0);
    personDay.setTimeAtWork(0);

    // Patch persone fixed
    if (fixedTimeAtWork) {
      return updateTimeAtWorkFixed(personDay, wttd);
    }

    // Gli invarianti del calcolo.
    //
    //   1) Tempo timbrature -> Tempo fra coppie ritenute valide.
    //   2) Tempo dentro la fascia -> N.B nei giorni di festa è zero
    //   3) Tempo fuori fascia -> Tempo timbrature - Tempo dentro fascia
    //   4) Tempo di festa -> Tempo timbrature giorno di festa

    List<PairStamping> validPairs = getValidPairStampings(personDay.stampings, exitingNow);

    personDay.setStampingsTime(stampingMinutes(validPairs));
    int stampingTimeInOpening = workingMinutes(validPairs, startWork, endWork);

    if (personDay.isHoliday) {
      stampingTimeInOpening = 0;
      personDay.setOnHoliday(personDay.getStampingsTime());
    } else {
      personDay.setOutOpening(personDay.getStampingsTime() - stampingTimeInOpening);  
    }

    //Caso assenza che assegna l'intera giornata ex 103, 103BP, 105BP
    Optional<Absence> assignAllDay = getAssignAllDay(personDay);
    if (assignAllDay.isPresent()) {
      personDay.setTimeAtWork(wttd.workingTime);
      setTicketStatusIfNotForced(personDay, assignAllDay.get().absenceType.mealTicketBehaviour);
      return personDay;
    }

    //Caso assenza giornaliera
    if (getAllDay(personDay).isPresent()) {
      personDay.setTimeAtWork(0);
      setTicketStatusIfNotForced(personDay, getAllDay(personDay).get()
          .absenceType.mealTicketBehaviour);
      return personDay;
    }
    
    if (getPreventMealTicket(personDay).isPresent()) {
      setTicketStatusIfNotForced(personDay, MealTicketBehaviour.preventMealTicket);
      return personDay;
    }
    

    //Giustificativi a grana minuti nel giorno
    for (Absence abs : personDay.getAbsences()) {

      //Numero di minuti giustificati
      int justifiedMinutes = 0;
      if (abs.justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
        justifiedMinutes = abs.justifiedMinutes;
      } else if (abs.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
        justifiedMinutes = abs.absenceType.justifiedTime;
      } else {
        continue;
      }

      //Assenza festiva
      if (personDay.isHoliday) {
        personDay.setOnHoliday(personDay.getOnHoliday() + justifiedMinutes);
        continue;
      } 

      //Assegnamento se contribuisce al buono pasto
      if (abs.absenceType.mealTicketBehaviour.equals(MealTicketBehaviour.allowMealTicket)) {
        personDay.setJustifiedTimeMeal(personDay.getJustifiedTimeMeal() + justifiedMinutes);
        continue;
      }

      personDay.setJustifiedTimeNoMeal(personDay.getJustifiedTimeNoMeal() + justifiedMinutes);
    }

    /*Qui inizia il pezzo aggiunto che controlla la provenienza delle timbrature*/

    int justifiedTimeBetweenZones = 0;

    List<ZoneToZones> link = personDay.person.getZones();

    if (!link.isEmpty() && validPairs.size() > 1) {      
      justifiedTimeBetweenZones = justifiedTimeBetweenZones(validPairs, startWork, endWork);
    }
    personDay.setJustifiedTimeBetweenZones(justifiedTimeBetweenZones);    

    //Il tempo a lavoro calcolato
    int computedTimeAtWork = stampingTimeInOpening //nei festivi è 0
        + personDay.getJustifiedTimeMeal()
        + personDay.getJustifiedTimeNoMeal()
        + personDay.getApprovedOnHoliday()
        + personDay.getApprovedOutOpening()
        + personDay.getJustifiedTimeBetweenZones();

    //TODO: il tempo ricavato deve essere persistito sul personDay su un nuovo campo
    // così posso sfruttare quel campo nel tabellone timbrature

    personDay.setTimeAtWork(computedTimeAtWork);

    mealTicketHandlerAndDecurtedMeal(personDay, wttd, stampingTimeInOpening, 
        startLunch, endLunch, exitingNow);

    //Gestione decurtazione.
    // Si applica solo se non ci sono assenze orarie che maturano il buono
    // o se il dipendente e' in missione oraria nella giornata da valutare
    if (isOnHourlyMission(personDay) || personDay.getJustifiedTimeMeal() <= 0) {
      personDay.setTimeAtWork(personDay.getTimeAtWork() - personDay.getDecurtedMeal());
    } else {
      personDay.setDecurtedMeal(0);
    }

    // Il caso di assenze a giustificazione "quello che manca"
    if (getCompleteDayAndAddOvertime(personDay).isPresent()) {      
      int missingTime = wttd.workingTime - personDay.getTimeAtWork();
      if (personDay.isHoliday) {
        //Nel caso "quello che manca", tipicamente per le missioni non si permette l'attivazione
        //delle ore da timbrature nel festivo perché gestite tramite le ore aggiuntive in missione.
        //Quindi si azzerano le ore onHoliday.
        personDay.setOnHoliday(0);
        //Il tempo a lavoro nei festivi è già impostato a 0.
        //personDay.setTimeAtWork(0);
      } else {
        if (missingTime < 0) {
          personDay.setTimeAtWork(personDay.getTimeAtWork());

        } else {
          //Time at work è quelle delle timbrature meno la pausa pranzo
          personDay.setTimeAtWork(personDay.getTimeAtWork() + missingTime);
        }
        if (!personDay.isTicketAvailable && getCompleteDayAndAddOvertime(personDay)
            .get().getAbsenceType().mealTicketBehaviour
            .equals(MealTicketBehaviour.allowMealTicket)) {
          personDay.isTicketAvailable = true;
        } 
      }

    }

    //Controllo se ho del tempo aggiuntivo dovuto al lavoro in missione da sommare al tempo a lavoro
    if (personDay.getWorkingTimeInMission() != null && personDay.getWorkingTimeInMission() != 0) {
      personDay.setTimeAtWork(personDay.getTimeAtWork() + personDay.getWorkingTimeInMission());
    }

    return personDay;
  }

  /**
   * Algoritmo del calcolo buono pasto. Una volta accertato che:
   * 1) Non ci sono assenze giornaliere.
   * 2) La persona non ha la timbratura automatica.
   */
  private PersonDay mealTicketHandlerAndDecurtedMeal(PersonDay personDay, WorkingTimeTypeDay wttd, 
      int stampingTimeInOpening, LocalTime startLunch, LocalTime endLunch, 
      Optional<Stamping> exitingNow) {

    // Reset
    personDay.setDecurtedMeal(0);

    // Buono pasto forzato
    if (personDay.isTicketForcedByAdmin) {
      return personDay;
    }

    // Giorno festivo: default false
    if (personDay.isHoliday()) {
      setTicketStatusIfNotForced(personDay, MealTicketBehaviour.notAllowMealTicket);
      return personDay;
    }

    // Il tipo orario non prevede il buono: default false
    if (!wttd.mealTicketEnabled()) {
      setTicketStatusIfNotForced(personDay, MealTicketBehaviour.notAllowMealTicket);
      return personDay;
    }

    // 1) Calcolo del tempo passato in pausa pranzo dalle timbrature.
    List<PairStamping> gapLunchPairs = 
        getGapLunchPairs(personDay, startLunch, endLunch, exitingNow);
    boolean baessoAlgorithm = true;
    for (PairStamping lunchPair : gapLunchPairs) {
      if (lunchPair.prPair) {
        baessoAlgorithm = false;
      }
    }
    int effectiveTimeSpent = 0;
    if (baessoAlgorithm) {
      if (!gapLunchPairs.isEmpty()) {
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
    int toCut = minBreakTicketTime - effectiveTimeSpent;
    if (toCut < 0) {
      toCut = 0;
    }

    //3) Decisioni

    int mealTicketTime = wttd.mealTicketTime;            //6 ore
    int mealTicketsMinutes = stampingTimeInOpening 
        + personDay.getApprovedOutOpening() 
        + personDay.getJustifiedTimeMeal();

    // Non ho eseguito il tempo minimo per buono pasto.
    if (mealTicketsMinutes - toCut < mealTicketTime) {
      setTicketStatusIfNotForced(personDay, MealTicketBehaviour.notAllowMealTicket);
      return personDay;
    }

    // Controllo pausa pomeridiana (solo se la soglia è definita)
    if (!isAfternoonThresholdConditionSatisfied(
        computeValidPairStampings(personDay.stampings), wttd)) {
      setTicketStatusIfNotForced(personDay, MealTicketBehaviour.notAllowMealTicket);
      return personDay;
    }

    // Il buono pasto è stato maturato
    setTicketStatusIfNotForced(personDay, MealTicketBehaviour.allowMealTicket);

    // Assegnamento tempo decurtato per pausa troppo breve.
    if (toCut > 0) {
      personDay.setDecurtedMeal(toCut);
    }

    return personDay;
  }

  /**
   * Le persone fixed hanno una gestione particolare per quanto riguarda tempo a lavoro e 
   * buono pasto. 
   */
  private PersonDay updateTimeAtWorkFixed(PersonDay personDay, WorkingTimeTypeDay wttd) {

    //In caso di giorno festivo niente tempo a lavoro ne festivo.
    if (personDay.isHoliday()) {
      personDay.setTimeAtWork(0);
      setTicketStatusIfNotForced(personDay, MealTicketBehaviour.notAllowMealTicket);
      return personDay;
    }

    if (getAllDay(personDay).isPresent() 
        && getAllDay(personDay).get().absenceType.mealTicketBehaviour
        .equals(MealTicketBehaviour.notAllowMealTicket)
        || (getAssignAllDay(personDay).isPresent() 
            && getAssignAllDay(personDay).get().absenceType.mealTicketBehaviour
            .equals(MealTicketBehaviour.notAllowMealTicket))
        || (getCompleteDayAndAddOvertime(personDay).isPresent() 
            && getCompleteDayAndAddOvertime(personDay).get().absenceType.mealTicketBehaviour
            .equals(MealTicketBehaviour.notAllowMealTicket))) {
      setTicketStatusIfNotForced(personDay, MealTicketBehaviour.notAllowMealTicket);
    } else {
      setTicketStatusIfNotForced(personDay, MealTicketBehaviour.allowMealTicket);
    }

    if (getAllDay(personDay).isPresent()
        || getCompleteDayAndAddOvertime(personDay).isPresent()) {
      personDay.setTimeAtWork(0);
      return personDay;
    }

    personDay.setTimeAtWork(wttd.workingTime);

    return personDay;
  }


  /**
   * Metodo che controlla il tempo giustificato dalle zone di timbratura.
   *
   * @param validPairs la lista di coppie di timbrature valide
   * @param startWork l'orario di inizio lavoro in sede
   * @param endWork l'orario di fine lavoro in sede
   * @return il quantitativo che viene giustificato timbrando uscita/ingresso su zone 
   *     appartenenti a un link.
   */
  private int justifiedTimeBetweenZones(List<PairStamping> validPairs, 
      LocalTime startWork, LocalTime endWork) {

    int timeToJustify = 0;
    PairStamping temp = null;
    for (PairStamping validPair : validPairs) {
      if (temp != null && stampingsBetweenZones(temp, validPair)) {
        Optional<ZoneToZones> zoneToZones = 
            zoneDao.getByLinkNames(temp.second.stampingZone, validPair.first.stampingZone);
        if (zoneToZones.isPresent()) {

          Range<LocalTime> range = Range.closed(temp.second.date.toLocalTime(), 
              validPair.first.date.toLocalTime());
          if (range.lowerEndpoint().isBefore(startWork) 
              && !range.upperEndpoint().isBefore(startWork)) {
            range.span(Range.closed(startWork, validPair.first.date.toLocalTime()));
          }
          if (isTimeInDelay(temp, validPair, zoneToZones)) {
            timeToJustify = timeToJustify 
                + (DateUtility.toMinute(range.upperEndpoint()) 
                    - DateUtility.toMinute(range.lowerEndpoint()));
          } else {
            timeToJustify = timeToJustify + zoneToZones.get().delay;
          }             
        }
      }
      temp = validPair;
    }

    return timeToJustify;
  }

  /**
   * Metodo che controlla se il gap tra due timbrature avviene tra due zone di timbratura diverse.
   *
   * @param first la prima coppia di timbrature
   * @param second la seconda coppia di timbrature
   * @return true se il gap tra le due coppie è fatto tra due zone di timbratura associate.
   */
  private boolean stampingsBetweenZones(PairStamping first, PairStamping second) {

    if (!Strings.isNullOrEmpty(first.second.stampingZone) 
        && !Strings.isNullOrEmpty(second.first.stampingZone) 
        && !first.second.stampingZone.equals(second.first.stampingZone)) {
      return true;
    }
    return false;
  }

  /**
   * Metodo che verifica se un intervallo tra due timbrature effettuate in un link tra zone
   * sta all'interno del tempo di trasferimento previsto tra una zona e l'altra.
   *
   * @param first la prima coppia di timbrature
   * @param second la seconda coppia di timbrature
   * @param zoneToZones il link tra zone di timbratura
   * @return true se il tempo trascorso tra una timbratura e l'altra è inferiore al tempo di
   *     trasferimento previsto tra una zona e l'altra.
   */
  private boolean isTimeInDelay(PairStamping first, PairStamping second, 
      Optional<ZoneToZones> zoneToZones) {
    if (!zoneToZones.isPresent()) {
      return false;
    }
    if (DateUtility.toMinute(second.first.date) - DateUtility.toMinute(first.second.date) 
        < zoneToZones.get().delay) {
      return true;
    }
    return false;
  }

  /**
   * Popola il campo difference del PersonDay.
   *
   * @param personDay       personDay
   * @param wttd            wttd
   * @param fixedTimeAtWork fixedTimeAtWork
   */
  public void updateDifference(PersonDay personDay, WorkingTimeTypeDay wttd,
      boolean fixedTimeAtWork,  LocalTime startLunch, LocalTime endLunch,
      LocalTime startWork, LocalTime endWork, Optional<Stamping> exitingNow) {

    // Patch fixed: la differenza è sempre 0
    if (fixedTimeAtWork) {
      personDay.setDifference(0);
      return;
    }

    //TODO: per pulizia i wttd festivi dovrebbero avere il campo wttd.workingTime == 0
    // Implementare la modifica dei piani ferie di default. 
    int plannedWorkingTime = wttd.workingTime;
    if (personDay.isHoliday) {
      plannedWorkingTime = 0;
    }

    // del caso di assenze giornaliere la differenza è 0 ed il calcolo è concluso.
    if (isAllDayAbsences(personDay)) {
      personDay.setDifference(0);
      return;
    } 

    personDay.setDifference(personDay.getTimeAtWork() - plannedWorkingTime);

    // Decurtazione straordinari

    // in ogni caso nessun straordinario 
    for (Absence absence : personDay.absences) {
      if (absence.absenceType.getBehaviour(JustifiedBehaviourName.no_overtime).isPresent()) {
        personDay.setDifference(Math.min(personDay.getDifference(), 0));
        personDay.setTimeAtWork(Math.min(personDay.getTimeAtWork(), plannedWorkingTime));
        return;
      }
    }

    // riduce l'assenza per impedire lo straordinario
    boolean recompute = false;
    for (Absence absence : personDay.absences) {
      if (personDay.difference <= 0) {
        continue;
      }
      if (!absence.absenceType.getBehaviour(JustifiedBehaviourName.reduce_overtime).isPresent()) {
        continue;
      }
      if (absence.justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
        if (absence.justifiedMinutes > 0) {
          int decurted = personDay.difference > absence.justifiedMinutes 
              ? absence.justifiedMinutes : personDay.difference;
          if (decurted > 0) {
            absence.justifiedMinutes = absence.justifiedMinutes - decurted;
            recompute = true;
            if (!exitingNow.isPresent()) {
              absence.save();
            }            
          }
        }
      } else {
        throw new IllegalStateException("Handler noOvertime non ancora implementato per " 
            + absence.justifiedType.name);
      }
    }

    if (recompute) {
      updateTimeAtWork(personDay, wttd, fixedTimeAtWork, 
          startLunch, endLunch, startWork, endWork, exitingNow);

      updateDifference(personDay, wttd, fixedTimeAtWork, 
          startLunch, endLunch, startWork, endWork, exitingNow);
    }
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
   * Setta il valore della variabile isTicketAvailable solo se isTicketForcedByAdmin è false.
   *
   * @param pd                personDay
   * @param mealTicketBehaviour value.
   */
  public void setTicketStatusIfNotForced(PersonDay pd, MealTicketBehaviour mealTicketBehaviour) {

    if (!pd.isTicketForcedByAdmin()) {
      pd.setTicketAvailable(mealTicketBehaviour);
    }
  }

  /**
   * Se il personDay ricade nel caso del calcolo stima uscendo in questo momento.
   *
   * @param personDay personDay
   * @return esito
   */
  public boolean toComputeExitingNow(PersonDay personDay) {

    if (!personDay.isToday()) {
      return false;
    }
    if (personDay.stampings.isEmpty()) {
      return false;
    }

    final List<Stamping> ordered = ImmutableList
        .copyOf(personDay.stampings.stream().sorted().collect(Collectors.toList()));
    if (ordered.get(ordered.size() - 1).isIn()
        || ordered.get(ordered.size() - 1).stampType == StampTypes.MOTIVI_DI_SERVIZIO) {
      return true;
    }

    return false;

  }

  /**
   * Computa le timbrature valide e nel caso popola il tempo uscendo in questo momento.
   *
   * @param personDay personDay
   * @param now momento exitingNow
   * @param previousForProgressive personDay precedente per progressivo
   * @param wttd tipo orario giorno
   * @param fixed se la persona è fixed nel giorno
   */
  public void queSeraSera(PersonDay personDay, LocalDateTime now,
      Optional<PersonDay> previousForProgressive,
      WorkingTimeTypeDay wttd, boolean fixed,
      LocalTimeInterval lunchInterval, LocalTimeInterval workInterval) {

    // aggiungo l'uscita fittizia 'now' nel caso risulti in servizio
    // TODO: non crearla nell'entity... da fastidio ai test. 
    // Crearla dentro updateTimeAtWork ma senza aggiungerla alla lista del personDay.

    Stamping stampingExitingNow = new Stamping(null, now);
    stampingExitingNow.way = WayType.out;
    stampingExitingNow.exitingNow = true;
    personDay.isConsideredExitingNow = true;

    updateTimeAtWork(personDay, wttd, fixed, lunchInterval.from, lunchInterval.to,
        workInterval.from, workInterval.to, Optional.of(stampingExitingNow));

    updateDifference(personDay, wttd, fixed, lunchInterval.from, lunchInterval.to,
        workInterval.from, workInterval.to, Optional.of(stampingExitingNow));

    updateProgressive(personDay, previousForProgressive);
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

    final PersonDay personDay = pd.getValue();

    final LocalDate sourceDateResidual = pd.getPersonDayContract().get().sourceDateResidual;
    //se prima o uguale a source contract il problema è fixato
    if (sourceDateResidual != null && !personDay.date.isAfter(sourceDateResidual)) {
      personDay.troubles.forEach(PersonDayInTrouble::delete);
      personDay.troubles.clear();

      log.info("Eliminati tutti i PersonDaysinTrouble relativi al giorno {} della persona {}"
          + " perchè precedente a sourceContract({})",
          personDay.date, personDay.person.fullName(),
          pd.getPersonDayContract().get().sourceDateResidual);
      return;
    }

    setValidPairStampings(pd.getValue().stampings);

    final boolean allValidStampings = allValidStampings(personDay);
    final boolean isAllDayAbsences = isAllDayAbsences(personDay);
    final boolean noStampings = personDay.stampings.isEmpty();
    final boolean isFixedTimeAtWork = pd.isFixedTimeAtWork();
    final boolean isHoliday = personDay.isHoliday;
    final boolean isEnoughHourlyAbsences = isEnoughHourlyAbsences(pd);
    final boolean isCompleteDayAndAddOvertimeAbsence = 
        isCompleteDayAndAddOvertimeAbsence(personDay);

    // PRESENZA AUTOMATICA
    if (isFixedTimeAtWork && !allValidStampings) {
      personDayInTroubleManager.setTrouble(personDay, Troubles.UNCOUPLED_FIXED);
    } else {
      personDayInTroubleManager.fixTrouble(personDay, Troubles.UNCOUPLED_FIXED);
    }

    // CASI STANDARD

    // ### CASO 1: no festa + no assenze giornaliere + no timbrature + no assenze a completamento
    if (!isFixedTimeAtWork && !isHoliday && !isAllDayAbsences && noStampings
        && !isEnoughHourlyAbsences && !isCompleteDayAndAddOvertimeAbsence) {
      personDayInTroubleManager.setTrouble(personDay, Troubles.NO_ABS_NO_STAMP);
    } else {
      personDayInTroubleManager.fixTrouble(personDay, Troubles.NO_ABS_NO_STAMP);
    }

    // ### CASO 2: no festa + no assenze giornaliere + timbrature disaccoppiate
    if (!isFixedTimeAtWork && !isHoliday && !isAllDayAbsences && !allValidStampings
        && !isCompleteDayAndAddOvertimeAbsence) {
      personDayInTroubleManager.setTrouble(personDay, Troubles.UNCOUPLED_WORKING);
    } else {
      personDayInTroubleManager.fixTrouble(personDay, Troubles.UNCOUPLED_WORKING);
    }

    // ### CASO 3 festa + no assenze giornaliere + timbrature disaccoppiate
    if (!isFixedTimeAtWork && isHoliday && !isAllDayAbsences && !allValidStampings 
        && ! isCompleteDayAndAddOvertimeAbsence) {
      personDayInTroubleManager.setTrouble(personDay, Troubles.UNCOUPLED_HOLIDAY);
    } else {
      personDayInTroubleManager.fixTrouble(personDay, Troubles.UNCOUPLED_HOLIDAY);
    }

    // ### CASO 4 Tempo a lavoro (di timbrature + assenze) non sufficiente
    // Per i livelli 4-8 dev'essere almeno la metà del tempo a lavoro
    if (personDay.person.qualification.qualification > 3
        && isAllDayAbsences == false && personDay.isHoliday == false
        && personDay.timeAtWork < (pd.getWorkingTimeTypeDay().get().getWorkingTime() / 2)) {
      personDayInTroubleManager.setTrouble(personDay, Troubles.NOT_ENOUGH_WORKTIME);

    } else {
      personDayInTroubleManager.fixTrouble(personDay, Troubles.NOT_ENOUGH_WORKTIME);

    }

  }

  /**
   * Metodo che controlla se il giorno è valido rispetto a tempo a lavoro, festivo.
   *
   * @param personDay il personDay relativo alla persona e al giorno di interesse
   * @param pd il wrapper contenente i metodi di utilità
   * @return true se è un giorno valido rispetto a tempo a lavoro, festivo, assenze ecc...
   *     false altrimenti.
   */
  public boolean isValidDay(PersonDay personDay, IWrapperPersonDay pd) {
    if (// non deve essere festivo
        personDay.isHoliday
        // le assenze non devono essere giornaliere 
        || isAllDayAbsences(personDay)
        // Il tempo a lavoro non è almeno la metà di quello previsto
        || personDay.timeAtWork < (pd.getWorkingTimeTypeDay().get().getWorkingTime() / 2)) {
      return false;
    }
    return true;
  }

  /**
   * Calcola le coppie di stampings valide al fine del calcolo del time at work. <br>
   *
   * @param stampings stampings
   * @return coppie
   */
  public List<PairStamping> getValidPairStampings(List<Stamping> stampings) {

    final List<Stamping> orderedStampings = ImmutableList
        .copyOf(stampings.stream().sorted().collect(Collectors.toList()));

    return computeValidPairStampings(orderedStampings);
  }

  /**
   * Calcola le coppie di stampings valide al fine del calcolo del time at work. <br>
   *
   * @param stampings stampings
   * @return coppie
   */
  public List<PairStamping> getValidPairStampings(List<Stamping> stampings,
      Optional<Stamping> exitingNow) {
    List<Stamping> copy = Lists.newArrayList(stampings);
    if (exitingNow.isPresent()) {
      copy.add(exitingNow.get());
    }
    Collections.sort(copy);
    return computeValidPairStampings(copy);
  }

  /**
   * - Setta il campo valid per ciascuna stamping del personDay 
   *   (sulla base del loro valore al momento della call) <br>
   * - Associa ogni stamping alla coppia valida individuata se presente
   *   (campo stamping.pairId).
   *
   * @param stampings la lista di timbrature
   */
  public void setValidPairStampings(List<Stamping> stampings) {

    final List<Stamping> orderedStampings = ImmutableList
        .copyOf(stampings.stream().sorted().collect(Collectors.toList()));

    computeValidPairStampings(orderedStampings);
  }


  /**
   * Calcola le coppie di stampings valide al fine del calcolo del time at work. <br>
   *
   * @modify setta il campo stamping.valid di ciascuna stampings contenuta nel personDay.<br>
   * @modify setta il campo stamping.pairId con il valore dalla coppia a cui appartengono.
   */
  private List<PairStamping> computeValidPairStampings(List<Stamping> orderedStampings) {

    if (orderedStampings.isEmpty()) {
      return Lists.newArrayList();
    }

    //(1)Costruisco le coppie valide per calcolare il worktime
    List<PairStamping> validPairs = Lists.newArrayList();
    List<Stamping> serviceStampings = Lists.newArrayList();

    Stamping stampEnter = null;

    for (Stamping stamping : orderedStampings) {
      //le stampings di servizio non entrano a far parte del calcolo del work time
      //ma le controllo successivamente
      //per segnalare eventuali errori di accoppiamento e appartenenza a orario di lavoro valido
      if (stamping.stampType == StampTypes.MOTIVI_DI_SERVIZIO) {
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
        }
      }
    }
    //(2) scarto le stamping di servizio che non appartengono ad alcuna coppia valida
    List<Stamping> serviceStampingsToCheck = Lists.newArrayList();
    for (Stamping stamping : serviceStampings) {
      boolean belongToValidPairNotOutsite = false;
      boolean belongToValidPairOutsite = false;
      for (PairStamping validPair : validPairs) {
        LocalDateTime outTime = validPair.second.date;
        LocalDateTime inTime = validPair.first.date;
        if (stamping.date.isAfter(inTime) && stamping.date.isBefore(outTime)) {
          if (validPair.second.stampType == StampTypes.LAVORO_FUORI_SEDE
              || validPair.first.stampType == StampTypes.LAVORO_FUORI_SEDE) {
            belongToValidPairOutsite = true;
          } else {
            belongToValidPairNotOutsite = true;
          }
          break;
        }
      }
      if (belongToValidPairNotOutsite) {
        serviceStampingsToCheck.add(stamping);
      } else if (belongToValidPairOutsite) {
        stamping.valid = true;         //capire se è corretto...
      } else {
        stamping.valid = false;
      }
    }

    //(3)aggrego le stamping di servizio per coppie valide ed eseguo il check di sequenza valida
    for (PairStamping validPair : validPairs) {
      LocalDateTime outTime = validPair.second.date;
      LocalDateTime inTime = validPair.first.date;
      List<Stamping> serviceStampingsInSinglePair = serviceStampingsToCheck.stream()
          .filter(stamping -> stamping.date.isAfter(inTime) && stamping.date.isBefore(outTime))
          .collect(Collectors.toList());
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
   * Metodo che controlla i giorni lavorabili in un mese per la persona passata come parametro.

   * @param person la persona di cui si cercano i giorni di lavoro nel mese
   * @param beginMonth l'inizio del mese
   * @param endMonth la fine del mese
   * @return la lista dei giorni di lavoro che il dipendente deve fare nel mese.
   */
  public List<PersonDay> workingDaysInMonth(Person person, 
      LocalDate beginMonth, LocalDate endMonth) {
    List<PersonDay> monthlyWorkDays = Lists.newArrayList();
    LocalDate currentDate = beginMonth;
    while (!currentDate.isAfter(endMonth)) {
      if (!isHoliday(person, currentDate)) {
        Optional<PersonDay> pd = personDayDao.getPersonDay(person, currentDate);
        if (!pd.isPresent()) {
          pd = Optional.fromNullable(new PersonDay(person, currentDate));
        }
        monthlyWorkDays.add(pd.get());
      }
      currentDate = currentDate.plusDays(1);
    }
    return monthlyWorkDays;
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
   * Se la persona è in missione nel giorno.
   *
   * @param personDay giorno
   * @return esito
   */
  public boolean isOnMission(PersonDay personDay) {
    return personDay.absences.stream()
        .filter(absence -> absence.absenceType.code.equals(AbsenceTypeMapping.MISSIONE.getCode()))
        .findAny().isPresent();
  }

  /**
   * Se la persona è in missione oraria nel giorno.
   *
   * @param personDay giorno
   * @return esito
   */
  public boolean isOnHourlyMission(PersonDay personDay) {
    return personDay.absences.stream()
        .filter(absence -> absence.absenceType.code.equals("92M"))
        .findAny().isPresent();
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
        if ("out".equals(s.way.description)) {
          coupleOfStampings++;
          lastWay = null;
          continue;
        }
        //trovo in lastWay diventa in
        if ("in".equals(s.way.description)) {
          lastWay = s.way.description;
          continue;
        }

      }
      //lastWay in
      if ("in".equals(lastWay)) {
        //trovo out chiudo una coppia
        if ("out".equals(s.way.description)) {
          coupleOfStampings++;
          lastWay = null;
          continue;
        }
        //trovo in chiudo una coppia e lastWay resta in
        if ("in".equals(s.way.description)) {
          coupleOfStampings++;
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
   * Metodo di utilità che determina la quantità di tempo a lavoro in eccesso nel caso 
   * una persona sia in turno.
   *
   * @param pd il personDay da controllare
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
   * Il metodo viene utilizzato nel calcolo del tempo a lavoro nei casi di finestre di 
   * ingresso/uscita specificate per quanto riguarda l'apertura/chiususa sede e nel caso 
   * di calcolo del tempo a lavoro all'interno di una fascia di turno.
   *
   * @param validPairs coppie valide di timbrature per il tempo a lavoro
   * @param start  inizio finestra
   * @param end    fine finestra
   * @return minuti
   */
  public int workingMinutes(List<PairStamping> validPairs,
      LocalTime start, LocalTime end) {

    int workingMinutes = 0;    

    //Per ogni coppia valida butto via il tempo oltre la fascia.
    for (PairStamping validPair : validPairs) {

      LocalTime consideredStart = new LocalTime(validPair.first.date);
      LocalTime consideredEnd = new LocalTime(validPair.second.date);
      if (consideredEnd.isBefore(start)) {
        continue;
      }
      if (consideredStart.isAfter(end)) {
        continue;
      }
      if (consideredStart.isBefore(start) && !validPair.first.isOffSiteWork()) {
        consideredStart = start;
      }
      if (consideredEnd.isAfter(end) && !validPair.second.isOffSiteWork()) {
        consideredEnd = end;
      }

      workingMinutes += DateUtility.toMinute(consideredEnd) 
          - DateUtility.toMinute(consideredStart);      

    }
    return workingMinutes;

  }

  /**
   * Se le stampings nel giorno sono tutte valide.
   *
   * @param personDay personDay
   * @return esito
   */
  public boolean allValidStampings(PersonDay personDay) {

    return !personDay.stampings.stream().filter(stamping -> !stamping.isValid()).findAny()
        .isPresent();
  }

  /**
   * Cerca il personDay se non esiste lo crea e lo persiste.
   */
  // FIXME questo è un duplicato del metodo che esisteva già nel personDayDao
  // è stato rimosso da li, ma spostare questo metodo in quella classe in alcuni casi
  // limita le classi da injettare, valutare se spostarlo
  public PersonDay getOrCreateAndPersistPersonDay(Person person, LocalDate date) {

    Optional<PersonDay> optPersonDay = personDayDao.getPersonDay(person, date);
    if (optPersonDay.isPresent()) {
      return optPersonDay.get();
    }
    PersonDay personDay = new PersonDay(person, date);
    // FIXME cosa ci fa l'informazione della festività in questo metodo?
    personDay.isHoliday = isHoliday(person, date);
    personDay.create();
    return personDay;
  }

  /**
   * Se il giorno è festivo per la persona.
   *
   * @param person Persona interessata
   * @param date   data da controllare
   * @return true se il giorno è festivo, false altrimenti.
   */
  public boolean isHoliday(Person person, LocalDate date) {

    return isHoliday(person, date, true);

  }

  /**
   * Metodo che verifica se il giorno, per la persona passata come parametro, 
   * è da considerarsi festivo o meno.
   *
   * @param person persona interessata
   * @param date la data da controllare
   * @param saturdayHoliday se il sabato deve essere considerato festivo
   * @return true se il giorno è festivo, false altrimenti.
   */
  public boolean isHoliday(Person person, LocalDate date, boolean saturdayHoliday) {
    //Festività generale
    MonthDay patron = (MonthDay) configurationManager
        .configValue(person.office, EpasParam.DAY_OF_PATRON, date);
    if (DateUtility.isGeneralHoliday(Optional.fromNullable(patron), date)) {
      return true;
    }

    Optional<WorkingTimeTypeDay> workingTimeTypeDay = workingTimeTypeDao
        .getWorkingTimeTypeDay(date, person);

    //persona fuori contratto
    if (!workingTimeTypeDay.isPresent()) {
      return false;
    }

    //tempo a lavoro
    if (saturdayHoliday) {
      return workingTimeTypeDay.get().holiday;
    } else {
      if (workingTimeTypeDay.get().dayOfWeek == DateTimeConstants.SUNDAY) {
        return true;
      }
      return false;
    }
  }

  /**
   * Metodo che controlla se si è trascorso abbastanza tempo in sede per essere considerati 
   * presenti.
   *
   * @param stampings la lista delle timbrature
   * @return true se il tempo trascorso in sede è sufficiente, false altrimenti.
   */
  public boolean enoughTimeInSeat(List<Stamping> stampings, IWrapperPersonDay day) {
    if (stampings.isEmpty()) {
      return false;
    }
    final List<Stamping> orderedStampings = ImmutableList
        .copyOf(stampings.stream().sorted().collect(Collectors.toList()));
    List<PairStamping> pairStampings = computeValidPairStampingsForPresence(orderedStampings);
    //List<PairStamping> pairStampings = computeValidPairStampings(orderedStampings);
    boolean enough = false;
    int timeInSeat = 0;
    //int timeOffSeat = 0;
    if (pairStampings.isEmpty()) {
      return false;
    }
    for (PairStamping pair : pairStampings) {
      if ((pair.first.stampType != null 
          && pair.first.stampType.equals(StampTypes.LAVORO_FUORI_SEDE)) 
          || (pair.second.stampType != null 
          && pair.second.stampType.equals(StampTypes.LAVORO_FUORI_SEDE))) {
        //timeOffSeat += pair.timeInPair;
      } else {
        timeInSeat += pair.timeInPair;
      }
    }
    if (timeInSeat >= day.getWorkingTimeTypeDay().get().workingTime / 2) {
      enough = true;
    }
    return enough;
  }

  /**
   * Metodo usato per calcolare quanto tempo è stato lavorato in sede dai dipendenti IV-VIII.
   *
   * @param orderedStampings la lista ordinata delle timbrature
   * @return la lista delle coppie valide di timbratura che servono per determinare il tempo 
   *     lavorato in sede.
   */
  private List<PairStamping> computeValidPairStampingsForPresence(List<Stamping> orderedStampings) {

    if (orderedStampings.isEmpty()) {
      return Lists.newArrayList();
    }
    boolean serviceStamping = false;
    Stamping stampEnter = null;
    List<PairStamping> pairStampings = Lists.newArrayList();
    for (Stamping stamping : orderedStampings) {

      if (stampEnter == null) {
        if (stamping.isIn()) {
          if ((stamping.stampType == StampTypes.MOTIVI_DI_SERVIZIO && serviceStamping) 
              || (stamping.stampType == null && !serviceStamping)
              || (stamping.stampType == StampTypes.PAUSA_PRANZO && !serviceStamping)) {
            stampEnter = stamping;
            continue;
          }

        }
        if (stamping.isOut()) {
          //una uscita prima di una entrata e' come se non esistesse          
          continue;
        }

      }
      //cerca l'uscita
      if (stampEnter != null) {
        if (stamping.isOut()) {
          if (!serviceStamping && stamping.stampType == StampTypes.MOTIVI_DI_SERVIZIO) {
            serviceStamping = true;
          }
          PairStamping pair = new PairStamping(stampEnter, stamping);
          pairStampings.add(pair);

          stampEnter = null;
          if (stamping.stampType == null) {
            serviceStamping = false;
          }
          continue;
        }
        //trovo un secondo ingresso, butto via il primo
        if (stamping.isIn()) {          
          stampEnter = stamping;
        }
      }

    }
    return pairStampings;
  }

  /**
   * L'insieme degli intervalli di tempo per cui non sono presenti timbrature nella fascia 
   * oraria obbligatoria.
   *
   * @param personDay il PersonDay di cui verificare le tibrature
   * @param mandatoryTimeSlotRange la fascia oraria obbligatoria
   * @return l'insieme degli intervalli in cui il dipendente non è al lavoro
   */
  private RangeSet<LocalTime> missingTimeRanges(PersonDay personDay, 
      Range<LocalTime> mandatoryTimeSlotRange) {
    val workingTimeRangeSet = TreeRangeSet.<LocalTime>create();

    val pairs = getValidPairStampings(personDay.stampings).stream().collect(Collectors.toList());

    for (PairStamping ps : pairs) {
      val vpr = Range.closed(ps.first.date.toLocalTime(), ps.second.date.toLocalTime());
      if (mandatoryTimeSlotRange.isConnected(vpr)) {
        workingTimeRangeSet.add(mandatoryTimeSlotRange.intersection(vpr));
      }
    }

    if (workingTimeRangeSet.isEmpty()) {
      return workingTimeRangeSet;
    }

    val missionTimeRangeSet = workingTimeRangeSet.complement();
    missionTimeRangeSet.remove(Range.upTo(mandatoryTimeSlotRange.lowerEndpoint(), BoundType.OPEN));
    missionTimeRangeSet.remove(Range.downTo(mandatoryTimeSlotRange.upperEndpoint(), 
        BoundType.OPEN));

    return missionTimeRangeSet;

  }

  /**
   * La somma del tempo in cui il dipendente non è al lavoro durante la fascia oraria
   * obbligatoria. 
   *
   * @param personDay il PersonDay di cui verificare le timbrature 
   * @param mandatoryTimeSlotRange la fascia oraria obbligatoria
   * @return il tempo (in minuti) in cui il dipendente non è al lavoro 
   *     durante la fascia obbligatoria.
   */
  private int missingTime(PersonDay personDay, Range<LocalTime> mandatoryTimeSlotRange) {
    val missingTimeRanges = missingTimeRanges(personDay, mandatoryTimeSlotRange);
    return missingTimeRanges.asRanges().stream()
        .mapToInt(tr -> 
        DateUtility.toMinute(tr.upperEndpoint()) - DateUtility.toMinute(tr.lowerEndpoint())).sum();
  } 

  /**
   * Calcola il tempo dell'eventuale pausa pranzo durante la fascia oraria obbligatoria 
   * di presenza.
   *
   * @param personDay il personDay di cui verificare il tempo per la pausa pranzo
   * @param mandatoryTimeSlotRange la fascia oraria obbligatoria
   * @return il tempo della pausa pranzo duranta la fascia oraria obbligatoria
   */
  private int lunchTime(PersonDay personDay, Range<LocalTime> mandatoryTimeSlotRange) {
    int lunchTime = 0;

    LocalTimeInterval lunchInterval = (LocalTimeInterval) configurationManager.configValue(
        personDay.person.office, EpasParam.LUNCH_INTERVAL, personDay.getDate());

    val gapLunchPairs = getGapLunchPairs(personDay, lunchInterval.from, 
        lunchInterval.to, Optional.absent());

    val lunchTimeRangeSet = TreeRangeSet.<LocalTime>create();

    for (PairStamping ps : gapLunchPairs) {
      val vpr = Range.closed(ps.first.date.toLocalTime(), ps.second.date.toLocalTime());
      if (mandatoryTimeSlotRange.isConnected(vpr)) {
        lunchTimeRangeSet.add(mandatoryTimeSlotRange.intersection(vpr));
      }
    }

    lunchTime = 
        lunchTimeRangeSet.asRanges().stream().mapToInt(
            lt -> DateUtility.toMinute(lt.upperEndpoint()) 
            - DateUtility.toMinute(lt.lowerEndpoint())).sum();

    return lunchTime;
  }

  /**
   * Effettua i calcoli del tempo lavorato durante la fascia oraria obbligatoria, costruendo
   * l'eventuale permesso breve di cui il dipendente deve usufruire nel caso ci siano assenze orarie
   * durante l'orario di lavoro obbligatorio.
   * Le eventuali pause pranzo durante la fascia di oraria obbligatoria sono ammesse e non danno
   * luogo alla necessità di chiedere un permesso breve.
   *
   * @param personDay il PersonDay contenente le timbrature da verificare (deve essere persistente)
   * @param mandatoryTimeSlot la fascia oraria obbligatoria da utilizzare nelle verifiche orarie
   * @return un Optional contente l'assenza che rappresenta il permesso breve 
   *     (l'assenza non è persita), Optional.absent() se tutta la fascia obbligatoria 
   *     è coperta come orario. 
   */
  private Optional<Absence> buildShortPermissionAbsence(PersonDay personDay, 
      TimeSlot mandatoryTimeSlot) {   
    val mandatoryTimeSlotRange = Range.closed(mandatoryTimeSlot.beginSlot, 
        mandatoryTimeSlot.endSlot);

    int missingTime = missingTime(personDay, mandatoryTimeSlotRange);
    int lunchTime = lunchTime(personDay, mandatoryTimeSlotRange);

    //Il tempo per pausa pranzo durante la fascia obbligatoria è giustificato
    missingTime -= lunchTime;
    if (missingTime > 0) {
      log.debug("Calcolato permesso breve di {} minuti per {} nel giorno {}", 
          missingTime, personDay.person.getFullname(), personDay.date);  
    }

    if (lunchTime > 0) {
      log.debug("Nell'attribuzione del permesso breve ignorati {} minuti di pausa pranzo", 
          lunchTime);
    }

    //create
    if (missingTime > 0) {

      val shortPermissionAbsenceType = absenceComponentDao.absenceTypeByCode("PB");
      if (!shortPermissionAbsenceType.isPresent()) {
        log.warn("Il codice PB per i permessi brevi non è disponibile");
        return Optional.absent();
      } else {
        val shortPermission = new Absence();
        shortPermission.personDay = personDay;
        shortPermission.absenceType = absenceComponentDao.absenceTypeByCode("PB").get();
        shortPermission.justifiedType = absenceComponentDao
            .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes_limit);
        shortPermission.justifiedMinutes = missingTime;
        return Optional.of(shortPermission);
      }
    } else {
      return Optional.absent(); 
    }

  }

  /**
   * Verifica e gestisce eventuali Permessi brevi legati a fascie orarie obbligatorie
   * per il dipendente.
   *
   * @param personDay il personday da verificare per l'eventuale permesso breve
   */
  public void checkAndManageMandatoryTimeSlot(PersonDay personDay) {
    Verify.verifyNotNull(personDay);
    Verify.verifyNotNull(personDay.person);

    new Job<Void>() {
      @Override
      public void doJob() {
        val mandatoryTimeSlot = contractDao
            .getContractMandatoryTimeSlot(personDay.date, personDay.person.id);
        if (!mandatoryTimeSlot.isPresent()) {
          log.trace("Le timbrature di {} del giorno {} NON necessitano di controlli "
              + "sulla fascia obbligatoria",
              personDay.person, personDay.date);
          return;
        }
        log.trace("Le timbrature di {} del giorno {} necessitano di controlli "
            + "sulla fascia obbligatoria", 
            personDay.person, personDay.date);

        //I turni non hanno vincoli di fascia obbligatoria nei giorni in cui sono in turno
        boolean inShift = personShiftDayDao
            .getPersonShiftDay(personDay.person, personDay.date).isPresent();

        //Se sono presenti assenze giornalieri la fascia obbigatoria non deve essere 
        //rispettata anche in presenta di timbrature
        boolean isAllDayAbsencePresent = isAllDayAbsences(personDay); 

        val previousShortPermission = 
            personDay.absences.stream().filter(a -> a.absenceType.code.equals("PB")).findFirst();

        if (inShift || isAllDayAbsencePresent || personDay.isHoliday()) {
          if (previousShortPermission.isPresent()) {
            //Viene fatta prima la merge perché l'assenza è detached
            previousShortPermission.get().merge()._delete();
            log.info("Rimosso permesso breve di {} minuti nel giorno {} per {} poiché sono presenti"
                + " assenze giornaliere oppure il dipendente è in turno, "
                + "oppure è un giorno festivo.",
                previousShortPermission.get().justifiedMinutes, personDay.date, 
                personDay.person.getFullname());
            return;
          } else {
            log.debug("Le timbrature di {} del giorno {} NON necessitano di controlli sulla fascia "
                + "obbligatoria poichè sono presenti assenze giornaliere oppure il dipendente "
                + "è in turno, oppure è un giorno festivo.",
                personDay.person, personDay.date);
            return;
          }
        }

        val shortPermission = 
            buildShortPermissionAbsence(personDay, mandatoryTimeSlot.get().timeSlot);

        if (!shortPermission.isPresent() && !previousShortPermission.isPresent()) {
          return;
        }

        if (shortPermission.isPresent() && !previousShortPermission.isPresent()) {
          log.info("Inserito permesso breve di {} minuti nel giorno {} per {}",
              shortPermission.get().justifiedMinutes, personDay.date, 
              personDay.person.getFullname());
          shortPermission.get().save();
          return;
        }

        if (!shortPermission.isPresent() && previousShortPermission.isPresent()) {
          //Viene fatta prima la merge perché l'assenza è detached
          previousShortPermission.get().merge()._delete();
          log.info("Rimosso permesso breve di {} minuti nel giorno {} per {}",
              previousShortPermission.get().justifiedMinutes, personDay.date, 
              personDay.person.getFullname());          
          return;
        }

        //Se era già presente un permesso breve di durata diversa dall'attuale viene aggiornato 
        //il precedente permesso breve
        if (!previousShortPermission.get().justifiedMinutes
            .equals(shortPermission.get().justifiedMinutes)) {
          val newShortPermission = previousShortPermission.get().<Absence>merge();
          newShortPermission.justifiedMinutes = shortPermission.get().justifiedMinutes;
          newShortPermission.save();
          log.debug("Permesso breve esistente nel giorno {} per {}, aggiornato da {} a {} minuti", 
              personDay.date, personDay.person.getFullname(),
              previousShortPermission.get().justifiedMinutes, 
              shortPermission.get().justifiedMinutes);
        }

      }
    }.afterRequest();
  }

}