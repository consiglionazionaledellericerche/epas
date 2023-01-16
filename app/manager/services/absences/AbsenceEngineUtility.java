/*
 * Copyright (C) 2022  Consiglio Nazionale delle Ricerche
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

package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import manager.services.absences.errors.CriticalError.CriticalProblem;
import manager.services.absences.errors.ErrorsBox;
import manager.services.absences.model.AbsencePeriod;
import models.Contract;
import models.ContractWorkingTimeType;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AbsenceTypeJustifiedBehaviour;
import models.absences.AmountType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedBehaviour.JustifiedBehaviourName;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeAmountAdjustment;
import org.joda.time.LocalDate;
import org.joda.time.MonthDay;
import org.testng.collections.Lists;

/**
 * Funzioni di utilità per i calcoli sulle assenze.
 */
public class AbsenceEngineUtility {
  
  private final Integer unitReplacingAmount = 1 * 100;

  /**
   * Le operazioni univocamente identificabili dal justifiedType. Devo riuscire a derivare
   * l'assenza da inserire attraverso il justifiedType.
   *  Lista con priorità:<br>
   *  - se esiste un solo codice allDay  -> lo metto tra le opzioni <br>
   *  - se esiste un solo codice halfDay -> lo metto tra le opzioni <br>
   *  - se esiste: un solo codice absence_type_minutes con Xminute <br>
   *               un solo codice absence_type_minutes con Yminute <br>
   *               ... <br>
   *               un solo codice absence_type_minutes con Zminute <br>
   *               un solo codice specifiedMinutes  <br>
   *               -> metto specifiedMinutes tra le opzioni <br>
   *  TODO: decidere come gestire il quanto manca               
   *
   * @param groupAbsenceType gruppo
   * @return entity list
   */
  public List<JustifiedTypeName> automaticJustifiedType(GroupAbsenceType groupAbsenceType) {
    
    // TODO: gruppo ferie, riposi compensativi
    if (groupAbsenceType.getPattern().equals(GroupAbsenceTypePattern.vacationsCnr)) {
      return Lists.newArrayList(JustifiedTypeName.all_day);
    }
    
    List<JustifiedTypeName> justifiedTypes = Lists.newArrayList();
    
    //TODO: Copia che mi metto da parte... ma andrebbe cachata!!
    final JustifiedTypeName specifiedMinutesVar = JustifiedTypeName.specified_minutes;
    JustifiedTypeName allDayVar = null;
    JustifiedTypeName halfDayVar = null;
    JustifiedTypeName completeDayAddOvertimeVar = null;

    //Map<Integer, Integer> specificMinutesFinded = Maps.newHashMap(); //(minute, count)
    //boolean specificMinutesDenied = false;
    Integer allDayFound = 0;
    Integer halfDayFound = 0;
    Integer specifiedMinutesFound = 0;
    Integer completeDayAddOvertimeFound = 0;

    if (groupAbsenceType.getTakableAbsenceBehaviour() == null) {
      return justifiedTypes;
    }
    
    for (AbsenceType absenceType : groupAbsenceType.getTakableAbsenceBehaviour()
        .getTakableCodes()) {
      for (JustifiedType justifiedType : absenceType.getJustifiedTypesPermitted()) { 
        if (justifiedType.getName().equals(JustifiedTypeName.all_day)) {
          allDayFound++;
          allDayVar = justifiedType.getName();
        }
        if (justifiedType.getName().equals(JustifiedTypeName.complete_day_and_add_overtime)) {
          completeDayAddOvertimeFound++;
          completeDayAddOvertimeVar = justifiedType.getName();
        }
        if (justifiedType.getName().equals(JustifiedTypeName.half_day)) {
          halfDayFound++;
          halfDayVar = justifiedType.getName();
        }
        if (justifiedType.getName().equals(JustifiedTypeName.specified_minutes)) {
          specifiedMinutesFound++;
        }
        if (justifiedType.getName().equals(JustifiedTypeName.absence_type_minutes)) {
          return Lists.newArrayList();
        }
      }
    }
    
    if (allDayFound == 1) {
      justifiedTypes.add(allDayVar);
    }
    if (completeDayAddOvertimeFound >= 1) {
      justifiedTypes.add(completeDayAddOvertimeVar);
    }
    if (halfDayFound == 1) {
      justifiedTypes.add(halfDayVar);
    }
    if (specifiedMinutesFound == 1) { //&& specificMinutesDenied == false) {
      justifiedTypes.add(specifiedMinutesVar);
    }
    
    return justifiedTypes;
  }
  
