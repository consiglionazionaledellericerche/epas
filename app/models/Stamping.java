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

import com.google.common.base.MoreObjects;
import helpers.validators.StringIsValid;
import it.cnr.iit.epas.NullStringBinder;
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
import models.base.BaseModel;
import models.enumerate.StampTypes;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;
import play.data.binding.As;
import play.data.validation.CheckWith;
import play.data.validation.Required;


/**
 * Modello della Timbratura.
 *
 * @author Cristian Lucchesi
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

  @Required @NotNull
  @Column(nullable = false)
  public LocalDateTime date;

  @Required @NotNull
  @Enumerated(EnumType.STRING)
  public WayType way;

  @As(binder = NullStringBinder.class)
  public String note;
  
  @As(binder = NullStringBinder.class)
  @CheckWith(StringIsValid.class)
  public String place;
  
  @As(binder = NullStringBinder.class)
  @CheckWith(StringIsValid.class)
  public String reason;

  /**
   * questo campo booleano consente di determinare se la timbratura è stata effettuata dall'utente
   * all'apposita macchinetta (valore = false) o se è stato l'amministratore a settare l'orario di
   * timbratura poichè la persona in questione non ha potuto effettuare la timbratura (valore =
   * true).
   */
  @Column(name = "marked_by_admin")
  public boolean markedByAdmin;

  /**
   * con la nuova interpretazione delle possibilità del dipendente, questo campo viene settato a
   * true quando è il dipendente a modificare la propria timbratura.
   */
  @Column(name = "marked_by_employee")
  public boolean markedByEmployee;
  
  /**
   * con la nuova interpretazione del telelavoro per i livelli I-III, quando un dipendente si 
   * inserisce una timbratura in telelavoro, questa deve essere inserita anche sul suo cartellino, 
   * diventando a tutti gli effetti una timbratura che concorre alla generazione del residuo 
   * giornaliero.
   */
  @Column(name = "marked_by_telework")
  public boolean markedByTelework;

  /**
   * questo nuovo campo si è reso necessario per la sede centrale per capire da quale lettore 
   * proviene la timbratura così da poter applicare un algoritmo che giustifichi le timbrature 
   * di uscita/ingresso consecutive dei dipendenti se provenienti da lettori diversi e appartenenti 
   * a un collegamento definito.e all'interno della tolleranza definita per quel collegamento.
   */
  @Column(name = "stamping_zone")
  public String stampingZone;

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
  public boolean exitingNow;

  @Transient
  public boolean isValid() {
    return valid;
  }

  @Transient
  public boolean isIn() {
    return way == WayType.in;
  }

  @Transient
  public boolean isOut() {
    return way == WayType.out;
  }
  

  /**
   * Verifica se è lavoro fuori sede.
   *
   * @return @see StampTypes::isOffSiteWork
   */
  @Transient
  public boolean isOffSiteWork() {
    return stampType != null && stampType.isOffSiteWork();
  }
  
  /**
   * costruttore di default implicitamente utilizzato dal play(controllers).
   */
  Stamping() {
  }

  /**
   * Costruttore.
   *
   * @param personDay personDay
   * @param time      time
   */
  public Stamping(PersonDay personDay, LocalDateTime time) {
    // FIXME se necessito di una stamping senza personDay (ex. per uscita in questo momento)
    // questo costruttore mi impedisce di costruirla. Per adesso permetto di passare personDay null.
    date = time;
    if (personDay != null) {
      this.personDay = personDay;
      personDay.stampings.add(this);
    }
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
  @Override
  public int compareTo(final Stamping compareStamping) {
    return date.compareTo(compareStamping.date);
  }

  /**
   * Orario formattato come HH:mm.
   *
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
   * Rappresentazione compatta della timbratura.
   *
   * @return Una rappresentazione compatta della timbratura.
   */
  @Transient
  public String getLabel() {
    String output = formattedHour();
    output += way == WayType.in ? " Ingr." : " Usc.";
    output += stampType != null ? " (" + stampType.getIdentifier() + ")" : "";
    return output;
  }

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
   * Ingresso/Uscita.
   */
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
