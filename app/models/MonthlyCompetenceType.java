package models;

import com.google.common.collect.Lists;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;

/**
 * Rappresenta le possibili tipologie di competenze mensili.
 */
@Getter
@Setter
@Audited
@Table(name = "monthly_competence_type")
@Entity
public class MonthlyCompetenceType extends BaseModel {
  
  private static final long serialVersionUID = -298105801035472529L;
  
  private String name;
  
  @OneToMany(mappedBy = "monthlyCompetenceType")
  private List<PersonReperibilityType> personReperibilityTypes = Lists.newArrayList();
  
  @Required
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "workdays_code", nullable = false)
  private CompetenceCode workdaysCode;  
  
  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "holidays_code", nullable = false)
  private CompetenceCode holidaysCode;
  
  /**
   * Transiente che ritorna i codici associati all'attività.
   *
   * @return la lista di codici di competenza feriale e festivo per l'attività.
   */
  @Transient
  public List<CompetenceCode> getCodesForActivity() {
    List<CompetenceCode> list = Lists.newArrayList();
    list.add(workdaysCode);
    list.add(holidaysCode);
    return list;
  }
  
  @Override
  public String toString() {
    return name;
  }
}