  /**
   * Quanto giustifica l'assenza passata.
   * Se non si riesce a stabilire il tempo giustificato si ritorna un numero negativo.
   *
   * @param person persona
   * @param absence assenza
   * @param amountType tipo di ammontare
   * @return tempo giustificato
   */
  public int absenceJustifiedAmount(Person person, Absence absence, AmountType amountType) {
    
    int amount = 0;

    if (absence.getJustifiedType().getName().equals(JustifiedTypeName.nothing)) {
      amount = 0;
    } else if (absence.getJustifiedType().getName().equals(JustifiedTypeName.all_day) 
        || absence.getJustifiedType().getName().equals(JustifiedTypeName.all_day_limit)
        || absence.getJustifiedType().getName().equals(JustifiedTypeName.assign_all_day)
        || absence.getJustifiedType().getName()
          .equals(JustifiedTypeName.complete_day_and_add_overtime)) {
      amount = absenceWorkingTime(person, absence);
    } else if (absence.getJustifiedType().getName().equals(JustifiedTypeName.half_day)) {
      amount = absenceWorkingTime(person, absence) / 2;
    } else if (absence.getJustifiedType().getName().equals(JustifiedTypeName.missing_time) 
        || absence.getJustifiedType().getName().equals(JustifiedTypeName.specified_minutes)
        || absence.getJustifiedType().getName().equals(JustifiedTypeName.specified_minutes_limit)) {
      // TODO: quello che manca va implementato. Occorre persistere la dacisione di quanto manca
      // se non si vogliono fare troppi calcoli.
      if (absence.getJustifiedMinutes() == null) {
        amount = 0;
      } else {
        amount = absence.getJustifiedMinutes();
      }
    } else if (absence.getJustifiedType().getName()
        .equals(JustifiedTypeName.absence_type_minutes)) {
      amount = absence.getAbsenceType().getJustifiedTime();
    } else {
      amount = -1;
    }
    
    if (amountType.equals(AmountType.units)) {
      int work = absenceWorkingTime(person, absence);
      if (work == -1) {
        //Patch: se è festa da verificare.
        if (absence.getJustifiedType().getName().equals(JustifiedTypeName.all_day)) {
          return 100;
        } 
      }
      if (work == 0) {
        return 0;
      }
      int result = amount * 100 / work;
      return result;
    } else {
      return amount;
    }
  }
  
  /**
   * la quantità corretta prendibile in percentuale.
   *
   * @param absence l'assenza
   * @param amount la quantità prendibile
   * @return la quantità corretta prendibile in percentuale.
   */
  public Integer takenBehaviouralFixes(Absence absence, Integer amount,
      List<Contract> fetchedContracts, DateInterval periodInterval, 
      TakeAmountAdjustment adjustment) {
    //Qui occorre verificare la quantità corretta per i codici 661G che, in caso di part time orizzontale,
    //non devono togliere l'intera giornata ma l'intera giornata meno un'ora
    Optional<AbsenceTypeJustifiedBehaviour> percentageTaken = 
        absence.getAbsenceType().getBehaviour(JustifiedBehaviourName.takenPercentageTime);
    if (percentageTaken.isPresent() && percentageTaken.get().getData() != null) {
      final int Max_Minutes_To_Cut_Back = 360;
      boolean workTimeAdjustment = adjustment.workTime;

      for (Contract contract : fetchedContracts) {
        if (DateUtility.intervalIntersection(contract.periodInterval(), periodInterval) == null) {
          continue;
        }
        for (ContractWorkingTimeType cwtt : contract.getContractWorkingTimeTypeOrderedList()) {
          if (DateUtility.intervalIntersection(cwtt.periodInterval(), periodInterval) == null) {
            continue;
          }          
          
          if (workTimeAdjustment && cwtt.getWorkingTimeType().isEnableAdjustmentForQuantity()) {
            //Adeguamento sull'incidenza del tipo orario
            if (cwtt.getWorkingTimeType().averageMinutesInWeek() > Max_Minutes_To_Cut_Back) {
              return Max_Minutes_To_Cut_Back;
            }
            return cwtt.getWorkingTimeType().averageMinutesInWeek() - 60;
          }          
          
        }
      }

    }
    
    return amount;
  }
  
