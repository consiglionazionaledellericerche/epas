/**
 *
 */
package models;

import models.base.BaseModel;

import play.data.validation.Required;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;


/**
 * @author cristian
 */
@Entity
@Table(name = "vacation_codes")
public class VacationCode extends BaseModel {

  private static final long serialVersionUID = 6182486562743326271L;

  //modificare quindi anche la funzione di popolamento. ci devono essere delle update ogni volta che per quell'id
  //viene trovato un nuovo piano ferie (ovvero se per quell'id ce n'è più di uno).


  @OneToMany(mappedBy = "vacationCode")
  public List<VacationPeriod> vacationPeriod;

  @Required
  public String description;

  @Required
  @Column(name = "vacation_days")
  public Integer vacationDays;

  @Required
  @Column(name = "permission_days")
  public Integer permissionDays;

  @Override
  public String toString() {
    return String.format("VacationCode[%s] - description = %s, vacationDays = %d, permissionDays = %d",
            id, description, vacationDays, permissionDays);
  }
}
