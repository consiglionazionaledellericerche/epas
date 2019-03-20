package manager.attestati.dto.insert;

import models.Certification;

public class InserimentoRigaBuoniPasto extends RichiestaInserimentoAttestati {

  /**
   * Constructor.
   */
  public InserimentoRigaBuoniPasto(Certification certification) {
    codiceSede = Integer.parseInt(certification.person.office.codeId);
    anno = certification.year;
    mese = certification.month;

    ItemDipendente dipendente = new ItemDipendente();
    dipendenti.add(dipendente);

    dipendente.matricola = certification.person.number;
    dipendente.numBuoniPasto = Integer.parseInt(certification.content);
  }

}
