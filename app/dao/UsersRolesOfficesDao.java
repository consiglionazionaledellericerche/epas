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

package dao;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.query.QBadgeReader;
import models.query.QRole;
import models.query.QUser;
import models.query.QUsersRolesOffices;
import org.testng.collections.Maps;
import org.testng.collections.Sets;

/**
 * DAO per UsersRolesOffices.
 */
public class UsersRolesOfficesDao extends DaoBase {

  @Inject
  UsersRolesOfficesDao(JPQLQueryFactory queryFactory,
      Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Preleva lo UsersRolesOffices per id.
   */
  public UsersRolesOffices getById(Long id) {
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    return getQueryFactory().selectFrom(uro).where(uro.id.eq(id)).fetchOne();
  }

  /**
   * La lista degli utenti che hanno un determinato permesso su un'ufficio.
   *
   * @param role il ruolo da verificare
   * @param office l'ufficio su cui avere il ruolo.
   * @return la lista degli user che hanno il ruolo specificato nell'ufficio indicato.
   */
  public List<User> getUsersWithRoleOnOffice(Role role, Office office) {
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    return getQueryFactory().select(uro.user).from(uro)
        .where(uro.role.eq(role)
            .and(uro.office.eq(office))).fetch();
  }

  /**
   * L'userRoleOffice associato ai parametri passati.
   *
   * @param user l'utente
   * @param role il ruolo
   * @param office la sede
   * @return l'usersRolesOffice associato ai parametri passati.
   */
  public Optional<UsersRolesOffices> getUsersRolesOffices(User user, Role role, Office office) {
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final UsersRolesOffices result = getQueryFactory().selectFrom(uro)
        .where(uro.user.eq(user)
            .and(uro.role.eq(role)
                .and(uro.office.eq(office)))).fetchOne();

    return Optional.fromNullable(result);
  }

  /**
   * La lista di tutti gli userRoleOffice legati all'utente passato.
   *
   * @param user l'utente
   * @return la lista di tutti gli usersRolesOffices associati al parametro passato.
   */
  public List<UsersRolesOffices> getUsersRolesOfficesByUser(User user) {
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    return getQueryFactory().selectFrom(uro).where(uro.user.eq(user)).fetch();
  }

  /**
   * La lista di tutti gli userRoleOffice legati all'utente passato.
   *
   * @param user l'utente
   * @return la lista di tutti gli usersRolesOffices associati al parametro passato.
   */
  public List<UsersRolesOffices> getAdministrativeUsersRolesOfficesByUser(User user) {
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    return getQueryFactory().selectFrom(uro)
        .where(uro.user.eq(user), uro.role.name.eq(Role.ABSENCE_MANAGER)
        .or(uro.role.name.eq(Role.GROUP_MANAGER))
        .or(uro.role.name.eq(Role.PERSONNEL_ADMIN))
        .or(uro.role.name.eq(Role.PERSONNEL_ADMIN_MINI))
        .or(uro.role.name.eq(Role.SEAT_SUPERVISOR))).fetch();
  }

  
  /**
   * Metodo per effettuare check dello stato ruoli epas <-> perseo.
   */
  public Map<Long, Set<String>> getEpasRoles(Optional<Office> office) {

    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final QUser user = QUser.user;

    ImmutableList<String> rolesName = ImmutableList.of(
        Role.PERSONNEL_ADMIN, Role.PERSONNEL_ADMIN_MINI, Role.TECHNICAL_ADMIN);

    final QRole role = QRole.role;
    JPQLQuery<Role> roles = JPAExpressions.selectFrom(role).where(role.name.in(rolesName));

    List<UsersRolesOffices> uroList = getQueryFactory().selectFrom(uro)
        .leftJoin(uro.user, user).fetchJoin()
        .leftJoin(user.badgeReaders, QBadgeReader.badgeReader).fetchJoin()
        .leftJoin(uro.role, role)
        .where(uro.role.in(roles)).fetch();

    Map<Long, Set<String>> urosMap = Maps.newHashMap();

    for (UsersRolesOffices uroItem : uroList) {
      if (uroItem.getUser().getPerson() == null 
          || uroItem.getUser().getPerson().getPerseoId() == null) {
        continue;
      }
      if (office.isPresent() && !office.get()
          .equals(uroItem.getUser().getPerson().getCurrentOffice().get())) {
        continue;
      }
      Set<String> personUros = urosMap.get(uroItem.getUser().getPerson().getPerseoId());
      if (personUros == null) {
        personUros = Sets.newHashSet();
        personUros.add(formatUro(uroItem));
        urosMap.put(uroItem.getUser().getPerson().getPerseoId(), personUros);
      } else {
        personUros.add(formatUro(uroItem));
      }
    }

    return urosMap;

  }

  /**
   * Il conteggio di quanti sono gli utenti con ruolo role già presenti nel db.
   *
   * @param role il ruolo da ricercare negli Uro
   * @return quanti sono gli utenti con ruolo role già inseriti nel db.
   */
  public long countSupervisors(Role role) {
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    return getQueryFactory().selectFrom(uro).where(uro.role.eq(role)).fetchCount();
  }

  /**
   * Formatta come stringa le info sullo UsersRolesOffices.
   */
  public String formatUro(UsersRolesOffices uro) {
    return uro.getRole().toString() + " - " + uro.getOffice().getName();
  }

}