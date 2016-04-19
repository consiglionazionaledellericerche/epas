package manager.attestati.dto.drop;

import com.google.common.collect.Lists;

import manager.attestati.dto.drop.CancellazioneRigaAssenza.DropItem;
import manager.attestati.dto.drop.CancellazioneRigaAssenza.ItemDipendente;

import models.Certification;

import java.util.List;

public class CancellazioneRigaFormazione {

  public int codiceSede;
  public int anno;
  public int mese;
  
  public List<ItemDipendente> dipendenti = Lists.newArrayList();
    
  public static class ItemDipendente {
    
    public int matricola;
    public List<DropItem> righeFormazione = Lists.newArrayList();
  }
  
  public static class DropItem {
    public int id;
  }
  /**
   * Constructor.
   * @param certification
   */
  public CancellazioneRigaFormazione(Certification certification) {
    this.codiceSede = Integer.parseInt(certification.person.office.codeId);
    this.anno = certification.year;
    this.mese = certification.month;
    
    ItemDipendente dipendente = new ItemDipendente();
    this.dipendenti.add(dipendente);
    
    dipendente.matricola = certification.person.number;

    DropItem dropItem = new DropItem();
    dropItem.id = certification.attestatiId;
    dipendente.righeFormazione.add(dropItem);
  }
  
}
