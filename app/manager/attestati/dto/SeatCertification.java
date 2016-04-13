package manager.attestati.dto;

import com.google.common.base.MoreObjects;

import com.beust.jcommander.internal.Lists;

import models.Contract;

import java.util.List;

/**
 * Json sulla situazione della persona per il mese specificato da Attestati.
 * @author alessandro
 *
 */
public class SeatCertification {

  public int codiceSede;
  public int anno;
  public int mese;
  public List<PersonCertification> dipendenti = Lists.newArrayList();
  
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(SeatCertification.class)
        .add("codiceSede", codiceSede)
        .add("anno", anno)
        .add("mese", mese)
        .add("dipendenti", dipendenti)
        .toString();
  }
  
  public static class PersonCertification {
    public int matricola;
    public List<RigaAssenza> righeAssenza;
    
    @Override
    public String toString() {
      return MoreObjects.toStringHelper(PersonCertification.class)
          .add("matricola", matricola)
          .add("righeAssenza", righeAssenza)
          .toString();
    }
  }
  
  public static class RigaAssenza {
    public int id;
    public String codiceAssenza;
    public int giornoInizio;
    public int giornoFine;
    
    @Override
    public String toString() {
      return MoreObjects.toStringHelper(RigaAssenza.class)
          .add("id", id)
          .add("codiceAssenza", codiceAssenza)
          .add("giornoInizio", giornoInizio)
          .add("giornoFine", giornoFine)
          .toString();
    }
    
  }
  
}
