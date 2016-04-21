package manager.attestati.dto.insert;

import models.Certification;

public class InserimentoRigaBuoniPasto extends RichiestaInserimentoAttestati {

  /**
   * Constructor.
   * @param certification
   */
  public InserimentoRigaBuoniPasto(Certification certification) {
    this.codiceSede = Integer.parseInt(certification.person.office.codeId);
    this.anno = certification.year;
    this.mese = certification.month;
    
    ItemDipendente dipendente = new ItemDipendente();
    this.dipendenti.add(dipendente);
    
    dipendente.matricola = certification.person.number;
    dipendente.numBuoniPasto = Integer.parseInt(certification.content);
  }
  
}
