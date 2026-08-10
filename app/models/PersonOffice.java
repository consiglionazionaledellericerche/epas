/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import play.data.validation.Required;

/**
 * Rappresenta l'afferenza di una persona ad un ufficio in un periodo di tempo.
 * La relazione tra Person e Office è molti-a-molti storicizzata con date di inizio e fine.
 *
 * @author Dario Tagliaferri
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Audited
@Entity
@Table(name = "person_offices")
public class PersonOffice extends BaseModel {

  private static final long serialVersionUID = 3561478234985612345L;

  @Required
  @ManyToOne
  @NotNull
  private Person person;

  @Required
  @ManyToOne
  @NotNull
  private Office office;

  @NotNull
  @Required
  private LocalDate beginDate;

  private LocalDate endDate;

  @NotAudited
  private LocalDateTime updatedAt;

  @PreUpdate
  @PrePersist
  private void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * Il Range che comprende le date di inizio e fine dell'afferenza.
   */
  public Range<LocalDate> getRange() {
    if (endDate != null) {
      return Range.closed(beginDate, endDate);
    }
    return Range.atLeast(beginDate);
  }

  /**
   * Verifica se la persona afferisce all'ufficio nella data specificata.
   *
   * @param date la data da verificare
   * @return true se l'afferenza è attiva alla data indicata, false altrimenti.
   */
  public boolean contains(LocalDate date) {
    return getRange().contains(date);
  }

  /**
   * Verifica se l'afferenza è attiva nella data corrente.
   *
   * @return true se l'afferenza è attiva oggi, false altrimenti.
   */
  public boolean isActive() {
    return contains(LocalDate.now());
  }
}
