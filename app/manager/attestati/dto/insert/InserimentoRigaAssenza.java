package manager.attestati.dto.insert;

import models.Certification;

public class InserimentoRigaAssenza extends RichiestaInserimentoAttestati {

  /**
   * Constructor.
   */
  public InserimentoRigaAssenza(Certification certification) {
    codiceSede = certification.person.office.codeId;
    anno = certification.year;
    mese = certification.month;

    ItemDipendente dipendente = new ItemDipendente();
    dipendenti.add(dipendente);

    dipendente.matricola = certification.person.number;
    InsertRigaAssenza rigaAssenza = new InsertRigaAssenza();
    rigaAssenza.codiceAssenza = deserializeCode(certification.content);
    rigaAssenza.giornoInizio = deserializeBegin(certification.content);
    rigaAssenza.giornoFine = deserializeEnd(certification.content);
    dipendente.righeAssenza.add(rigaAssenza);
  }

  private String deserializeCode(String key) {
    return key.split(";")[0];
  }

  private int deserializeBegin(String key) {
    return Integer.parseInt(key.split(";")[1]);
  }

  private int deserializeEnd(String key) {
    return Integer.parseInt(key.split(";")[2]);
  }

}
