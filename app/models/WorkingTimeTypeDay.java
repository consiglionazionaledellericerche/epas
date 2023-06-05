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

import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;


/**
 * Per ogni giorno della settimana ci sono riportate le informazioni necessarie all'utilizzo di
 * questa tipologia di orario nel giorno specificato.
 *
 * @author Cristian Lucchesi
 * @author Dario Tagliaferri
 */
@Getter
@Setter
@ToString
@Audited
@Entity
@Table(name = "working_time_type_days")
public class WorkingTimeTypeDay extends BaseModel {

  private static final long serialVersionUID = 4622948996966018754L;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "working_time_type_id", nullable = false)
  private WorkingTimeType workingTimeType;

  @Getter
  @Required
  @Min(1)
  @Max(7)
  private int dayOfWeek;

  /**
   * tempo di lavoro giornaliero espresso in minuti.
   */
  @Getter
  @Required
  private Integer workingTime;

  /**
   * booleano per controllo se il giorno in questione è festivo o meno.
   */
  @Getter
  private boolean holiday = false;

  /**
   * tempo di lavoro espresso in minuti che conteggia se possibile usufruire del buono pasto.
   */
  @Required
  private Integer mealTicketTime = 0;

  @Required
  private Integer breakTicketTime = 0;

  /**
   * La soglia pomeridiana dopo la quale è necessario effettuare lavoro per avere diritto al buono
   * pasto.
   */
  @Getter
  private Integer ticketAfternoonThreshold = 0;

  /**
   * La quantità di lavoro dopo la soglia pomeridiana necessaria per avere diritto al buono pasto.
   */
  @Getter
  public Integer ticketAfternoonWorkingTime = 0;


  // Campi non utilizzati

  private Integer timeSlotEntranceFrom;
  private Integer timeSlotEntranceTo;
  private Integer timeSlotExitFrom;
  private Integer timeSlotExitTo;

  /**
   * tempo inizio pausa pranzo.
   */
  private Integer timeMealFrom;

  /**
   * tempo fine pausa pranzo.
   */
  private Integer timeMealTo;

  @NotAudited
  private LocalDateTime updatedAt;

  @PreUpdate
  @PrePersist
  private void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * True se è ammesso il calcolo del buono pasto per la persona, false altrimenti (il campo
   * mealTicketTime che rappresenta il tempo minimo di lavoro per avere diritto al buono pasto è
   * pari a zero).
   */
  @Transient
  public boolean mealTicketEnabled() {

    if (this.holiday) {
      return false;
    }
    if (this.mealTicketTime > 0) {
      return true;
    } else {
      return false;
    }
  }
  

}
