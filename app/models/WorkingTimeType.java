/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package models;

import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.DateTimeConstants;
import play.data.validation.Required;
import play.data.validation.Unique;


/**
 * Modello per le tipologie di orario di lavoro.
 *
 * @author Cristian Lucchesi
 * @author Dario Tagliaferri
 */
@Getter
@Setter
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
  private String description;

  @Getter
  @Required
  private Boolean horizontal;

  /**
   * True se il tipo di orario corrisponde ad un "turno di lavoro" false altrimenti.
   */
  private boolean shift = false;

  @Column(name = "meal_ticket_enabled")
  private boolean mealTicketEnabled = true;    //inutile

  @NotAudited
  @OneToMany(mappedBy = "workingTimeType")
  private List<ContractWorkingTimeType> contractWorkingTimeType = Lists.newArrayList();

  //@Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id")
  private Office office;

  @Column(name = "disabled")
  private boolean disabled = false;

  @Getter
  @OneToMany(mappedBy = "workingTimeType", fetch = FetchType.EAGER)
  @OrderBy("dayOfWeek")
  private List<WorkingTimeTypeDay> workingTimeTypeDays = new ArrayList<WorkingTimeTypeDay>();
  
  private boolean enableAdjustmentForQuantity = true;

  @Unique(value = "office, externalId")
  private String externalId;

  @NotAudited
  private LocalDateTime updatedAt;

  @PreUpdate
  @PrePersist
  private void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  @Override
  public String toString() {
    return description;
  }
  
  /**
   * Il tempo a lavoro medio giornaliero.
   *
   * @return tempo medio.
   */
  public int weekAverageWorkingTime() {
    int count = 0;
    int sum = 0;
    for (WorkingTimeTypeDay wttd : this.workingTimeTypeDays) {
      if (wttd.getWorkingTime() > 0) {
        sum += wttd.getWorkingTime();
        count++;
      }
    }
    return sum / count;
  }
  
  /**
   * Euristica per capire se il tipo orario è orizzontale.
   *
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

      if (wttd.isHoliday()) {
        continue;
      }

      if (workingTime == null) {
        workingTime = wttd.getWorkingTime();
        mealTicketTime = wttd.getMealTicketTime();
        breakTicketTime = wttd.getBreakTicketTime();
        afternoonThreshold = wttd.getTicketAfternoonThreshold();
        afternoonThresholdTime = wttd.getTicketAfternoonWorkingTime();
        continue;
      }

      if (!workingTime.equals(wttd.getWorkingTime())) {
        equal = false;
      }
      if (!mealTicketTime.equals(wttd.getMealTicketTime())) {
        equal = false;
      }
      if (!breakTicketTime.equals(wttd.getBreakTicketTime())) {
        equal = false;
      }
      if (!afternoonThreshold.equals(wttd.getTicketAfternoonThreshold())) {
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
   *
   * @return percentuale
   */
  public int percentEuristic() {
    int average = averageMinutesInWeek();
    if (average == 0) {
      return 100;
    }
    
    int percent = (average * 100) / WORKTIME_BASE;  
    return percent;
  }
  
  /**
   * Ritorna la media dei minuti lavorati in una settimana.
   *
   * @return la media dei minuti lavorati in una settimana.
   */
  public int averageMinutesInWeek() {
    int totalMinutes = 0;
    int totalDays = 0;
    for (WorkingTimeTypeDay workingTimeTypeDay : this.workingTimeTypeDays) {
      if (!workingTimeTypeDay.isHoliday()) {
        totalMinutes += workingTimeTypeDay.getWorkingTime();        
      }
      if (workingTimeTypeDay.getDayOfWeek() != DateTimeConstants.SATURDAY 
          && workingTimeTypeDay.getDayOfWeek() != DateTimeConstants.SUNDAY) {
        totalDays++;
      }
      
    }
    return totalMinutes / totalDays;
  }
    

}

