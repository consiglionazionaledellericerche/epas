package manager.attestati.dto;

import com.google.common.base.MoreObjects;

public class RigaFormazione {

  public int id;
  public String giornoInizio;
  public String giornoFine;
  public int numOre;
  
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(RigaFormazione.class)
        .add("id", id)
        .add("giornoInizio", giornoInizio)
        .add("giornoFine", giornoFine)
        .add("numOre",numOre)
        .toString();
  }
  
  public String serializeContent() {
    return this.giornoInizio + ";" + this.giornoFine + ";" + this.numOre;
  }
  
}
