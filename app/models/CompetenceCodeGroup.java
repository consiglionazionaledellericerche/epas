package models;

import models.base.BaseModel;
import models.enumerate.LimitType;

import play.data.validation.Required;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "competence_code_groups")
public class CompetenceCodeGroup extends BaseModel{

  @OneToMany(mappedBy = "competenceCodeGroup")
  public List<CompetenceCode> competenceCodes;
  
  @Required
  public String label;
  
  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "limit_type")
  public LimitType limitType;
  
  @Column(name = "limit_value")
  public Integer limitValue;
  

}
