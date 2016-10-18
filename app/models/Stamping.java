package models;

import com.google.common.base.MoreObjects;

import it.cnr.iit.epas.NullStringBinder;

import models.base.BaseModel;
import models.enumerate.StampTypes;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;

import play.data.binding.As;
import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;


/**
 * @author cristian.
 */
@Audited
@Entity
@Table(name = "stampings")

public class Stamping extends BaseModel implements Comparable<Stamping> {


  private static final long serialVersionUID = -2422323948436157747L;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "personDay_id", nullable = false, updatable = false)
  public PersonDay personDay;

  @Column(name = "stamp_type")
  @Enumerated(EnumType.STRING)
  public StampTypes stampType;

  @ManyToOne(optional = true)
  @JoinColumn(name = "stamp_modification_type_id")
  public StampModificationType stampModificationType;

  @Column(nullable = false)
  public LocalDateTime date;

  @Required
  @Enumerated(EnumType.STRING)
  public WayType way;

  @As(binder = NullStringBinder.class)
  public String note;

  /**
   * questo campo booleano consente di determinare se la timbratura è stata effettuata dall'utente
   * all'apposita macchinetta (valore = false) o se è stato l'amministratore a settare l'orario di
   * timbratura poichè la persona in questione non ha potuto effettuare la timbratura (valore =
   * true).
   */
  @Column(name = "marked_by_admin")
  public boolean markedByAdmin = false;

  /**
   * con la nuova interpretazione delle possibilità del dipendente, questo campo viene settato a
   * true quando è il dipendente a modificare la propria timbratura.
   */
  @Column(name = "marked_by_employee")
  public boolean markedByEmployee = false;
  /**
   * true, cella bianca; false, cella gialla.
   */
  @Transient
  public boolean valid;
  @Transient
  public int pairId = 0;

  /**
   * true, la cella fittizia di uscita adesso.
   */
  @Transient
  public boolean exitingNow = false;

  @Transient
  public boolean isValid() {
    return this.valid;
  }

  @Transient
  public boolean isIn() {
    return way.equals(WayType.in);
  }

  @Transient
  public boolean isOut() {
    return way.equals(WayType.out);
  }

  /**
   * Costruttore.
   *
   * @param personDay personDay
   * @param time      time
   */
  public Stamping(PersonDay personDay, LocalDateTime time) {
    this.personDay = personDay;
    this.date = time;
  }

  /**
   * Costruttore.
   *
   * @param personDay personDay
   * @param time      time
   */
  public Stamping(PersonDay personDay, WayType way, LocalDateTime time) {
    this.personDay = personDay;
    this.date = time;
    this.way = way;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).omitNullValues()
        .add("id", id)
        .add("personDay.id", personDay.id)
        .add("way", way)
        .add("date", date)
        .add("stampType", stampType)
        .add("stampModificationType", stampModificationType)
        .toString();

  }

  /**
   * Comparator Stamping.
   */
  public int compareTo(Stamping compareStamping) {
    if (date.isBefore(compareStamping.date)) {
      return -1;
    } else if (date.isAfter(compareStamping.date)) {
      return 1;
    } else {
      return 0;
    }
  }

  /**
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

  /**
   * @return Una rappresentazione compatta della timbratura.
   */
  @Transient
  public String getLabel() {
    String output = formattedHour();
    output += WayType.in.equals(this.way) ? " Ingr." : " Usc.";
    output += stampType != null ? " (" + stampType.getIdentifier() + ")" : "";
    return output;
  }

  /**
   * Fondamentale per far funzionare alcune drools
   * @return Restituisce il proprietario della timbratura
   */
  public Person getOwner() {
    return personDay.person;
  }

  /**
   * Utile per effettuare i controlli temporali sulle drools
   * @return il mese relativo alla data della timbratura
   */
  public YearMonth getYearMonth() {
    return new YearMonth(date.getYear(),date.getMonthOfYear());
  }

  public enum WayType {
    in("in"),
    out("out");

    public String description;

    WayType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return this.description;
    }
  }

}
