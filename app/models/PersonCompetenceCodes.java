package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import models.base.PeriodModel;
import play.data.validation.Required;

/**
 * Relazione tra persona e competenze abilitate.
 * @author dario
 *
 */
@Entity
@Table(name = "persons_competence_codes")
public class PersonCompetenceCodes extends PeriodModel {

  private static final long serialVersionUID = 1769306446762966211L;

  @Required
  @ManyToOne
  @JoinColumn(name = "person_id")
  public Person person;
  
  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "competence_code_id")
  public CompetenceCode competenceCode;

  @Override
  public String toString() {
    return String.format(
        "PersonCompetenceCodes[%d] - person.name = %s, competenceCode = %s, "
        + "beginDate = %s, endDate = %s",
         id, person.fullName(), competenceCode.code, beginDate, endDate);
  }
}