  /**
   * Il tempo di lavoro nel giorno dell'assenza.
   *
   * @param person persona
   * @param absence assenza
   * @return tempo a lavoro assenza, 0 in caso di giorno contrattuale festivo
   */
  private int absenceWorkingTime(Person person, Absence absence) {
    LocalDate date = absence.getAbsenceDate();
    for (Contract contract : person.getContracts()) {
      for (ContractWorkingTimeType cwtt : contract.getContractWorkingTimeType()) {
        if (DateUtility.isDateIntoInterval(date, cwtt.periodInterval())) {
          if (cwtt.getWorkingTimeType().getWorkingTimeTypeDays()
              .get(date.getDayOfWeek() - 1).isHoliday()) {
            if (absence.getAbsenceType().isConsideredWeekEnd()) {
              LocalDate dateToChange = date;
              while (!DateUtility.isGeneralHoliday(Optional.<MonthDay>absent(), dateToChange)) {
                dateToChange = dateToChange.plusDays(1);
              }
              return cwtt.getWorkingTimeType().getWorkingTimeTypeDays().get(dateToChange.getDayOfWeek() - 1)
                  .getWorkingTime();
            } else {
              return 0;
            }            
          }
          return cwtt.getWorkingTimeType().getWorkingTimeTypeDays().get(date.getDayOfWeek() - 1)
              .getWorkingTime();
        }
      }
    }
    return 0;
  }
  
  /**
   * Quanto completa il rimpiazzamento.
   * Se non si riesce a stabilire il tempo di completamento si ritorna un numero negativo.
   *
   * @param absenceType tipo assenza
   * @param amountType tipo ammontare
   * @return ammontare
   */
  public int replacingAmount(AbsenceType absenceType, AmountType amountType) {

    //TODO: studiare meglio questa parte... 
    //Casi trattati:
    // 1) tipo completamento unità -> codice completamento un giorno
    //    ex:  89, 09B, 23H7, 25H7 
    // 2) tipo completamento minuti -> codice completamento minuti specificati assenza
    //    ex:  661H1C, 18H1C, 19H1C 

    if (absenceType == null) {
      return -1;
    }
    if (amountType.equals(AmountType.units) 
        && (absenceType.getReplacingType().getName().equals(JustifiedTypeName.all_day))) {
      return unitReplacingAmount; //una unità
    } 
    if (amountType.equals(AmountType.minutes) 
        && absenceType.getReplacingType().getName()
        .equals(JustifiedTypeName.absence_type_minutes)) {
      return absenceType.getReplacingTime();
    }

    return -1;

  }
  
  /**
   * Prova a inferire l'absenceType dell'assenza all'interno del periodo.
   *
   * @param absencePeriod periodo
   * @param absence assenza
   * @return assenza con tipo inferito
   */
  public Absence inferAbsenceType(AbsencePeriod absencePeriod, Absence absence) {

    if (absence.getJustifiedType() == null || !absencePeriod.isTakable()) {
      return absence;
    }
    
    // Controllo che il tipo sia inferibile
    if (!automaticJustifiedType(absencePeriod.groupAbsenceType)
        .contains(absence.getJustifiedType().getName())) {
      return absence;
    }

    //Cerco il codice
    if (absence.getJustifiedType().getName().equals(JustifiedTypeName.all_day)) {
      for (AbsenceType absenceType : absencePeriod.takableCodes) { 
        if (absenceType.getJustifiedTypesPermitted().contains(absence.getJustifiedType())) {
          absence.setAbsenceType(absenceType);
          return absence;
        }
      }
    }
    if (absence.getJustifiedType().getName()
        .equals(JustifiedTypeName.complete_day_and_add_overtime)) {
      for (AbsenceType absenceType : absencePeriod.takableCodes) { 
        if (absenceType.getJustifiedTypesPermitted().contains(absence.getJustifiedType())) {
          absence.setAbsenceType(absenceType);
          return absence;
        }
      }
    }

    if (absence.getJustifiedType().getName().equals(JustifiedTypeName.specified_minutes)) {
      
      AbsenceType specifiedMinutes = null;
      for (AbsenceType absenceType : absencePeriod.takableCodes) {
        for (JustifiedType absenceTypeJustifiedType : absenceType.getJustifiedTypesPermitted()) {
          if (absenceTypeJustifiedType.getName().equals(JustifiedTypeName.specified_minutes)) {
            if (absence.getJustifiedMinutes() != null) {
              absence.setAbsenceType(absenceType);
              return absence; 
            }
            specifiedMinutes = absenceType;
          }
          if (absenceTypeJustifiedType.getName().equals(JustifiedTypeName.absence_type_minutes)) {
            if (absenceType.getJustifiedTime().equals(absence.getJustifiedMinutes())) { 
              absence.setAbsenceType(absenceType);
              absence.setJustifiedType(absenceTypeJustifiedType);
              return absence;
            }
          }
        }
      }
      absence.setAbsenceType(specifiedMinutes);
      return absence; 
    }
    // TODO: quanto manca?
    return absence;
  }
 
