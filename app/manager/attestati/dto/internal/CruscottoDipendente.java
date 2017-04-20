package manager.attestati.dto.internal;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import lombok.ToString;

import manager.attestati.dto.internal.PeriodoDipendente.PeriodoDipendenteDettagli;

import org.assertj.core.util.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Json caricato quando si richiede il cruscotto annuale di un dipendente.
 * https://attestativ2.rm.cnr.it/api/rest/dipendente/stato/cruscotto/11028/2017
 * 
 * @author alessandro
 *
 */
@ToString
public class CruscottoDipendente {

  public int annoSituazione;                          //2017
  
  public PeriodoDipendenteDettagli dipendente;        //stessi dett. dipendente in PeriodoDipendente
  public SituazioneDipendenteAssenze[] situazioneDipendenteAssenze;
  public SituazioneParametriControllo[] situazioneParametriControllo;

  public static class SituazioneDipendenteCompetenze {
    
  }
  
  @ToString
  public static class SituazioneDipendenteAssenze {
    
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
    public static class SituazioneCodiceAssenza {
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
    public List<LocalDate> codeDates() {
      List<LocalDate> dates = Lists.newArrayList();
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

  public static class SituazioneParametriControllo {

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
    public Map<String, List<LocalDate>> codesDates() {
      Map<String, List<LocalDate>> codesDates = Maps.newHashMap();
      
      //1) mappa consolidate
      if (giorniAssenzaConsolidatiMap != null) {
        for (String dateKey : giorniAssenzaConsolidatiMap.keySet()) {
          String code = giorniAssenzaConsolidatiMap.get(dateKey);
          List<LocalDate> dates = codesDates.get(code);
          if (dates == null) {
            dates = Lists.newArrayList();
            codesDates.put(code, dates);
          }
          dates.add(formatter(dateKey));
        }
      }
      
      //2) mappa non consolidate
      if (giorniAssenzaNoConsolidatiMap != null) {
        for (String dateKey : giorniAssenzaNoConsolidatiMap.keySet()) {
          String code = giorniAssenzaNoConsolidatiMap.get(dateKey);
          List<LocalDate> dates = codesDates.get(code);
          if (dates == null) {
            dates = Lists.newArrayList();
            codesDates.put(code, dates);
          }
          dates.add(formatter(dateKey));
        }
      }
      
      return codesDates;
    }
    
  }
  
  /**
   * Formatta la stringa e aggiunge l'ora utc-roma....
   * @param time esempio 2017-01-02T09:23:05.366+0000 da trasformare in utc+1
   * @return data
   */
  public static LocalDate formatter(String time) {
    final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    LocalDateTime dt = dtf.parseLocalDateTime(time.substring(0, 23));
    dt = dt.plusHours(2);
    LocalDate date = new LocalDate(dt);
    return date;
  }

}
