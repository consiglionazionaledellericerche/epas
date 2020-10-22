package models.absences;

import it.cnr.iit.epas.DateUtility;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import models.absences.JustifiedBehaviour.JustifiedBehaviourName;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;

@Audited
@Entity
@Table(name = "absence_types_justified_behaviours")
public class AbsenceTypeJustifiedBehaviour extends BaseModel {

  private static final long serialVersionUID = -3532986170397408935L;

  @Required
  @ManyToOne
  @JoinColumn(name = "absence_type_id")
  public AbsenceType absenceType;
  
  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "justified_behaviour_id")
  public JustifiedBehaviour justifiedBehaviour;
  
  @Getter
  @Column
  public Integer data;
  
  /**
   * Stampa la quantità di ore e minuti giustificata.
   * @return la stringa in cui stampare la quantità di ore e minuti giustificata.
   */
  public String printData() {
    if (justifiedBehaviour.name.equals(JustifiedBehaviourName.minimumTime) 
        || justifiedBehaviour.name.equals(JustifiedBehaviourName.maximumTime)) {
      return DateUtility.fromMinuteToHourMinute(data);
    }
    if (justifiedBehaviour.name.equals(JustifiedBehaviourName.takenPercentageTime)) {
      return DateUtility.fromMinuteToHourMinute(432 * data / 1000);
    }
    return data + "";
  }

}
