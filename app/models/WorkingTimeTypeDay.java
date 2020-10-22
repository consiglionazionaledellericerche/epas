package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.ToString;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;


/**
 * Per ogni giorno della settimana ci sono riportate le informazioni necessarie all'utilizzo di
 * questa tipologia di orario nel giorno specificato.
 *
 * @author cristian
 * @author dario
 */
@ToString
@Audited
@Entity
@Table(name = "working_time_type_days")
public class WorkingTimeTypeDay extends BaseModel {

  private static final long serialVersionUID = 4622948996966018754L;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "working_time_type_id", nullable = false)
  public WorkingTimeType workingTimeType;

  @Getter
  @Required
  @Min(1)
  @Max(7)
  public int dayOfWeek;

  /**
   * tempo di lavoro giornaliero espresso in minuti.
   */
  @Getter
  @Required
  public Integer workingTime;

  /**
   * booleano per controllo se il giorno in questione è festivo o meno.
   */
  @Getter
  public boolean holiday = false;

  /**
   * tempo di lavoro espresso in minuti che conteggia se possibile usufruire del buono pasto.
   */
  @Getter
  @Required
  public Integer mealTicketTime = 0;

  @Getter
  @Required
  public Integer breakTicketTime = 0;

  /**
   * La soglia pomeridiana dopo la quale è necessario effettuare lavoro per avere diritto al buono
   * pasto.
   */
  @Getter
  public Integer ticketAfternoonThreshold = 0;

  /**
   * La quantità di lavoro dopo la soglia pomeridiana necessaria per avere diritto al buono pasto.
   */
  @Getter
  public Integer ticketAfternoonWorkingTime = 0;


  // Campi non utilizzati

  public Integer timeSlotEntranceFrom;
  public Integer timeSlotEntranceTo;
  public Integer timeSlotExitFrom;
  public Integer timeSlotExitTo;

  /**
   * tempo inizio pausa pranzo.
   */
  public Integer timeMealFrom;

  /**
   * tempo fine pausa pranzo.
   */
  public Integer timeMealTo;


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
