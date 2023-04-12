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
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import models.absences.Absence;
import models.absences.definitions.DefaultGroup;
import org.joda.time.LocalDate;

/**
 * Contiene le informazioni per inizializzare le assenze del dipendente da Attestati.
 *
 * @author Alessandro Martelli
 *
 */
public class CertificationYearSituation implements Serializable {

  private static final long serialVersionUID = -8220636622879573718L;

  public int year;
  
  public LocalDate beginDate;
  public LocalDate endDate;
  
  public List<AbsenceSituation> absenceSituations = Lists.newArrayList();
  
  public Map<String, Set<LocalDate>> certificationMap = Maps.newHashMap();
  
  /**
   * La situazione per quel tipo. 
   */
  public AbsenceSituation getAbsenceSituation(AbsenceSituationType type) {
    for (AbsenceSituation absenceSituation : this.absenceSituations) {
      if (absenceSituation.type.equals(type)) {
        return absenceSituation;
      }
    }
    return null;
  }
  
  /**
   * Se nella situazione ci sono assenza da inserire automaticamente.
   */
  public boolean toAddAuto() {
    for (AbsenceSituation absenceSituation : this.absenceSituations) {
      for (String code : absenceSituation.toAddAutomatically.keySet()) {
        if (!absenceSituation.toAddAutomatically.get(code).isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }
  
  /**
   * Se nella situazione ci sono assenza da inserire manualmente.
   */
  public boolean toAddManually() {
    for (AbsenceSituation absenceSituation : this.absenceSituations) {
      for (String code : absenceSituation.toAddManually.keySet()) {
        if (!absenceSituation.toAddManually.get(code).isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }
  
  /**
   * Se nella situazione tutte le assenze sono state correttemente importate.
   */
  public boolean allImported() {
    return !toAddAuto() && !toAddManually();    
  }
  
  /**
   * Primo figlio mancante.
   */
  public boolean firstChildMissing() {
    for (AbsenceSituation abSit : this.absenceSituations) {
      if (abSit.firstChildMissing) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Secondo figlio mancante.
   */
  public boolean secondChildMissing() {
    for (AbsenceSituation abSit : this.absenceSituations) {
      if (abSit.secondChildMissing) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Terzo figlio mancante.
   */
  public boolean thirdChildMissing() {
    for (AbsenceSituation abSit : this.absenceSituations) {
      if (abSit.thirdChildMissing) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * La situazione circa un singolo codice o gruppo.
   *
   * @author Alessandro Martelli
   *
   */
  public static class AbsenceSituation implements Serializable {

    private static final long serialVersionUID = -4945653666009422856L;

    public AbsenceSituationType type;
    
    //le date con assenza epas corretta per quel codice 
    public Map<String, Set<LocalDate>> datesPerCodeOk = Maps.newHashMap();
    
    //le date con assenza epas mancante per quel codice, inseribile automaticamente
    public Map<String, Set<LocalDate>> toAddAutomatically = Maps.newHashMap();
    
    //le date con assenza epas mancante per quel codice da inserire manualmente 
    // 1) riposi compensativi post inizializzazione non presenti in epas
    public Map<String, Set<LocalDate>> toAddManually = Maps.newHashMap();
    
    //assenze significative in epas non presenti in attestati e successive l'ultimo caricamento
    public List<Absence> notPresent = Lists.newArrayList();

    //assenze significative in epas non presenti in attestati e non successive l'ultimo caricamento
    public List<Absence> notPresentSent = Lists.newArrayList();
    
    //il totale usabile (ex 661, ferie, permessi, malattia)
    public Integer limit = null;
    public Integer limitVacationWithout32 = null;
    
    //Codici non presenti su epas
    public List<String> absentCodes = Lists.newArrayList();
    
    public boolean firstChildMissing = false;
    public boolean secondChildMissing = false;
    public boolean thirdChildMissing = false;


    /**
     * Costruttore absenceSituation.
     */
    public AbsenceSituation(AbsenceSituationType type) {
      this.type = type;
    }
    
  }
  
  /**
   * Tipologia di possibili situazioni circa un singolo codice o gruppo.
   */
  public static enum AbsenceSituationType {
    FERIE_ANNO_CORRENTE("Ferie anno corrente", null),
    FERIE_ANNO_PRECEDENTE("Ferie anno precedente", null),
    PERMESSI_LEGGE("Permessi legge", null),
    
    RIPOSO_COMPENSATIVO("Riposi compensativi", DefaultGroup.RIPOSI_CNR_ATTESTATI),
    
    RIDUCE_FERIE_ANNO_CORRENTE("Riduce ferie anno corrente", DefaultGroup.RIDUCE_FERIE_CNR),
    RIDUCE_FERIE_ANNO_PRECEDENTE("Riduce ferie anno passato", DefaultGroup.RIDUCE_FERIE_CNR),
    PERMESSO_PERSONALI("Permessi personali 661", DefaultGroup.G_661),
    ASTENSIONE_FIGLIO_1("Astensione facoltativa primo figlio", DefaultGroup.G_23),
    ASTENSIONE_FIGLIO_2("Astensione facoltativa secondo figlio", DefaultGroup.G_232),
    ASTENSIONE_FIGLIO_3("Astensione facoltativa terzo figlio", DefaultGroup.G_233),
    MALATTIA_3_ANNI("Malattia", DefaultGroup.MALATTIA_3_ANNI),
    MALATTIA_FIGLIO_1("Malattia primo figlio", DefaultGroup.MALATTIA_FIGLIO_1),
    MALATTIA_FIGLIO_2("Malattia secondo figlio", DefaultGroup.MALATTIA_FIGLIO_2),
    MALATTIA_FIGLIO_3("Malattia terzo figlio", DefaultGroup.MALATTIA_FIGLIO_3),
    
    MISSIONE("Missione", DefaultGroup.MISSIONE_GIORNALIERA),
    MISSIONE_ESTERA("Missione estera", DefaultGroup.MISSIONE_ESTERA),
    MISSIONE_ORARIA("Missione oraria", DefaultGroup.MISSIONE_ORARIA),
    
    LAVORO_AGILE("Lavoro agile", DefaultGroup.G_LAGILE),
    
    ALTRI("Altri codici", null);
    
    public DefaultGroup group;
    public String label;
    
    private AbsenceSituationType(String label, DefaultGroup group) {
      this.label = label;
      this.group = group;
    }
  }

  
}
