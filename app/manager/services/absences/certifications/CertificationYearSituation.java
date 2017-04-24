package manager.services.absences.certifications;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
   * La situazione specifica.
   * @return null se non presente.
   */
  public AbsenceSituation getAbsenceSituation(AbsenceImportType type) {
    for (AbsenceSituation absenceSituation : absenceSituations) {
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
    
    //la tipologia situazione
    public AbsenceImportType type; 
 
    //le date con assenza epas corretta per quel codice 
    public Map<String, List<LocalDate>> datesPerCodeOk = Maps.newHashMap();
    
    //le date con assenza epas mancante per quel codice, inseribile automaticamente
    public Map<String, List<LocalDate>> datesPerCodeToAddAutomatically = Maps.newHashMap();
    
    //le date con assenza epas mancante per quel codice da inserire manualmente 
    // 1) riposi compensativi post inizializzazione non presenti in epas
    public Map<String, List<LocalDate>> datesPerCodeToAddManually = Maps.newHashMap();
    
    //le assenze epas significative non presenti in attestati
    // giorni ferie e permessi dopo l'inizializzazione
    // giorni di astensione fac. dopo l'inizializzazione
    // giorni di riduce ferie e permessi (non considerare inizializzazioni, servono gg specifici) 
    // giorni di malattia figli
    // giorni di malattia
    public List<Absence> absencesToRemove = Lists.newArrayList();
    
    //Campi per il controllo residui (da utilizzare se non sono null)
    public Integer totalUsable = null;
    public Integer totalResidual = null;
    
    //Codici non presenti su epas
    public List<String> absentCodes = Lists.newArrayList();
    
    //Se in ePAS in un giorno ci sono due codici dello stesso tipo (errore improbabile)
    public List<Absence> duplicatedAbsences = Lists.newArrayList();

    /**
     * Costruttore absenceSituation.
     * @param person persona
     */
    public AbsenceSituation(Person person, AbsenceImportType type) {
      this.person = person;
      this.type = type;
    }
    
    /**
     * Tutti i codici.
     */
    public Set<String> codes() {
      Set<String> codes = Sets.newHashSet();
      codes.addAll(datesPerCodeOk.keySet());
      codes.addAll(datesPerCodeToAddAutomatically.keySet());
      codes.addAll(datesPerCodeToAddManually.keySet());
      return codes;
    }
  }
  
  /**
   * Le tipologie di assenze da importare da Attestati. Significato dei campi:<br>
   * 1) absenceMatchPattern === situazioneDipendenteAssenze.codice.codice -> record da tenere<br> 
   * 2) absenceMatchExclusion === situazioneDipendenteAssenze.codice.codice -> record da 
   * scartare<br>
   * 3) controlMatchpattern === situazioneParametriControllo.descrizione -> record da tenere<br>
   * 4) defaultGroup i gruppi epas corrospondenti
   * 
   * @author alessandro
   *
   */
  public static enum AbsenceImportType {
    
    //Situazioni da prelevare dalla situazione assenze
    TIPO_GENERICO(
        null, 
        null, 
        null, 
        null),
    
    FERIE_ANNO_CORRENTE(
        "32", 
        null,
        null,
        ImmutableSet.of(DefaultGroup.FERIE_CNR, DefaultGroup.FERIE_CNR_PROROGA)),
    
    PERMESSI_LEGGE(
        "94", 
        null, 
        null,
        ImmutableSet.of(DefaultGroup.FERIE_CNR, DefaultGroup.FERIE_CNR_PROROGA)),
    
    RIPOSO_COMPENSATIVO(
        "91", 
        null,  
        null, 
        ImmutableSet.of(DefaultGroup.RIPOSI_CNR)),
    
    //Situazioni da prelevare da parametri controllo
    PERMESSO_PERSONALE_661(
        null,
        ImmutableSet.of("661"), 
        "Legge 661", 
        ImmutableSet.of(DefaultGroup.G_661)),
    
    FERIE_ANNO_PRECEDENTE(
        null, 
        ImmutableSet.of("31"), 
        "Ferie Anno Precedente", 
        ImmutableSet.of(DefaultGroup.FERIE_CNR, DefaultGroup.FERIE_CNR_PROROGA)),
    
    MALATTIA_PERSONALE(
        null,
        DefaultGroup.MALATTIA_3_ANNI.takable.allTakableTakenCodes(), 
        "Malattia Personale 100%", 
        ImmutableSet.of(DefaultGroup.MALATTIA_3_ANNI));

    
    public String absenceMatchPattern;   
    public Set<String> absenceMatchExclusions; 
    public String controlMatchPattern;   
    public DefaultGroup defaultGroup;    
    
    private AbsenceImportType(String absenceMatchPattern, Set<String> absenceMatchExclusions,
        String controlMatchPattern, 
        Set<DefaultGroup> defaultGroup) {
      this.absenceMatchPattern = absenceMatchPattern;
      this.absenceMatchExclusions = absenceMatchExclusions;
      this.controlMatchPattern = controlMatchPattern;
    }
  }
  
}
