package models;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import play.data.validation.Required;
import play.data.validation.Unique;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;


/**
 * @author cristian
 * @author dario
 */
@Entity
@Audited
@Table(name="working_time_types")
public class WorkingTimeType extends BaseModel {

  private static final long serialVersionUID = -3443521979786226461L;

  @Required
  @Column(nullable = false)
  @Unique("office")
  public String description;

  @Required
  public Boolean horizontal;

  /**
   * True se il tipo di orario corrisponde ad un "turno di lavoro" false altrimenti
   */
  public boolean shift = false;

  @Column(name = "meal_ticket_enabled")
  public boolean mealTicketEnabled = true;    //inutile

  @NotAudited
  @OneToMany(mappedBy = "workingTimeType")
  public List<PersonWorkingTimeType> personWorkingTimeType = new ArrayList<PersonWorkingTimeType>();

  @NotAudited
  @OneToMany(mappedBy = "workingTimeType")
  public List<ContractWorkingTimeType> contractWorkingTimeType = new ArrayList<ContractWorkingTimeType>();

  //@Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id")
  public Office office;

  @Column(name = "disabled")
  public boolean disabled = false;

  @OneToMany(mappedBy = "workingTimeType", fetch = FetchType.EAGER)
  @OrderBy("dayOfWeek")
  public List<WorkingTimeTypeDay> workingTimeTypeDays = new ArrayList<WorkingTimeTypeDay>();

  @Override
  public String toString() {
    return description;
  }

  @Transient
  public boolean horizontalEuristic() {

    Integer workingTime = null;
    Integer mealTicketTime = null;
    Integer breakTicketTime = null;
    Integer afternoonThreshold = null;
    Integer afternoonThresholdTime = null;

    boolean equal = true;

    for (WorkingTimeTypeDay wttd : this.workingTimeTypeDays) {

      if (wttd.holiday) {
        continue;
      }

      if (workingTime == null) {
        workingTime = wttd.workingTime;
        mealTicketTime = wttd.mealTicketTime;
        breakTicketTime = wttd.breakTicketTime;
        afternoonThreshold = wttd.ticketAfternoonThreshold;
        afternoonThresholdTime = wttd.ticketAfternoonWorkingTime;
        continue;
      }

      if (!workingTime.equals(wttd.workingTime)) {
        equal = false;
      }
      if (!mealTicketTime.equals(wttd.mealTicketTime)) {
        equal = false;
      }
      if (!breakTicketTime.equals(wttd.breakTicketTime)) {
        equal = false;
      }
      if (!afternoonThreshold.equals(wttd.ticketAfternoonThreshold)) {
        equal = false;
      }
      if (!afternoonThresholdTime.equals(wttd.ticketAfternoonWorkingTime)) {
        equal = false;
      }
    }

    return equal;

  }

}

