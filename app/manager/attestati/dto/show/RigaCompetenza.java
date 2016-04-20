package manager.attestati.dto.show;

import com.google.common.base.MoreObjects;

public class RigaCompetenza {
  
  public int id;
  public String codiceCompetenza;
  public String numOre;
  
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(RigaCompetenza.class)
        .add("id", id)
        .add("codiceCompetenza", codiceCompetenza)
        .add("numOre", numOre)
        .toString();
  }
  
  public String serializeContent() {
    return this.codiceCompetenza + ";" + this.numOre;
  }
}
