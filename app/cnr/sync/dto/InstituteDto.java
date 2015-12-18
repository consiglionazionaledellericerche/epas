package cnr.sync.dto;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;

import models.Institute;

public class InstituteDto {
  public int id;
  public String name;
  public String code;
  public String cds;
  public String dismissionDate;

  @Override
  public boolean equals(Object obj) {
    if (obj != null && obj instanceof InstituteDto) {
      final InstituteDto other = (InstituteDto) obj;
      return cds.equals(other.cds) | code.equals(other.code);
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
            + ((cds == null) ? 0 : cds.toUpperCase().replace(" ", "").hashCode());
    result = prime * result
            + ((code == null || code.equals("0")) ? 0 : code.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("id", id).add("name", name).add("code", code).add("cds", cds)
            .toString();
  }

  public enum ToInstitute implements Function<InstituteDto, Institute> {
    ISTANCE;

    @Override
    public Institute apply(InstituteDto instituteDto) {
      Institute institute = new Institute();
      institute.name = instituteDto.name;
      institute.code = instituteDto.code;
      institute.cds = instituteDto.cds;
      return institute;
    }
  }
}
