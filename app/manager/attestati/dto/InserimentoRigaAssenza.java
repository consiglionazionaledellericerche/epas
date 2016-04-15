package manager.attestati.dto;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import models.Certification;

import java.util.List;

public class InserimentoRigaAssenza {

  public static class ItemRigaAssenza {
    public String codiceAssenza;
    public int giornoInizio;
    public int giornoFine;
  }
  
  public static class ItemDipendente {
    public int matricola;
    public List<ItemRigaAssenza> righeAssenza = Lists.newArrayList();
  }
  
  public int codiceSede;
  public int anno;
  public int mese;
  
  public List<ItemDipendente> dipendenti = Lists.newArrayList();
  
  /**
   * Constructor.
   * @param certification
   */
  public InserimentoRigaAssenza(Certification certification) {
    this.codiceSede = Integer.parseInt(certification.person.office.codeId);
    this.anno = certification.year;
    this.mese = certification.month;
    
    ItemDipendente dipendente = new ItemDipendente();
    this.dipendenti.add(dipendente);
    
    dipendente.matricola = certification.person.number;
    ItemRigaAssenza rigaAssenza = new ItemRigaAssenza();
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
