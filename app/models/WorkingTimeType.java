package models;

import com.google.common.collect.Lists;

import lombok.Getter;

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
 * Modello per le tipologie di orario di lavoro.
 *
 * @author cristian
 * @author dario
 */
@Entity
@Audited
@Table(name = "working_time_types")
public class WorkingTimeType extends BaseModel {

  private static final long serialVersionUID = -3443521979786226461L;
  
  //serve per il calcolo della percentuale part time
  public static final int WORKTIME_BASE = 432;

  @Getter
  @Required
  @Column(nullable = false)
  @Unique("office")
  public String description;

  @Getter
  @Required
  public Boolean horizontal;

  /**
   * True se il tipo di orario corrisponde ad un "turno di lavoro" false altrimenti.
   */
  public boolean shift = false;

  @Column(name = "meal_ticket_enabled")
  public boolean mealTicketEnabled = true;    //inutile

  @NotAudited
  @OneToMany(mappedBy = "workingTimeType")
  public List<ContractWorkingTimeType> contractWorkingTimeType = Lists.newArrayList();

  //@Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id")
  public Office office;

  @Column(name = "disabled")
  public boolean disabled = false;

  @Getter
  @OneToMany(mappedBy = "workingTimeType", fetch = FetchType.EAGER)
  @OrderBy("dayOfWeek")
  public List<WorkingTimeTypeDay> workingTimeTypeDays = new ArrayList<WorkingTimeTypeDay>();

  @Override
  public String toString() {
    return description;
  }
  
  /**
   * Il tempo a lavoro medio giornaliero. 
   * @return tempo medio.
   */
  public int weekAverageWorkingTime() {
    int count = 0;
    int sum = 0;
    for (WorkingTimeTypeDay wttd : this.workingTimeTypeDays) {
      if (wttd.workingTime > 0) {
        sum += wttd.workingTime;
        count++;
      }
    }
    return sum / count;
  }
  
  /**
   * Euristica per capire se il tipo orario è orizzontale.
   * @return esito
   */
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
  
  /**
   * Calcola il tempo percentuale di part time.
   * @return percentuale
   */
  public int percentEuristic() {
    int totalMinutes = 0;
    int totalDays = 0;
    for (WorkingTimeTypeDay workingTimeTypeDay : this.workingTimeTypeDays) {
      if (!workingTimeTypeDay.holiday) {
        totalMinutes += workingTimeTypeDay.workingTime;
        totalDays++;
      }
    }
    if (totalDays == 0) {
      return 100;
    }
    
    int percent = ((totalMinutes / totalDays) * 100) / WORKTIME_BASE;  
    return percent;
  }
    

}

