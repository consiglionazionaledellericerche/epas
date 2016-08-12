package models;

import models.base.BaseModel;
import models.enumerate.LimitType;
import models.enumerate.LimitUnit;

import play.data.validation.Required;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import com.beust.jcommander.internal.Lists;

/**
 * I gruppi servono per descrivere comportamenti e limiti comuni a più
 * codici di competenza.
 * 
 * @author dario
 *
 */
@Audited
@Entity
@Table(name = "competence_code_groups")
public class CompetenceCodeGroup extends BaseModel {

  private static final long serialVersionUID = 6486248571013912369L;

  @OneToMany(mappedBy = "competenceCodeGroup")
  public List<CompetenceCode> competenceCodes = Lists.newArrayList();
  
  @Required
  public String label;
  
  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "limit_type")
  public LimitType limitType;
  
  @Column(name = "limit_value")
  public Integer limitValue;
  
 
  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "limit_unit")
  public LimitUnit limitUnit;
  

}
