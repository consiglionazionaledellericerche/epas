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

package manager;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.stream.Collectors;
import models.Office;
import models.Role;
import models.User;

/**
 * Manager per la gestione della sicurezza su uffici e persone.
 *
 */
public class SecureManager {

  /**
   * La lista degli uffici permessi per l'utente user passato.
   *
   * @param user l'utente 
   * @param rolesNames la lista dei ruoli
   * @return la lista degli uffici permessi per l'utente user passato come parametro.
   */
  private Set<Office> getOfficeAllowed(User user, ImmutableList<String> rolesNames) {

    Preconditions.checkNotNull(user);
    Preconditions.checkState(user.isPersistent());

    // Utente con ruoli di sistema
    if (!user.getRoles().isEmpty()) {
      return Sets.newHashSet(Office.findAll());
    }

    return user.getUsersRolesOffices().stream().filter(uro -> 
    rolesNames.contains(uro.getRole().getName()))
        .map(uro -> uro.getOffice()).distinct().collect(Collectors.toSet());
  }

  /**
   * Le sedi per le quali l'utente dispone di almeno il ruolo di sola lettura.
   */
  public Set<Office> officesReadAllowed(User user) {

    ImmutableList<String> rolesNames = ImmutableList.of(
        Role.PERSONNEL_ADMIN,
        Role.PERSONNEL_ADMIN_MINI,
        Role.SEAT_SUPERVISOR);

    return getOfficeAllowed(user, rolesNames);
  }

  /**
   * Le sedi per le quali l'utente dispone del ruolo di scrittura.
   */
  public Set<Office> officesWriteAllowed(User user) {

    ImmutableList<String> rolesNames = ImmutableList.of(Role.PERSONNEL_ADMIN);

    return getOfficeAllowed(user, rolesNames);
  }

  /**
   * Le sedi per le quali l'utente dispone del ruolo di badge reader.
   */
  public Set<Office> officesBadgeReaderAllowed(User user) {

    ImmutableList<String> roles = ImmutableList.of(Role.BADGE_READER);

    return getOfficeAllowed(user, roles);
  }

  /**
   * Le sedi per le quali l'utente dispone del ruolo di amministratore di sistema.
   */
  public Set<Office> officesSystemAdminAllowed(User user) {

    ImmutableList<String> roles = ImmutableList.of(Role.PERSONNEL_ADMIN);

    return getOfficeAllowed(user, roles);

  }

  /**
   * L'insieme degli uffici su cui user è Amm. tecnico.
   *
   * @param user l'utente per cui si cercano gli uffici su cui è Amm. Tecnico
   * @return l'insieme degli uffici su cui user è Amm. tecnico.
   */
  public Set<Office> officesTechnicalAdminAllowed(User user) {
    ImmutableList<String> roles = ImmutableList.of(Role.TECHNICAL_ADMIN);

    return getOfficeAllowed(user, roles);
  }

  /**
   * Metodo usato per individuare sedi e persone amministrate da visualizzare nella navbar.
   *
   * @param user l'utente
   * @return set
   */
  public Set<Office> officesForNavbar(User user) {

    ImmutableList<String> roles = ImmutableList.of(
        Role.PERSONNEL_ADMIN, Role.PERSONNEL_ADMIN_MINI, Role.TECHNICAL_ADMIN,
        Role.SEAT_SUPERVISOR, Role.MEAL_TICKET_MANAGER);

    return getOfficeAllowed(user, roles);
  }
}