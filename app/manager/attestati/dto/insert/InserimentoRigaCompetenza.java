package manager.attestati.dto.insert;

import models.Certification;

public class InserimentoRigaCompetenza extends RichiestaInserimentoAttestati {

  /**
   * Constructor.
   */
  public InserimentoRigaCompetenza(Certification certification) {
    codiceSede = certification.person.office.codeId;
    anno = certification.year;
    mese = certification.month;

    ItemDipendente dipendente = new ItemDipendente();
    dipendenti.add(dipendente);

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
