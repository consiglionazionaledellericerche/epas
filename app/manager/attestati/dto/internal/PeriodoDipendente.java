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

package manager.attestati.dto.internal;

import java.io.Serializable;

/**
 * Json caricato quando si clicca su un nominativo a partire dall'elenco dei dipendenti
 * della sede.
 * https://attestativ2.rm.cnr.it/api/rest/dipendente/periodo/145872
 * Scarica lo stato attestati del periodo (inerente anno e mese) del dipendente.
 * Attivati solo i campi utili:
 * 1) dipendente.id -> id dipendente in attestati, necessario per aprire il suo cruscotto
 *
 * @author Alessandro Martelli
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
  
  
  /**
   * Rappresenta i dettagli di un periodo di un dipendente.
   */
  public static class PeriodoDipendenteDettagli implements Serializable {

    private static final long serialVersionUID = 8981830602033609237L;

    public int id;                                  //id dipendente 
    public int matricola;                         //17162
    //public SedePeriodoDipendente sede;
    public String nominativo;                     //TAGLIAFERRI DARIO
    //public Long decorrenzaIniziale;               //1463349600000
    public Long dataAssunzione;                   //1463349600000
    public Long dataCessazione;                   //1494799200000
    //public ContrattoPeriodoDipendente contratto;
    //public String profiloDipendente;              //"064"
    //public String lastPeriodoConsolidato;         //"201701"
    //public String currentCodiceOrario;            //"55"

  }

}