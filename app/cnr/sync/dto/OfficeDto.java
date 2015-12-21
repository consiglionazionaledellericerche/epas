package cnr.sync.dto;

import models.Office;

public class OfficeDto {
  public int id;
  public String name;
  public String code;
  public String codeId;
  public boolean isHeadQuarters;
  public InstituteDto institute;
  public String dismissionDate;

  /**
   * Copia i dati dell'ufficio passato nell'ufficio corrente.
   * @param office l'ufficio da cui copiare i dati
   */
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
