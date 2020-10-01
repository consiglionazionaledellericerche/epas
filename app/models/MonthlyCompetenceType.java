package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import models.base.BaseModel;
import play.data.validation.Required;

@Audited
@Table(name = "monthly_competence_type")
@Entity
public class MonthlyCompetenceType extends BaseModel{
  
  private static final long serialVersionUID = -298105801035472529L;
  
  public String name;
  
  
  @Required
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "workdays_code", nullable = false)
  public CompetenceCode workdaysCode;
  
  
  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "holidays_code", nullable = false)
  public CompetenceCode holidaysCode;
  
  @Override
  public String toString() {
    return name;
  }
}
