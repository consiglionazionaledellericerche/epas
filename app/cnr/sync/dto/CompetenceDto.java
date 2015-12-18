package cnr.sync.dto;

import com.google.common.base.Function;

import models.Competence;

public class CompetenceDto {
  public int year;
  public int month;
  public String code;
  public int valueApproved;

  public enum FromCompetence implements Function<Competence, CompetenceDto> {
    ISTANCE;

    @Override
    public CompetenceDto apply(Competence competence) {
      CompetenceDto competenceDto = new CompetenceDto();
      competenceDto.year = competence.year;
      competenceDto.month = competence.month;
      competenceDto.code = competence.competenceCode.code;
      competenceDto.valueApproved = competence.valueApproved;
      return competenceDto;
    }
  }
}
