package manager.attestati.dto.insert;

import models.Certification;

public class InserimentoRigaCompetenza extends RichiestaInserimentoAttestati {

  /**
   * Constructor.
   * @param certification
   */
  public InserimentoRigaCompetenza(Certification certification) {
    this.codiceSede = Integer.parseInt(certification.person.office.codeId);
    this.anno = certification.year;
    this.mese = certification.month;
    
    ItemDipendente dipendente = new ItemDipendente();
    this.dipendenti.add(dipendente);
    
    dipendente.matricola = certification.person.number;
    InsertRigaCompetenza rigaCompetenza = new InsertRigaCompetenza();
    rigaCompetenza.codiceCompetenza = deserializeCode(certification.content);
    rigaCompetenza.numOre = deserializeNumber(certification.content);
    dipendente.righeCompetenza.add(rigaCompetenza);
  }
  
  private String deserializeCode(String key) {
    return key.split(";")[0];
  }
  
  private int deserializeNumber(String key) {
    return Integer.parseInt(key.split(";")[1]);
  }
  
}
