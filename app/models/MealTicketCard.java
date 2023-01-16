/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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
import java.time.LocalDate;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import models.base.PeriodModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;

/**
 * Nuova classe che implementa le card dei buoni elettronici.
 *
 * @author dario
 *
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "meal_ticket_card")
public class MealTicketCard extends PeriodModel {
  
  private static final long serialVersionUID = -2102768569995667210L;

  private String number;

  @ManyToOne
  @Required
  private Person person;
  
  @OneToMany(mappedBy = "mealTicketCard")
  private List<MealTicket> mealTickets = Lists.newArrayList();
  
  private boolean isActive;
  
  /**
   * Data di consegna
   */
  private LocalDate deliveryDate;
  
  /**
   * La sede che ha consegnato la tessera
   */
  @ManyToOne
  @Required
  private Office deliveryOffice;
  
  @Override
  public String getLabel() {
    return this.number + "";
  }
}
