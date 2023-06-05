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

import com.google.common.collect.Range;
import java.util.Comparator;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.data.validation.Required;


/**
 * Contiene le informazioni per l'eventuale "reperibilità" svolta dalla persona.
 *
 * @author Cristian Lucchesi
 */
@Getter
@Setter
@ToString
@Audited
@Entity
@Table(name = "person_reperibility")
public class PersonReperibility extends BaseModel {

  private static final long serialVersionUID = 7543768807724174894L;

  //@Unique
  @ManyToOne
  @Required
  @JoinColumn(name = "person_id")
  private Person person;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Required
  @ManyToOne
  @JoinColumn(name = "person_reperibility_type_id")
  private PersonReperibilityType personReperibilityType;

  @OneToMany(mappedBy = "personReperibility", cascade = {CascadeType.REMOVE})
  private List<PersonReperibilityDay> personReperibilityDays;


  private String note;

  /**
   * il range di date di validità dell'appartenenza di una persona al servizio di reperibilità.
   *
   * @return il range di date di validità dell'appartenenza di una persona al servizio di 
   *     reperibilità.
   */
  @Transient
  public Range<LocalDate> dateRange() {
    if (startDate == null && endDate == null) {
      return Range.all();
    }
    if (startDate == null) {
      return Range.atMost(endDate);
    }
    if (endDate == null) {
      return Range.atLeast(startDate);
    }
    return Range.closed(startDate, endDate);
  }

  public static Comparator<PersonReperibility> PersonReperibilityComparator = 
      new Comparator<PersonReperibility>() {

          public int compare(PersonReperibility pr1, PersonReperibility pr2) {
            String prName1 = pr1.personReperibilityType.getDescription().toUpperCase();
            String prName2 = pr2.personReperibilityType.getDescription().toUpperCase();
            return prName1.compareTo(prName2);
          }
      };

  /**
   * Verifica se il dipendente è attivo in reperibilità in una certa data.
   *
   * @param date la data in cui verificare se la reperibilità era attiva
   *     per questa persona.
   * @return true se la reperibilità era attiva nella data passata, false altrimenti. 
   */
  @Transient
  public boolean isActive(LocalDate date) {
    return (startDate == null || !startDate.isAfter(date)) 
        && endDate == null || !endDate.isBefore(date);
  }

  /**
   * Verifica se il dipendente è attivo in reperibilità nell'anno/mese.
   *
   * @param yearMonth l'anno/mese in cui verificare se la reperibilità era attiva
   *     per questa persona almeno un giorno.
   * @return true se la reperibilità era attiva nella data passata, false altrimenti. 
   */
  @Transient
  public boolean isActive(YearMonth yearMonth) {
    return (startDate == null 
        || !startDate.isAfter(yearMonth.toLocalDate(1).dayOfMonth().withMaximumValue())) 
        && endDate == null || !endDate.isBefore(yearMonth.toLocalDate(1));
  }
}
