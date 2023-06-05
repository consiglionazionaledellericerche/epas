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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import models.enumerate.BlockType;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import play.data.validation.Required;
import play.data.validation.Unique;


/**
 * Buoni pasto.
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "meal_ticket", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"code", "office_id"})})
public class MealTicket extends BaseModel {

  private static final long serialVersionUID = -963204680918650598L;

  @Required
  @ManyToOne(optional = false)
  @JoinColumn(name = "contract_id", nullable = false)
  private Contract contract;

  private Integer year;

  @Required
  private LocalDate date;

  @Required
  private String block; /*esempio 5941 3165 01 */
  
  @Enumerated(EnumType.STRING)
  private BlockType blockType;

  private Integer number;

  //@CheckWith(MealTicketInOffice.class)
  @Unique(value = "code, office")
  private String code; /* concatenzazione block + number */

  @Required
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "admin_id", nullable = false)
  private Person admin;

  @Required
  @Column(name = "expire_date")
  private LocalDate expireDate;
  
  private boolean returned = false;
  
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id", nullable = false)
  private Office office;
  
  @ManyToOne
  @JoinColumn(name = "meal_ticket_card_id")
  private MealTicketCard mealTicketCard;

  @Transient
  public Boolean used = null;

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("contract", contract.id)
            .add("code", code)
            .add("person", contract.getPerson().getName() + " " + contract.getPerson().getSurname())
            .add("date", date)
            .add("expire", expireDate).toString();

  }
}
