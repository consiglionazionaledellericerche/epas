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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.data.validation.Required;
import play.data.validation.Unique;


/**
 * Tipo di reperibilità.
 *
 * @author Cristian Lucchesi
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "person_reperibility_types")
public class PersonReperibilityType extends BaseModel {

  private static final long serialVersionUID = 3234688199593333012L;

  @Required
  @Unique
  private String description;

  @OneToMany(mappedBy = "personReperibilityType")
  private List<PersonReperibility> personReperibilities;

  /* responsabile della reperibilità */
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @Required
  private Person supervisor;
  
  private boolean disabled;
  
  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @NotNull
  private Office office; 
 
  @OneToMany(mappedBy = "personReperibilityType", cascade = CascadeType.REMOVE)
  private Set<ReperibilityTypeMonth> monthsStatus = new HashSet<>();
  
  @ManyToMany
  private List<Person> managers = Lists.newArrayList();
  
  /*Tipo di competenza mensile*/
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotNull
  private MonthlyCompetenceType monthlyCompetenceType;

  @Override
  public String toString() {
    return this.description;
  }
  
  /**
   * Ritorna l'oggetto che contiene l'approvazione della reperibilità alla data.
   *
   * @param date la data da considerare
   * @return l'oggetto che contiene l'approvazione della reperibilità se esistente.
   */
  @Transient
  public Optional<ReperibilityTypeMonth> monthStatusByDate(LocalDate date) {
    final YearMonth requestedMonth = new YearMonth(date);
    return monthsStatus.stream()
        .filter(reperibilityTypeMonth -> reperibilityTypeMonth
            .getYearMonth().equals(requestedMonth)).findFirst();
  }

  /**
   * Controlla se la reperibilità è stata approvata alla data passata come parametro.
   *
   * @param date la data da verificare
   * @return true se la reperibilità è stata approvata alla data date, false altrimenti.
   */
  @Transient
  public boolean approvedOn(LocalDate date) {
    Optional<ReperibilityTypeMonth> monthStatus = monthStatusByDate(date);
    if (monthStatus.isPresent()) {
      return monthStatus.get().isApproved();
    } else {
      return false;
    }
  }
}
