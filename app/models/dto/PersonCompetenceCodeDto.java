package models.dto;


public class PersonCompetenceCodeDto {

  public Long personId;
  public Long competenceCodeId;
  
  
  public PersonCompetenceCodeDto(Long id, Long id2) {
    this.personId = id;
    this.competenceCodeId = id2;
  }
}
