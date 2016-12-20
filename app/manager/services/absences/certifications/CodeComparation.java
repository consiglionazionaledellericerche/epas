package manager.services.absences.certifications;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import manager.attestati.dto.show.CodiceAssenza;
import manager.services.absences.certifications.CodeComparation.SuperCode;
import manager.services.absences.enums.GroupEnum;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contiene la comparazione epas - attestati.
 */
public class CodeComparation {

  Map<String, SuperCode> superCodes = Maps.newHashMap();
  
  public List<SuperCode> onlyAttestati = Lists.newArrayList();
  public List<SuperCode> onlyEpas = Lists.newArrayList();
  public List<SuperCode> both = Lists.newArrayList();
  
  /**
   * Aggiunge un codice assenza attestati.
   * @param codiceAssenza codice in attestati
   * @return superCode inserito
   */
  public SuperCode putCodiceAssenza(CodiceAssenza codiceAssenza) {
    SuperCode superCode = superCodes.get(codiceAssenza.codice);
    if (superCode == null) {
      superCode = new SuperCode();
      superCode.code = codiceAssenza.codice;
      superCode.codiceAssenza = codiceAssenza;
      superCodes.put(superCode.code, superCode);
    } else {
      superCode.codiceAssenza = codiceAssenza;
    }
    return superCode;
  }

  /**
   * Aggiunge un codice assenza epas.
   * @param absenceType codice in epas
   * @return superCode inserito
   */
  public SuperCode putAbsenceType(AbsenceType absenceType) {
    final String code = absenceType.code.trim().toUpperCase();
    SuperCode superCode = superCodes.get(code);
    if (superCode == null) {
      superCode = new SuperCode();
      superCode.code = code;
      superCode.absenceType = absenceType;
      superCodes.put(superCode.code, superCode);
    } else {
      superCode.absenceType = absenceType;
    }
    return superCode;
  }
 
  /**
   * Aggiunge una assenza epas al rispettivo superCode.
   * @param absence assenza epas
   */
  public void putAbsence(Absence absence) {
    final String code = absence.absenceType.code.trim().toUpperCase();
    SuperCode superCode = superCodes.get(code);
    if (superCode == null) {
      superCode = putAbsenceType(absence.absenceType);
    }
    superCode.absences.add(absence);
  }

  /**
   * Imposta la lista dei codici solo in attestati.
   * @return la lista
   */
  public List<SuperCode> setOnlyAttestati() {
    
    onlyAttestati = Lists.newArrayList();
    for (SuperCode superCode : superCodes.values()) {
      if (superCode.codiceAssenza != null && superCode.absenceType == null) {
        onlyAttestati.add(superCode);
      }
    }
    return onlyAttestati;
  }
  
  /**
   * Imposta la lista dei codici solo in epas.
   * @return la lista
   */
  public List<SuperCode> setOnlyEpas() {
    onlyEpas = Lists.newArrayList();
    for (SuperCode superCode : superCodes.values()) {
      if (superCode.codiceAssenza == null && superCode.absenceType != null) {
        onlyEpas.add(superCode);
      }
    }
    return onlyEpas;
  }
  
  /**
   * Imposta la lista dei codici sia in attestati che in epas.
   * @return la lista
   */
  public List<SuperCode> setBoth() {
    both = Lists.newArrayList();
    for (SuperCode superCode : superCodes.values()) {
      if (superCode.codiceAssenza != null && superCode.absenceType != null) {
        both.add(superCode);
      }
    }
    return both;
  }
  
  /**
   * Elimina da epas i codici cancellabili (che soddisfano la condizione erasable() ).
   */
  public void eraseErasable() {
    for (SuperCode superCode : superCodes.values()) {
      if (superCode.erasable()) {
        Set<GroupAbsenceType> groupsInvolved = 
            superCode.absenceType.involvedGroupAbsenceType(false);
        for (GroupAbsenceType group : groupsInvolved) {
          group.takableAbsenceBehaviour.takableCodes.remove(superCode.absenceType);
          group.takableAbsenceBehaviour.takenCodes.remove(superCode.absenceType);
          group.takableAbsenceBehaviour.save();
          group.takableAbsenceBehaviour.refresh();
        }
        superCode.absenceType.delete();
        superCode.absenceType = null;
      }
    }
    
    setOnlyEpas();
    setOnlyAttestati();
    setBoth();
  }
  
  /**
   * Classe di supporto contenente i due stati.
   */
  public class SuperCode {
    
    String code;
    
    CodiceAssenza codiceAssenza;
    AbsenceType absenceType;
    List<Absence> absences = Lists.newArrayList();
    
    public boolean onlyEpas() {
      return absenceType != null && codiceAssenza == null;
    }
    
    public boolean onlyAttestati() {
      return absenceType == null && codiceAssenza != null;
    }
    
    public boolean both() {
      return absenceType != null && codiceAssenza != null;
    }
    
   
    /**
     * Se in epas il superCode risulta assegnato solo al gruppo generico.
     * @return esito
     */
    public boolean onlyOtherGroup() {
      for (GroupAbsenceType group : absenceType.involvedGroupAbsenceType(false)) {
        String groupName = group.name;
        String enumName = GroupEnum.ALTRI.name();
            
        if (!groupName.equals(enumName)) {
          return false;
        }
      }
      return true;
    }
    
    /**
     * Se il codice solo in epas ha problemi.
     * @return esito
     */
    public boolean withProblems() {
      
      //ha problemi un codice che è solo in epas..
      if (!onlyEpas()) {
        return false;
      }
      
      //.. e che non è cancellabile e appartiene solo al gruppo altri
      if (!erasable() && onlyOtherGroup()) {
        return true;
      }
      
      return false;
      
    }
    
    /**
     * Se il codice solo in epas è cancellabile.
     * @return esito
     */
    public boolean erasable() {
      
      //è cancellabile solo un codice che è solo in epas
      if (!onlyEpas()) {
        return false;
      }
      
      boolean notUsed = absences.isEmpty();
      boolean onlyOtherGroup = onlyOtherGroup();
      
      // non usato e senza gruppo
      if (notUsed && onlyOtherGroup) {
        return true;
      }
      
      return false;
        
    }
  }
  
}
