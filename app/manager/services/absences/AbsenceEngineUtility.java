package manager.services.absences;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;

import manager.services.absences.model.AbsenceEngine;
import manager.services.absences.model.AbsencePeriod;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import java.util.List;
import java.util.Map;

public class AbsenceEngineUtility {
  
  private final AbsenceComponentDao absenceComponentDao;

  @Inject
  public AbsenceEngineUtility(AbsenceComponentDao absenceComponentDao) {
    this.absenceComponentDao = absenceComponentDao;
  }
 
  /**
   * Le operazioni univocamente identificabili dal justifiedType. Devo riuscire a derivare
   * l'assenza da inserire attraverso il justifiedType.
   *  Lista con priorità:
   *  - se esiste un solo codice allDay  -> lo metto tra le opzioni
   *  - se esiste un solo codice halfDay -> lo metto tra le opzioni
   *  - se esiste: un solo codice absence_type_minutes con Xminute
   *               un solo codice absence_type_minutes con Yminute
   *               ...
   *               un solo codice absence_type_minutes con Zminute
   *               un solo codice specifiedMinutes 
   *               -> metto specifiedMinutes tra le opzioni
   *  
   *  TODO: decidere come gestire il quanto manca               
   *                
   * @param groupAbsenceType
   * @return
   */
  public List<JustifiedType> automaticJustifiedType(GroupAbsenceType groupAbsenceType) {
    
    // TODO: gruppo ferie, riposi compensativi
    
    List<JustifiedType> justifiedTypes = Lists.newArrayList();
    
    //TODO: Copia che mi metto da parte... ma andrebbe cachata!!
    final JustifiedType specifiedMinutesVar = 
        absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);
    JustifiedType allDayVar = null;
    JustifiedType halfDayVar = null;

    Map<Integer, Integer> specificMinutesFinded = Maps.newHashMap(); //(minute, count)
    boolean specificMinutesDenied = false;
    Integer allDayFinded = 0;
    Integer halfDayFinded = 0;
    Integer specifiedMinutesFinded = 0;

    if (groupAbsenceType.takableAbsenceBehaviour == null) {
      return justifiedTypes;
    }
    
    for (AbsenceType absenceType : groupAbsenceType.takableAbsenceBehaviour.takableCodes) {
      for (JustifiedType justifiedType : absenceType.justifiedTypesPermitted) { 
        if (justifiedType.name.equals(JustifiedTypeName.all_day)) {
          allDayFinded++;
          allDayVar = justifiedType;
        }
        if (justifiedType.name.equals(JustifiedTypeName.half_day)) {
          halfDayFinded++;
          halfDayVar = justifiedType;
        }
        if (justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
          specifiedMinutesFinded++;
        }
        if (justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
          Integer minuteKey = absenceType.justifiedTime;
          Integer count = specificMinutesFinded.get(minuteKey);
          if (count == null) {
            specificMinutesFinded.put(minuteKey, 1);
          } else {
            count++;
            if (count > 1) {
              specificMinutesDenied = true;
            }
            specificMinutesFinded.put(minuteKey, count);
          }
        }
      }
    }
    
    if (allDayFinded == 1) {
      justifiedTypes.add(allDayVar);
    }
    if (halfDayFinded == 1) {
      justifiedTypes.add(halfDayVar);
    }
    if (specifiedMinutesFinded == 1 && specificMinutesDenied == false) {
      justifiedTypes.add(specifiedMinutesVar);
    }
    
    return justifiedTypes;
  }

  /**
   * Quanto giustifica l'assenza passata.
   * Se non si riesce a stabilire il tempo giustificato si ritorna un numero negativo.
   * @param person
   * @param date
   * @param absence
   * @param amountType
   * @return
   */
  public int computeAbsenceAmount(AbsenceEngine engineInstance, Absence absence, 
      AmountType amountType) {
    
    int amount = 0;

    if (absence.justifiedType.name.equals(JustifiedTypeName.nothing)) {
      amount = 0;
    } 
    else if (absence.justifiedType.name.equals(JustifiedTypeName.all_day)) {
      amount = engineInstance.workingTime(engineInstance.date);
    } 
    else if (absence.justifiedType.name.equals(JustifiedTypeName.half_day)) {
      amount = engineInstance.workingTime(engineInstance.date) / 2;
    }
    else if (absence.justifiedType.name.equals(JustifiedTypeName.missing_time) ||
        absence.justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
      // TODO: quello che manca va implementato. Occorre persistere la dacisione di quanto manca
      // se non si vogliono fare troppi calcoli.
      if (absence.justifiedMinutes == null) {
        amount = -1;
      } else {
        amount = absence.justifiedMinutes;
      }
    }
    else if (absence.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
      amount = absence.absenceType.justifiedTime;
    }
    else if (absence.justifiedType.name.equals(JustifiedTypeName.assign_all_day)) {
      amount = -1;
    }
    
    if (amountType.equals(AmountType.units)) {
      int work = engineInstance.workingTime(engineInstance.date);
      int result = amount * 100 / work;
      return result;
    } else {
      return amount;
    }
    
  }
  
  /**
   * Prova a inferire l'absenceType dal gruppo e dal justifiedTime e specifiedMinutes
   * @param absencePeriod
   * @param absence
   * @return
   */
  public Absence inferAbsenceType(AbsencePeriod absencePeriod, Absence absence) {

    if (absence.justifiedType == null || !absencePeriod.takableComponent.isPresent()) {
      return absence;
    }
    
    // Controllo che il tipo sia inferibile
    if (!automaticJustifiedType(absencePeriod.groupAbsenceType).contains(absence.justifiedType)) {
      return absence;
    }

    //Cerco il codice
    if (absence.justifiedType.name.equals(JustifiedTypeName.all_day)) {
      for (AbsenceType absenceType : absencePeriod.takableComponent.get().takableCodes) { 
        if (absenceType.justifiedTypesPermitted.contains(absence.justifiedType)) {
          absence.absenceType = absenceType;
          return absence;
        }
      }
    }
    if (absence.justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
      
      AbsenceType specifiedMinutes = null;
      for (AbsenceType absenceType : absencePeriod.takableComponent.get().takableCodes) {
        for (JustifiedType absenceTypeJustifiedType : absenceType.justifiedTypesPermitted) {
          if (absenceTypeJustifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
            if (absence.justifiedMinutes != null) {
              absence.absenceType = absenceType;
              return absence; 
            }
            specifiedMinutes = absenceType;
          }
          if (absenceTypeJustifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
            if (absenceType.justifiedTime.equals(absence.justifiedMinutes)) { 
              absence.absenceType = absenceType;
              absence.justifiedType = absenceTypeJustifiedType;
              return absence;
            }
          }
        }
      }
      absence.absenceType = specifiedMinutes;
      return absence; 
    }
    // TODO: quanto manca?
    return absence;
  }
 
  /**
   * Il valore già utilizzato da inizializzazione.
   * @return
   */
  @SuppressWarnings("unused")
  public int computeInitialComplationPercent() {
    // TODO: recuperare la percentuale inizializzazione quando ci sarà.
    return 0;
  }
  
  /**
   * La data cui si riferisce la percentuale inizializzazione.
   * @param absencePeriod
   * @return
   */
  @SuppressWarnings("unused")
  public LocalDate getInitialComplationDate(AbsencePeriod absencePeriod) {
    // TODO: utilizzare le strutture dati quando ci saranno.
    return absencePeriod.from;
  }
  
      
}
