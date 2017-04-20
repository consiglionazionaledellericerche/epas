package manager.services.absences.certifications;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import dao.absences.AbsenceComponentDao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import manager.attestati.dto.internal.CruscottoDipendente;
import manager.attestati.dto.internal.CruscottoDipendente.SituazioneDipendenteAssenze;
import manager.attestati.dto.internal.CruscottoDipendente.SituazioneParametriControllo;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;

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

    private final AbsenceComponentDao absenceComponentDao;
    
    public Person person;
    
    //la tipologia situazione
    public AbsenceImportType type; 
   
    //la lista delle date per ciascun codice.
    public Map<String, List<LocalDate>> datesPerCode = Maps.newHashMap();
    
    //Campi per il controllo residui (da utilizzare se non sono null)
    public Integer totalUsable = null;
    public Integer totalResidual = null;
    
    //Codici non presenti su epas
    public List<String> absentCodes = Lists.newArrayList();
    
    //Se in ePAS in un giorno ci sono due codici dello stesso tipo (errore improbabile)
    public List<Absence> duplicatedAbsences = Lists.newArrayList();

    /**
     * Costruttore absenceSituation.
     * @param absenceComponentDao injected
     * @param person persona
     */
    public AbsenceSituation(AbsenceComponentDao absenceComponentDao, Person person) {
      this.absenceComponentDao = absenceComponentDao;
      this.person = person;
    }
    
    /**
     * Calcola la somma delle assenze in datesPerCode.
     * @return int
     */
    public int totalUsed() {
      int totalUsed = 0;
      for (List<LocalDate> dates : datesPerCode.values()) {
        totalUsed = totalUsed + dates.size();
      }
      return totalUsed;
    }

    /**
     * Se l'assenza è presente su ePAS.
     * @param code codice in attestati
     * @param date data
     * @return esito
     */
    public boolean isAbsencePresent(String code, LocalDate date) {
      
      Set<AbsenceType> certificationCodes = Sets.newHashSet();
      
      Optional<AbsenceType> absenceType = absenceComponentDao.absenceTypeByCode(code);
      if (!absenceType.isPresent() 
          || (absenceType.get().certificateCode != null 
          && !absenceType.get().certificateCode.equalsIgnoreCase(code))) {
        
        //Se non lo trovo oppure il codice in attestati è diverso da quello trovato
        //Lo cerco per codice attestati. Si potrebbe migliorare popolando per ogni codice 
        //la colonna codice attestati.
      } else {
        certificationCodes.add(absenceType.get());
      }
      
      certificationCodes.addAll(absenceComponentDao.absenceTypesByCertificationCode(code));
      
      if (certificationCodes.isEmpty()) {
        return false;
      }

      List<Absence> absences = Lists.newArrayList();
      for (AbsenceType certificationType : certificationCodes) {
        absences.addAll(absenceComponentDao.orderedAbsences(person, date, date, 
            Sets.newHashSet(certificationType)));
      }

      //Dovrebbe essere unica
      if (absences.isEmpty()) {
        return false;
      }
      
      if (absences.size() > 1) {
        duplicatedAbsences.addAll(absences);
      }
      
      return true;
    }
    
  }
  
  /**
   * La tipologia assenze da importare da Attestati. 
   * I parametri absencePattern e controlPattern vengono utilizzati per individuare i record
   * che arrivano da attestati.
   * @author alessandro
   *
   */
  public static enum AbsenceImportType {
    
    //Situazioni da prelevare dalla situazione assenze
    GENERIC(null, null),
    FERIE_ANNO_CORRENTE("32", null),
    PERMESSI_LEGGE("94", null),
    
    //Situazioni da prelevare da parametri controllo
    G661(null, "661"),
    FERIE_ANNO_PRECEDENTE(null, "Ferie Anno Precedente");

    
    public String absencePattern;      // matching campo situazioneDipendenteAssenze.codice.codice
    public String controlPattern;      // matching campo situazioneParametriControllo.descrizione
    
    private AbsenceImportType(String absencePattern, String controlPattern) {
      this.absencePattern = absencePattern;
      this.controlPattern = controlPattern;
    }
    
    /**
     * Individua la tipologia da importare a partire dalla struttura SituazioneDipendenteAssenze.
     * @param sda sda
     * @return type
     */
    public static AbsenceImportType get(SituazioneDipendenteAssenze sda) {
      if (sda.codice.codice.contains("661") || sda.codice.codice.equals("31")) {
        return null;
      }
      for (AbsenceImportType type : AbsenceImportType.values()) {
        if (type.absencePattern == null) {
          continue;
        }
        if (sda.codice.codice.equals(type.absencePattern)) {
          return type;
        }
      }
      return AbsenceImportType.GENERIC;
    }
    
    /**
     * Individua la tipologia da importare 
     * a partire dalla struttura dati SituazioneParametriControllo.
     * Mi interessano per adesso solo 661 e ferie anno precedente.
     * @param spa spa
     * @return type
     */
    public static AbsenceImportType get(SituazioneParametriControllo spa) {
      for (AbsenceImportType type : AbsenceImportType.values()) {
        if (type.controlPattern == null) {
          continue;
        }
        if (spa.descrizione.contains(type.controlPattern)) {
          return type;
        }
      }
      return null;
    }
  }
  
  /**
   * Costruttore della situazione annuale dipendente.
   * @param cruscotto cruscotto
   */
  public CertificationYearSituation(AbsenceComponentDao absenceComponentDao, Person person, 
      CruscottoDipendente cruscotto) { 
    
    this.person = person;
    this.year = cruscotto.annoSituazione;
    
    this.beginDate = new LocalDate(cruscotto.dipendente.dataAssunzione);
    if (cruscotto.dipendente.dataCessazione != null) {
      this.endDate = new LocalDate(cruscotto.dipendente.dataCessazione);
    }
    
    for (SituazioneDipendenteAssenze sda : cruscotto.situazioneDipendenteAssenze) {
      
      AbsenceSituation absenceSituation = new AbsenceSituation(absenceComponentDao, person);
      
      //type
      absenceSituation.type = AbsenceImportType.get(sda);
      if (absenceSituation.type == null) {
        continue;
      }
      
      //absences
      absenceSituation.datesPerCode.put(sda.codice.codice, sda.codeDates());
      
      //totali residui
      if (absenceSituation.type.equals(AbsenceImportType.FERIE_ANNO_CORRENTE)
          || absenceSituation.type.equals(AbsenceImportType.PERMESSI_LEGGE)) {
        absenceSituation.totalUsable = sda.qtLimiteConsentito;
        absenceSituation.totalResidual = sda.qtResiduaOreGiorni;
      }
      
      this.absenceSituations.add(absenceSituation);
    }
    
    for (SituazioneParametriControllo spa : cruscotto.situazioneParametriControllo) {
      
      AbsenceSituation absenceSituation = new AbsenceSituation(absenceComponentDao, person);
      
      //type
      absenceSituation.type = AbsenceImportType.get(spa);
      if (absenceSituation.type == null) {
        continue;
      }
      //absences 
      absenceSituation.datesPerCode = spa.codesDates();
      
      //totali residui
      if (absenceSituation.type.equals(AbsenceImportType.FERIE_ANNO_PRECEDENTE) 
          || absenceSituation.type.equals(AbsenceImportType.G661)) {
        absenceSituation.totalUsable = spa.qtLimiteConsentito;
        absenceSituation.totalResidual = spa.qtResiduaOreGiorni;
      }
      
      this.absenceSituations.add(absenceSituation);
    }
    
  }

  
}
