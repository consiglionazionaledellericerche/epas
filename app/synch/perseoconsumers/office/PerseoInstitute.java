package synch.perseoconsumers.office;

import com.google.common.base.MoreObjects;

public class PerseoInstitute {
  public int id; //perseoId
  public String name;
  public String code;
  public String cds;
  public String dismissionDate;

  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("id", id).add("name", name).add("code", code).add("cds", cds)
            .toString();
  }
 
}
