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

package manager.services.absences.certifications;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import manager.attestati.dto.show.CodiceAssenza;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;

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
   *
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
   *
   * @param absenceType codice in epas
   * @return superCode inserito
   */
  public SuperCode putAbsenceType(AbsenceType absenceType) {
    final String code = absenceType.getCode().trim().toUpperCase();
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
   *
   * @param absence assenza epas
   */
  public void putAbsence(Absence absence) {
    final String code = absence.getAbsenceType().getCode().trim().toUpperCase();
    SuperCode superCode = superCodes.get(code);
    if (superCode == null) {
      superCode = putAbsenceType(absence.getAbsenceType());
    }
    superCode.absences.add(absence);
  }

  /**
   * Imposta la lista dei codici solo in attestati.
   *
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
   *
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
   *
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
          group.getTakableAbsenceBehaviour().getTakableCodes().remove(superCode.absenceType);
          group.getTakableAbsenceBehaviour().getTakenCodes().remove(superCode.absenceType);
          group.getTakableAbsenceBehaviour().save();
          group.getTakableAbsenceBehaviour().refresh();
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
     * Un codice solo in epas ha problemi se non appartiene ad alcun gruppo e non è cancellabile.
     *
     * @return esito
     */
    public boolean withProblems() {
      if (onlyEpas() && !groupEpas() && !erasable()) {
        return true;
      }
      return false;
    }

    /**
     * Un codice in epas è cancellabile.
     * 1) non appartiene ad attestati
     * 2) non appartiene ad alcun gruppo epas
     * 3) non è mai stato usato
     *
     * @return esito
     */
    public boolean erasable() {

      //1) un codice in attestati non è cancellabile
      if (both() || onlyAttestati()) {
        return false;
      }

      //2) un codice che appartiene ad un gruppo epas non è cancellabile
      if (groupEpas()) {
        return false;
      }

      //3) un codice usato non è cancellabile
      if (!absences.isEmpty()) {
        return false;
      }
      
      return true;
    }

    /**
     * Se il codice in epas appartiene ad un gruppo epas.
     *
     * @return esito
     */
    public boolean groupEpas() {
      if (absenceType == null) {
        return false;
      }
      if (absenceType.involvedGroupAbsenceType(false).isEmpty()) {
        return false;
      }
      return true;
    }
  }

}
