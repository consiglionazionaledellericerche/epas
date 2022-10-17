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

import it.cnr.iit.epas.NullStringBinder;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import play.data.binding.As;
import play.data.validation.Required;

/**
 * Sono stati implementati i metodi Equals e HashCode in modo che Se sono presenti più badge per la
 * persona che differiscono solo per il campo badgeReader venga restituito un solo elemento
 * (effettivamente per noi è lo stesso badge) Quindi person.badges non restituisce i duplicati.
 */
@Getter
@Setter
@Entity
@Audited
@EqualsAndHashCode(exclude = { "badgeReader" }, callSuper = true)
@Table(name = "badges", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"badge_reader_id", "code"})})
public class Badge extends BaseModel {

  private static final long serialVersionUID = -4397151725225276985L;

  @Required
  @As(binder = NullStringBinder.class)
  @NotNull
  private String code;

  @ManyToOne
  private Person person;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "badge_reader_id")
  private BadgeReader badgeReader;

  @ManyToOne
  @JoinColumn(name = "badge_system_id")
  private BadgeSystem badgeSystem;

  /**
   * Assegna code come numero del badge.
   *
   * @param code il numero del badge
   */
  public void setCode(String code) {
    try {
      this.code = String.valueOf(Integer.valueOf(code));
    } catch (NumberFormatException ignored) {
      this.code = code;
    }
  }
}