  /**
   * I minuti.
   *
   * @param hours ore
   * @param minutes minuti
   * @return minuti
   */
  public Integer getMinutes(Integer hours, Integer minutes) {
    Integer selectedSpecifiedMinutes = null;
    if (hours == null) {
      hours = 0;
    }
    if (minutes == null) {
      minutes = 0;
    }
    selectedSpecifiedMinutes = (hours * 60) + minutes; 
    
    return selectedSpecifiedMinutes;
  }
  
  /**
   * Popola la lista ordinata in senso decrescente replacingCodesDesc a partire dal set di codici
   * replacingCodes. Popola gli eventuali errori.
   *
   * @param complationAmountType tipo ammontare
   * @param replacingCodes i codici da analizzare
   * @param date data per errori
   * @param replacingCodesDesc lista popolata
   * @param errorsBox errori popolati
   */
  public void setReplacingCodesDesc(
      final AmountType complationAmountType,
      final Set<AbsenceType> replacingCodes, 
      final LocalDate date,
      SortedMap<Integer, List<AbsenceType>> replacingCodesDesc,  // campi valorizzati dal metodo 
      ErrorsBox errorsBox) {
    
    for (AbsenceType absenceType : replacingCodes) {
      int amount = replacingAmount(absenceType, complationAmountType);
      if (amount < 1) {
        errorsBox.addCriticalError(date, absenceType, 
            CriticalProblem.IncalcolableReplacingAmount);
        continue;
      }

      List<AbsenceType> amountAbsenceType = replacingCodesDesc.get(amount);
      if (amountAbsenceType == null) {
        amountAbsenceType = Lists.newArrayList();
        replacingCodesDesc.put(amount, amountAbsenceType);
      }
      for (AbsenceType potentialConflicting : amountAbsenceType) {
        if (DateUtility.intervalIntersection(
            absenceType.validity(), potentialConflicting.validity()) != null) {
          errorsBox.addCriticalError(date, absenceType, potentialConflicting, 
              CriticalProblem.ConflictingReplacingAmount);
          continue;
        }
      }
      amountAbsenceType.add(absenceType);

    }
  }
  
  /**
   * Quale rimpiazzamento inserire se aggiungo il complationAmount al period nella data. 
   * I codici sono ordinati in modo decrescente in modo da testare per primi quelli col valore
   * più alto.
   *
   * @return tipo del rimpiazzamento
   */
  public Optional<AbsenceType> whichReplacingCode(
      SortedMap<Integer, List<AbsenceType>> replacingCodesDesc, 
      LocalDate date, int complationAmount) {
    
    for (Integer replacingTime : replacingCodesDesc.keySet()) {
      int amountToCompare = replacingTime;
      if (amountToCompare <= complationAmount) {
        // Il rimpiazzamento è corretto solo se non è scaduto alla data.
        for (AbsenceType absenceType : replacingCodesDesc.get(amountToCompare)) {
          if (!absenceType.isExpired(date)) {
            return Optional.of(absenceType);
          }  
        }
      }
    }
    
    return Optional.absent();
  }
  
  /**
   * I gruppi coinvolti nel tipo di assenza.
   *
   * @param absenceType tipo assenza
   * @return set gruppi
   */
  public Set<GroupAbsenceType> involvedGroup(AbsenceType absenceType) {
    Set<GroupAbsenceType> involvedGroup = Sets.newHashSet();
    for (TakableAbsenceBehaviour takableAbsenceBehaviour : absenceType.getTakableGroup()) {
      involvedGroup.addAll(takableAbsenceBehaviour.getGroupAbsenceTypes());
    }
    for (TakableAbsenceBehaviour takableAbsenceBehaviour : absenceType.getTakenGroup()) {
      involvedGroup.addAll(takableAbsenceBehaviour.getGroupAbsenceTypes());
    }
    for (ComplationAbsenceBehaviour complationAbsenceBehaviour : absenceType.getComplationGroup()) {
      involvedGroup.addAll(complationAbsenceBehaviour.getGroupAbsenceTypes());
    }
    for (ComplationAbsenceBehaviour complationAbsenceBehaviour : absenceType.getReplacingGroup()) {
      involvedGroup.addAll(complationAbsenceBehaviour.getGroupAbsenceTypes());
    }
    return involvedGroup;
  }
  
    
  /**
   * Ordina per data tutte le liste di assenze in una unica lista.
   *
   * @param absences liste di assenze
   * @return entity list
   */
  public List<Absence> orderAbsences(List<Absence>... absences) {
    SortedMap<LocalDate, Set<Absence>> map = Maps.newTreeMap();
    for (List<Absence> list : absences) {
      for (Absence absence : list) {
        Set<Absence> set = map.get(absence.getAbsenceDate());
        if (set == null) {
          set = Sets.newHashSet();
          map.put(absence.getAbsenceDate(), set);
        }
        set.add(absence);
      }
    }
    List<Absence> result = Lists.newArrayList();
    for (Set<Absence> set : map.values()) {
      result.addAll(set);
    }
    return result;
  }
  
