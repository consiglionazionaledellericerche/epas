package manager.attestati.dto.drop;

import com.google.common.collect.Lists;
import java.util.List;
import models.Certification;

public class CancellazioneRigaFormazione {

  public int codiceSede;
  public int anno;
  public int mese;

  public List<ItemDipendente> dipendenti = Lists.newArrayList();

  public static class ItemDipendente {

    public String matricola;
    public List<DropItem> righeFormazione = Lists.newArrayList();
  }

  public static class DropItem {

    public int id;
  }

  /**
   * Constructor.
   */
  public CancellazioneRigaFormazione(Certification certification) {
    codiceSede = Integer.parseInt(certification.person.office.codeId);
    anno = certification.year;
    mese = certification.month;

    ItemDipendente dipendente = new ItemDipendente();
    dipendenti.add(dipendente);

    dipendente.matricola = certification.person.number;

    DropItem dropItem = new DropItem();
    dropItem.id = certification.attestatiId;
    dipendente.righeFormazione.add(dropItem);
  }

}
