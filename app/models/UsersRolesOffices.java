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
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;
import play.data.validation.Unique;

/**
 * Associazione di un ruolo per un utente per una determinata sede.
 * IMPORTANTE: relazione con user impostata a LAZY per non scaricare tutte le informazioni della
 * persona durante la valutazione delle drools con target!=null. Avremmo potuto impostare a lazy la
 * successiva relazione fra user e person ma ciò non portava al risultato sperato (probabilmente a
 * causa della natura della relazione fra user e person OneToOne).
 */
@Getter
@Setter
@Entity
@Table(name = "users_roles_offices")
@Audited
public class UsersRolesOffices extends BaseModel {

  private static final long serialVersionUID = -1403683534643592790L;

  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  @Required
  @NotNull
  private User user;

  
  @ManyToOne
  @JoinColumn(name = "office_id")
  @Required
  @NotNull
  @Unique("user office role")
  private Office office;

  
  @ManyToOne
  @JoinColumn(name = "role_id")
  @Required
  @NotNull
  private Role role;

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this).omitNullValues()
        .add("id", id)
        .add("user", user)
        .add("role", role)
        .add("office", office)
        .toString();
  }

}
