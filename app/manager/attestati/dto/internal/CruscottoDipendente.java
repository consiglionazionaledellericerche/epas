package manager.attestati.dto.internal;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

import manager.attestati.dto.internal.PeriodoDipendente.PeriodoDipendenteDettagli;

import org.joda.time.LocalDate;

/**
 * Json caricato quando si richiede il cruscotto annuale di un dipendente.
 * https://attestativ2.rm.cnr.it/api/rest/dipendente/stato/cruscotto/11028/2017
 * 
 * @author alessandro
 *
 */
public class CruscottoDipendente {

  public int annoSituazione;                          //2017
  
  public PeriodoDipendenteDettagli dipendente;        //stessi dett. dipendente in PeriodoDipendente
  public SituazioneDipendenteAssenze[] situazioneDipendenteAssenze;
  public SituazioneParametriControllo[] situazioneParametriControllo;

  public static class SituazioneDipendenteCompetenze {
    
  }
  
  public static class SituazioneDipendenteAssenze {
    
    //public int dipendente.id;
    //public int dipendente.matricola;
    
    public int anno;                                //2017
    public SituazioneCodiceAssenza codice;
    public int usateOreGiorni;                          //0   assenze usate nel absencePeriod prima dell'anno richiesto (1) 
    public int qtImputatePeriodiChiusiNonConsolidati;   //1   assenze usate nell'anno richiesto periodi chiusi ma non consolidati (2) esempio dei 18...
    public int qtImputatePeriodiChiusiConsolidati;      //0   assenze usate nell'anno periodi chiusi consolidati (3)
    public int qtImputatePeriodiAperti;                 //2   assenze usate nell'anno periodi aperti (4)
    public int qtLimiteConsentito;                      //240 quantità periodo
    public Map<String, Integer> giorniConsolidatiMap;   //    giorni consolidati periodo
    public Map<String, Integer> giorniNoConsolidatiMap; //    giorni non consolidati periodo
    public int qtImputatePeriodiNonConsolidati;         //0   probabilmente la somma  (2) e (4)
    
    public Integer qtResiduaOreGiorni;                  //237 
    //presente in missioni
    
    public static class SituazioneCodiceAssenza {
      public int id;                                    //255
      public String codice;                             //92
      public String tipoCodice;                         //ASS       sempre ASS...
      public String descrizione;                        //missione
      public String tipoFrequenza;                      //G         O oraria G giornaliera
      public int qtFrequenza;                           //1         ore oppure 1 per giornaliera
      public String tipologia;                          //MSS       MSS(missione), GEN(lutto), Gen(rip.comp.), 661, APP(23, 23H7), FAC(32), FES(94)
      public Integer limiteMin;                         //0
      public Integer limiteMax;                         //240
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
    public int qtResiduoOreGiorni;
    public int qtImputatePeriodiNonConsolidati;

  }

}
