package manager.attestati.dto.internal;

/**
 * Il Json caricato appena si clicca sul Mese in attestati. 
 * https://attestativ2.rm.cnr.it/api/rest/sede/listaDipendenti/223400/2017/3
 * Scarica un array con lo stato di sintesi dei dipendenti di cui vanno compilati gli attestati.
 * Attivati solo i campi utili:
 *   1) id è l'id del periodo     -> necessario per recuperare l'id attestati del dipendente
 *   2) dipendente.matricola
 *      dipendente.dataAssunzione
 *      dipendente.dataCessazione -> i campi con le date contrattuali considerate da attestati
 * 
 * @author alessandro
 *
 */
public class StatoAttestatoMese {

  public int id;                                        // 145872 id periodo
  public StatoDipendenteMese dipendente;                
  //public boolean controlliDisattivati;                //false 
  //public boolean controlliCompetenzaDisattivati;      //false
  //public boolean attestatoInizializzato;              //true
  //public String stato;                                //"INI"
  //public boolean datiParttimeAssenti;                 //false
  //public boolean inErrore;                            //false

  public static class StatoDipendenteMese {
    
    public int matricola;                               //17162           
    //public String nominativo;                         //TAGLIAFERRI DARIO
    public Long dataAssunzione;                         //1463349600000
    public Long dataCessazione;                         //1494799200000
    //public String currentCodiceOrario;                //"55" 

  }



}
