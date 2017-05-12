package manager.attestati.dto.internal;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import lombok.ToString;

import manager.attestati.dto.internal.PeriodoDipendente.PeriodoDipendenteDettagli;

import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultGroup;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.collections.Sets;

/**
 * Json caricato quando si richiede il cruscotto annuale di un dipendente.
 * https://attestativ2.rm.cnr.it/api/rest/dipendente/stato/cruscotto/11028/2017
 * 
 * @author alessandro
 *
 */
@ToString
public class CruscottoDipendente implements Serializable {

  public int annoSituazione;                          //2017
  
  public PeriodoDipendenteDettagli dipendente;        //stessi dett. dipendente in PeriodoDipendente
  public SituazioneDipendenteAssenze[] situazioneDipendenteAssenze;
  public SituazioneParametriControllo[] situazioneParametriControllo;

  public static class SituazioneDipendenteCompetenze implements Serializable {
    
  }
  
  @ToString
  public static class SituazioneDipendenteAssenze implements Serializable {
    
    //public int dipendente.id;
    //public int dipendente.matricola;
    
    public int anno;                                    //2017
    public SituazioneCodiceAssenza codice;
    
    //0   assenze usate nel absencePeriod totali? (1)
    public Integer usateOreGiorni;                       
    //1  assenze usate nell'anno richiesto periodi chiusi ma non consolidati (2) esempio dei 18...
    public Integer qtImputatePeriodiChiusiNonConsolidati;
    //0   assenze usate nell'anno periodi chiusi consolidati (3)
    public Integer qtImputatePeriodiChiusiConsolidati;  
    //2   assenze usate nell'anno periodi aperti (4)
    public Integer qtImputatePeriodiAperti;             
    public Integer qtLimiteConsentito;                  //240 quantità periodo
    public Map<String, Integer> giorniConsolidatiMap;   //    giorni consolidati periodo
    public Map<String, Integer> giorniNoConsolidatiMap; //    giorni non consolidati periodo
    public Integer qtImputatePeriodiNonConsolidati;         //0   probabilmente la somma  (2) e (4)
    
    public Integer qtResiduaOreGiorni;                  //237 
    //presente in missioni
    
   
    @ToString
    public static class SituazioneCodiceAssenza implements Serializable {
      public int id;                                    //255
      public String codice;                             //92
      public String tipoCodice;                         //ASS       sempre ASS...
      public String descrizione;                        //missione
      public String tipoFrequenza;                      //G         O oraria G giornaliera
      public int qtFrequenza;                           //1         ore oppure 1 per giornaliera
      
      public String tipologia;                          
      //MSS       MSS(missione), GEN(lutto), Gen(rip.comp.), 661, APP(23, 23H7), FAC(32), FES(94)
      
      public Integer limiteMin;                         //0
      public Integer limiteMax;                         //240
    }
    
    public int usateOreGiorniAnno() {
      return qtImputatePeriodiChiusiNonConsolidati 
          + qtImputatePeriodiChiusiConsolidati + qtImputatePeriodiAperti;
    }
    
    /**
     * Estrae le date della situazione dipendente per quel codice di assenza.
     * Preleva le date dalle chiavi delle due mappe giorniConsolidatiMap e giorniNoConsolidatiMap.
     * Il campo value della mappa è un dato che modella le quantità e non ci interessa. 
     * 
     * @return le date utilizzate.
     */
    public Set<LocalDate> codeDates() {
      Set<LocalDate> dates = Sets.newHashSet();
      if (giorniConsolidatiMap != null) {
        for (String dateKey : giorniConsolidatiMap.keySet()) {
          dates.add(formatter(dateKey));
        }
      }
      
      if (giorniNoConsolidatiMap != null) {
        for (String dateKey : giorniNoConsolidatiMap.keySet()) {
          dates.add(formatter(dateKey));
        }
      }
      
      return dates;
    }
  }

  public static class SituazioneParametriControllo implements Serializable {

    public int id;
    public String descrizione;
    public int usateOreGiorni;
    public int qtImputatePeriodiChiusiNonConsolidati;
    public int qtImputatePeriodiChiusiConsolidati;
    public int qtImputatePeriodiAperti;
    public int qtLimiteConsentito;
    public Map<String, String> giorniAssenzaConsolidatiMap;
    public Map<String, String> giorniAssenzaNoConsolidatiMap;
    public int qtResiduaOreGiorni;
    public int qtImputatePeriodiNonConsolidati;
    
    /**
     * Estrae i codici e le rispettive date utilizzate dal controllo situazione.
     * Le mappe giorniAssenzaConsolidatiMap e giorniAssenzaNoConsolidatiMap hanno come
     * chiavi le date, e come valori il codice.
     * 
     * @return le date utilizzate.
     */
    public Map<String, Set<LocalDate>> codesDates() {
      Map<String, Set<LocalDate>> codesDates = Maps.newHashMap();
      
      //1) mappa consolidate
      if (giorniAssenzaConsolidatiMap != null) {
        for (String dateKey : giorniAssenzaConsolidatiMap.keySet()) {
          String code = giorniAssenzaConsolidatiMap.get(dateKey);
          Set<LocalDate> dates = codesDates.get(code);
          if (dates == null) {
            dates = Sets.newHashSet();
            codesDates.put(code, dates);
          }
          dates.add(formatter(dateKey));
        }
      }
      
      //2) mappa non consolidate
      if (giorniAssenzaNoConsolidatiMap != null) {
        for (String dateKey : giorniAssenzaNoConsolidatiMap.keySet()) {
          String code = giorniAssenzaNoConsolidatiMap.get(dateKey);
          Set<LocalDate> dates = codesDates.get(code);
          if (dates == null) {
            dates = Sets.newHashSet();
            codesDates.put(code, dates);
          }
          dates.add(formatter(dateKey));
        }
      }
      
      return codesDates;
    }
    
  }
  
  public static enum AbsenceImportType {

    //Situazioni da prelevare da SituazioneParametriControllo
    PERMESSO_PERSONALE_661(
        DefaultGroup.G_661.complation.replacingCodes,       //661H1, 66H2, ... , 661H9 
        "Legge 661"),                                       //descrizione in controllo situazione
        
    FERIE_ANNO_PRECEDENTE(
        ImmutableSet.of(DefaultAbsenceType.A_31, DefaultAbsenceType.A_37), 
        "Ferie Anno Precedente"),
    
    MALATTIA_PERSONALE(
        DefaultGroup.MALATTIA_3_ANNI.takable.takenCodes,    //111, ...capire se usare anche takable
        "Malattia Personale 100%");                         //descrizione in controllo situazione
    
    public Set<DefaultAbsenceType> codesFromSpc; 
    public String controlMatchPattern;
        
    private AbsenceImportType(Set<DefaultAbsenceType> codesFromSpc,
        String controlMatchPattern) {
      this.codesFromSpc = codesFromSpc;
      this.controlMatchPattern = controlMatchPattern;
    }
    
  }
  
  /**
   * Formatta la stringa e aggiunge l'ora utc-roma....
   * @param time esempio 2017-01-02T09:23:05.366+0000 da trasformare in utc+1
   * @return data
   */
  public static LocalDate formatter(String time) {
    final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    DateTime dt = dtf.parseDateTime(time);
    //DateTime dateTime = dt.toDateTime(DateTimeZone.UTC);
    LocalDate date = new LocalDate(dt);
    return date;
  }

}
