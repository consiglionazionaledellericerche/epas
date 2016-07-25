package models;

import models.base.BaseModel;
import models.enumerate.AccumulationType;
import models.enumerate.LimitType;

import play.data.validation.Required;
import play.data.validation.Unique;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;


/**
 * tabella di decodifica dei codici di competenza
 *
 * @author dario.
 */
@Entity
@Table(name = "competence_codes")
public class CompetenceCode extends BaseModel {

  private static final long serialVersionUID = 9211205948423608460L;

  @OneToMany(mappedBy = "competenceCode")
  public List<Competence> competence;

  @ManyToMany(mappedBy = "competenceCode")
  public List<Person> persons;
  
  @ManyToOne
  @JoinColumn(name = "competence_code_group_id")
  public CompetenceCodeGroup competenceCodeGroup;

  @Required
  @Unique
  public String code;

  @Column
  public String codeToPresence;

  @Required
  public String description;
  
  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "limit_type")
  public LimitType limitType;
  
  @Column(name = "limit_value")
  public Integer limitValue;

  @Override
  public String toString() {
    return String.format("CompetenceCode[%d] - description = %s", id, description);
  }
  
  @Override
  public String getLabel() {
    return this.code + " - " + this.description;
  }
  

}
