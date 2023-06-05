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
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;

/**
 * Ruolo all'interno di ePAS.
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "roles")
public class Role extends BaseModel {

  private static final long serialVersionUID = 6717202212924325368L;

  public static final String SEAT_SUPERVISOR = "seatSupervisor";
  public static final String PERSONNEL_ADMIN = "personnelAdmin";
  public static final String PERSONNEL_ADMIN_MINI = "personnelAdminMini";
  public static final String EMPLOYEE = "employee";
  public static final String BADGE_READER = "badgeReader";
  public static final String REST_CLIENT = "restClient";
  public static final String TECHNICAL_ADMIN = "technicalAdmin";
  public static final String SHIFT_MANAGER = "shiftManager";
  public static final String REPERIBILITY_MANAGER = "reperibilityManager";
  public static final String GROUP_MANAGER = "groupManager";
  public static final String MEAL_TICKET_MANAGER = "mealTicketManager";
  public static final String REGISTRY_MANAGER = "registryManager";
  public static final String PERSON_DAY_READER = "personDayReader";
  public static final String ABSENCE_MANAGER = "absenceManager";
  public static final String BADGE_MANAGER = "badgeManager";  

  private String name;

  
  @OneToMany(mappedBy = "role", cascade = CascadeType.REMOVE, orphanRemoval = true)
  private List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();

  @Override
  public String toString() {
    if (name.equals(SEAT_SUPERVISOR)) {
      return "Responsabile Sede";
    }
    if (name.equals(PERSONNEL_ADMIN)) {
      return "Amministratore Personale";
    }
    if (name.equals(PERSONNEL_ADMIN_MINI)) {
      return "Amministratore Personale Sola lettura";
    }
    if (name.equals(TECHNICAL_ADMIN)) {
      return "Amministratore Tecnico";
    }
    if (name.equals(EMPLOYEE)) {
      return "Dipendente";
    }
    if (name.equals(REPERIBILITY_MANAGER)) {
      return "Gestore reperibilità";
    }
    if (name.equals(SHIFT_MANAGER)) {
      return "Gestore turni";
    }
    if (name.equals(BADGE_READER)) {
      return "Lettore di badge";
    }
    if (name.equals(REST_CLIENT)) {
      return "Client rest";
    }
    if (name.equals(GROUP_MANAGER)) {
      return "Responsabile gruppo";
    }
    if (name.equals(MEAL_TICKET_MANAGER)) {
      return "Gestore buoni pasto";
    }
    if (name.equals(REGISTRY_MANAGER)) {
      return "Gestore anagrafica";
    }
    if (name.equals(PERSON_DAY_READER)) {
      return "Lettore informazioni";
    }
    if (name.equals(ABSENCE_MANAGER)) {
      return "Gestore assenze";
    }
    if (name.equals(BADGE_MANAGER)) {
      return "Gestore badge";
    }
    return name;
  }

}
