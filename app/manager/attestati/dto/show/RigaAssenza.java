package manager.attestati.dto.show;

import com.google.common.base.MoreObjects;

public class RigaAssenza {

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
  
  public String serializeContent() {
    return this.codiceAssenza + ";" + this.giornoInizio + ";" + this.giornoFine;
  }
}
