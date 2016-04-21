package manager.attestati.dto.drop;

import com.google.common.collect.Lists;

import models.Certification;

import java.util.List;

public class CancellazioneRigaAssenza {
  
  public int codiceSede;
  public int anno;
  public int mese;
  
  public List<ItemDipendente> dipendenti = Lists.newArrayList();
    
  public static class ItemDipendente {
    
    public int matricola;
    public List<DropItem> righeAssenza = Lists.newArrayList();
  }
  
  public static class DropItem {
    public int id;
  }
  
  /**
   * Constructor.
   * @param certification
   */
  public CancellazioneRigaAssenza(Certification certification) {
    this.codiceSede = Integer.parseInt(certification.person.office.codeId);
    this.anno = certification.year;
    this.mese = certification.month;
    
    ItemDipendente dipendente = new ItemDipendente();
    this.dipendenti.add(dipendente);
    
    dipendente.matricola = certification.person.number;
    
    DropItem dropItem = new DropItem();
    dropItem.id = certification.attestatiId;
    dipendente.righeAssenza.add(dropItem);
  }

  
}
