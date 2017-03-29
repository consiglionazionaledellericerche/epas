package manager.attestati.dto.internal;

import com.google.common.collect.Lists;

import java.util.List;

import org.joda.time.LocalDate;

/**
 * Json caricato quando si clicca su un nominativo a partire dall'elenco dei dipendenti
 * della sede.
 * https://attestativ2.rm.cnr.it/api/rest/dipendente/periodo/145872
 * Scarica lo stato attestati del periodo (inerente anno e mese) del dipendente.
 * Attivati solo i campi utili:
 * 1) dipendente.id -> id dipendente in attestati, necessario per aprire il suo cruscotto
 * 
 * @author alessandro
 *
 */
public class PeriodoDipendente {

  //public int id;                                    //145872 (id del periodo)
  public PeriodoDipendenteDettagli dipendente;
  //public String periodo;                            //201703
  //public boolean controlliDisattivati;              //false
  //public boolean controlliCompetenzaDisattivati;    //false
  //public String stato;                              //"INI"
  //public String datiParttimeAssenti;                //false
  
  //... dati sulle righe caricate nel periodo ...     [...]
  
  //public int oreStraordinarioFatte;                 //0
  //public int tettoMatricola;                        //200
  //public boolean inErrore;                          //false
  //public boolean datiParttimeAssenti;               //false
  
  
  public static class PeriodoDipendenteDettagli {
    
    public int id;                                  //id dipendente 
    //public int matricola;                         //17162
    //public SedePeriodoDipendente sede;
    //public String nominativo;                     //TAGLIAFERRI DARIO
    //public Long decorrenzaIniziale;               //1463349600000
    //public Long dataAssunzione;                   //1463349600000
    //public Long dataCessazione;                   //1494799200000
    //public ContrattoPeriodoDipendente contratto;
    //public String profiloDipendente;              //"064"
    //public String lastPeriodoConsolidato;         //"201701"
    //public String currentCodiceOrario;            //"55"

    
    public static class SedePeriodoDipendente {
      //public int id;                              //79
      //public String codiceSede;                   //"223400"
      //public String descrizioneSede;              //"ISTITUTO DI INFORMATICA E TELEMATICA"
      //public boolean sedeDisagiata;               //false  
    }
    
    public static class ContrattoPeriodoDipendente {
      //public String id;                           //"CL0609"
    }

  }



}