  /**
   * Aggiunge alla mappa le assenze presenti in absences.
   *
   * @param absences le assenze da aggiungere alla mappa
   * @param map mappa
   * @return mappa
   */
  public Map<LocalDate, Set<Absence>> mapAbsences(List<Absence> absences, 
      Map<LocalDate, Set<Absence>> map) {
    if (map == null) {
      map = Maps.newHashMap();
    }
    for (Absence absence : absences) {
      Set<Absence> set = map.get(absence.getAbsenceDate());
      if (set == null) {
        set = Sets.newHashSet();
        map.put(absence.getAbsenceDate(), set);
      }
      set.add(absence);
    }
    return map;
  }

  /**
   * Calcola la riduzione in base al tempo contrattuale effettivamente lavorato ed alla % di
   * part time. (A seconda del tipo di aggiustamento richiesto).
   *
   * @param fixed tempo fisso di partenza
   * @param adjustment tipo aggiustamento richiesto
   * @param periodInterval periodo totale
   * @param contracts i contratti del dipendente
   * @return valora aggiustato (absent se impossibile calcolarlo)
   */
  public Integer takableAmountAdjustment(AbsencePeriod absencePeriod, LocalDate date, 
      int fixed, TakeAmountAdjustment adjustment, DateInterval periodInterval, 
      List<Contract> contracts) {
    
    boolean workTimeAdjustment = adjustment.workTime;
    boolean periodAdjustment = adjustment.periodTime;
    
    if (!periodInterval.isClosed()) {
      // Se si verificherà capire la casistica
      absencePeriod.errorsBox.addCriticalError(date, CriticalProblem.IncalcolableAdjustment);
      return null;
    }
    
    // I giorni del periodo da considerare per la riduzione sono: 
    // periodAdjustment -> l'intero periodo
    // !periodAdjustment -> i giorni di contratto attivi nel periodo 
    //                      (*) in tal caso la somma di tutte le cwttPercent sarà 100%
    int periodDays = 0;
    if (periodAdjustment) {
      periodDays = DateUtility.daysInInterval(periodInterval);
    } else {
      for (Contract contract : contracts) {
        if (DateUtility.intervalIntersection(contract.periodInterval(), periodInterval) == null) {
          continue;
        }
        for (ContractWorkingTimeType cwtt : contract.getContractWorkingTimeTypeOrderedList()) {
          DateInterval cwttInterval = 
              DateUtility.intervalIntersection(cwtt.periodInterval(), periodInterval); 
          if (cwttInterval != null) {
            periodDays += cwttInterval.dayInInterval();
          }
        }
      }  
    }
        
    Double totalAssigned = Double.valueOf(0);
    
    //Ricerca dei periodi di attività
    for (Contract contract : contracts) {
      if (DateUtility.intervalIntersection(contract.periodInterval(), periodInterval) == null) {
        continue;
      }
      for (ContractWorkingTimeType cwtt : contract.getContractWorkingTimeTypeOrderedList()) {
        if (DateUtility.intervalIntersection(cwtt.periodInterval(), periodInterval) == null) {
          continue;
        }
        
        //Intesezione
        DateInterval cwttInverval = DateUtility
            .intervalIntersection(cwtt.periodInterval(), periodInterval);

        //Per ogni contractWorkingTimeType coinvolto
        int cwttDays = DateUtility.daysInInterval(cwttInverval);

        //Incidenza cwtt sul periodo totale (in giorni)
        Double cwttPercent = ((double) cwttDays * 100) / periodDays; // (*)
        
        //Adeguamento sull'incidenza del periodo
        Double cwttAssigned = (cwttPercent * fixed) / 100;
        
        if (workTimeAdjustment && cwtt.getWorkingTimeType().isEnableAdjustmentForQuantity()) {
          //Adeguamento sull'incidenza del tipo orario
          cwttAssigned = (cwttAssigned * cwtt.getWorkingTimeType().percentEuristic()) / 100;
        }
        
        totalAssigned += cwttAssigned;
      }
    }
    
    return totalAssigned.intValue();
  }

}