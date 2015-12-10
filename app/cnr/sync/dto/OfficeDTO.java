package cnr.sync.dto;

import models.Office;

public class OfficeDTO {
  public int id;
  public String name;
  public String code;
  public String codeId;
  public boolean isHeadQuarters;
  public InstituteDTO institute;
  public String dismissionDate;

  public void copyInto(Office office) {
    office.name = name;
    office.code = code;
    office.codeId = codeId;
    office.headQuarter = isHeadQuarters;
  }

  @Override
  public String toString() {
    return String.format("%s - %s - %s", name, code, codeId);
  }
}
