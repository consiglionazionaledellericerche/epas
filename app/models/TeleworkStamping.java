package models;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;
import it.cnr.iit.epas.NullStringBinder;
import models.Stamping.WayType;
import models.base.BaseModel;
import models.enumerate.StampTypes;
import play.data.binding.As;
import play.data.validation.Required;
import play.db.jpa.Model;

@Audited
@Entity
@Table(name = "telework_stampings")
public class TeleworkStamping extends BaseModel {

  private static final long serialVersionUID = 1L;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(nullable = false, updatable = false)
  public PersonDay personDay;
  
  @Column(name = "stamp_type")
  @Enumerated(EnumType.STRING)
  public StampTypes stampType;
  
  @Required @NotNull
  @Column(nullable = false)
  public LocalDateTime date;

  @As(binder = NullStringBinder.class)
  public String note;
  
  /**
   * Fondamentale per far funzionare alcune drools.
   *
   * @return Restituisce il proprietario della timbratura.
   */
  public Person getOwner() {
    return personDay.person;
  }
  
  /**
   * Utile per effettuare i controlli temporali sulle drools.
   *
   * @return il mese relativo alla data della timbratura.
   */
  public YearMonth getYearMonth() {
    return new YearMonth(date.getYear(), date.getMonthOfYear());
  }
  
  /**
   * Orario formattato come HH:mm.
   * @return orario della timbratura formattato come HH:mm.
   */
  @Transient
  public String formattedHour() {
    if (this.date != null) {
      return date.toString("HH:mm");
    } else {
      return "";
    }
  }
  
}
