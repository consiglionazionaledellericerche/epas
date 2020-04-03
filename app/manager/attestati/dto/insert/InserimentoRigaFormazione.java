package manager.attestati.dto.insert;

import models.Certification;

public class InserimentoRigaFormazione extends RichiestaInserimentoAttestati {


  /**
   * Constructor.
   */
  public InserimentoRigaFormazione(Certification certification) {
    codiceSede = certification.person.office.codeId;
    anno = certification.year;
    mese = certification.month;

    ItemDipendente dipendente = new ItemDipendente();
    dipendenti.add(dipendente);

    dipendente.matricola = certification.person.number;
    InsertRigaFormazione rigaFormazione = new InsertRigaFormazione();
    rigaFormazione.giornoInizio = deserializeBegin(certification.content);
    rigaFormazione.giornoFine = deserializeEnd(certification.content);
    rigaFormazione.numOre = deserializeNumber(certification.content);

    dipendente.righeFormazione.add(rigaFormazione);
  }

  private int deserializeNumber(String key) {
    return Integer.parseInt(key.split(";")[2]);
  }

  private int deserializeBegin(String key) {
    return Integer.parseInt(key.split(";")[0]);
  }

  private int deserializeEnd(String key) {
    return Integer.parseInt(key.split(";")[1]);
  }

}
