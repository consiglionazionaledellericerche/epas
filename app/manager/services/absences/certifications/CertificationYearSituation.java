package manager.services.absences.certifications;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

import manager.services.absences.model.PeriodChain;

import models.Person;
import models.absences.Absence;
import models.absences.definitions.DefaultGroup;

import org.joda.time.LocalDate;
import org.testng.collections.Sets;

/**
 * Contiene le informazioni per inizializzare le assenze del dipendente da Attestati.
 * @author alessandro
 *
 */
public class CertificationYearSituation {
  
  public Person person;
  public int year;
  
  public LocalDate beginDate;
  public LocalDate endDate;
  
  public List<AbsenceSituation> absenceSituations = Lists.newArrayList();
  
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
   * La situazione circa un singolo codice o gruppo.
   * @author alessandro
   *
   */
  public static class AbsenceSituation {

    public Person person;
    
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


    /**
     * Costruttore absenceSituation.
     * @param person persona
     */
    public AbsenceSituation(Person person, AbsenceSituationType type) {
      this.person = person;
      this.type = type;
    }
    
  }
  
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
    
    ALTRI("Altri codici", null);
    
    public DefaultGroup group;
    public String label;
    
    private AbsenceSituationType(String label, DefaultGroup group) {
      this.label = label;
      this.group = group;
    }
  }

  
}
