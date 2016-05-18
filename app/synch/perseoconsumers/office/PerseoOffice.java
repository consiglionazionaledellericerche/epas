package synch.perseoconsumers.office;

import com.google.common.base.MoreObjects;

public class PerseoOffice {
  
  public int id; //perseoId
  public String shortName;
  public String code;
  public String codeId;
  public String city;
  public String street;
  public boolean isHeadQuarters;
  public PerseoInstitute institute;
  public String dismissionDate;

  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id).add("shortName", shortName).add("code", code).add("codeId", codeId)
        .toString();
  }
}
