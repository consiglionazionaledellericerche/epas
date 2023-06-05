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

package synch.perseoconsumers.roles;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dao.OfficeDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import helpers.rest.ApiRequestException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import synch.perseoconsumers.AnagraficaApis;

/**
 * Preleva da Perseo le informazioni relative ai ruoli dei dipendenti.
 */
@Slf4j
public class RolePerseoConsumer {

  private final PersonDao personDao;
  private final OfficeDao officeDao;
  private final RoleDao roleDao;
  private final UsersRolesOfficesDao uroDao;

  /**
   * Costruttore.
   *
   * @param personDao injected PersonDao
   * @param officeDao injected OfficeDao
   * @param roleDao   injected RoleDao
   * @param uroDao    injected UsersRolesOfficesDao
   */
  @Inject
  public RolePerseoConsumer(PersonDao personDao, OfficeDao officeDao, RoleDao roleDao,
      UsersRolesOfficesDao uroDao) {
    this.personDao = personDao;
    this.officeDao = officeDao;
    this.roleDao = roleDao;
    this.uroDao = uroDao;
  }


  /**
   * a lista dei ruoli assegnati su perseo alle sedi.
   */
  private ListenableFuture<List<PerseoRole>> perseoRoles() {

    final String url;
    final String user;
    final String pass;

    try {
      url = AnagraficaApis.getAllRolesEpasEndpoint();
      user = AnagraficaApis.getPerseoUser();
      pass = AnagraficaApis.getPerseoPass();
    } catch (NoSuchFieldException ex) {
      final String error = String.format("Parametro necessario non trovato: %s", ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    final WS.WSRequest request = WS.url(url).authenticate(user, pass);

    log.info("Invio richiesta ruoli a Perseo: {}", request.url);

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(request.getAsync());

    return Futures.transform(future, new Function<HttpResponse, List<PerseoRole>>() {
      @Override
      public List<PerseoRole> apply(WS.HttpResponse response) {
        if (!response.success()) {
          final String error = String.format("Errore nella risposta del server di Perseo: %s %s",
              response.getStatus(), response.getStatusText());
          log.warn(error);
          throw new ApiRequestException(error);
        }
        log.info("Recuperato Json contenente i ruoli da Perseo: {} {}",
            response.getStatus(), response.getStatusText());
        try {
          return new Gson().fromJson(response.getJson(), new TypeToken<List<PerseoRole>>() {
            private static final long serialVersionUID = -2548702453839391511L;
          }.getType());
        } catch (JsonSyntaxException ex) {
          final String error = String.format("Errore nel parsing del json: %s", ex.getMessage());
          log.warn(error);
          throw new ApiRequestException(error);
        }
      }
    }, MoreExecutors.directExecutor());
  }

  /**
   * Serve per sincronizzare i ruoli epas.
   *
   * @param office ?
   * @return mappa
   */
  public Map<Long, Set<String>> perseoRoles(Optional<Office> office) {

    List<PerseoRole> perseoRoles = Lists.newArrayList();

    try {
      perseoRoles = perseoRoles().get();
    } catch (InterruptedException | ExecutionException ex) {
      String error = String
          .format("Impossibile recuperare i ruoli da Perseo - %s", ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }


    //Mappa per la ricerca degli office by perseoId
    Map<Long, Office> mapOffices = Maps.newHashMap();
    if (office.isPresent()) {
      mapOffices.put(office.get().getPerseoId(), office.get());
    } else {
      // TODO: richiesta per tutte le sedi, mettercele tutte
    }

    Map<Long, Set<String>> peoplePerseoRoles = Maps.newHashMap();

    for (PerseoRole perseoRole : perseoRoles) {
      Office officeRole = mapOffices.get(perseoRole.perseoDepartmentId);
      if (officeRole == null) {
        continue;
      }
      Role role = roleFromPerseo(perseoRole.roleName);
      if (role == null) {
        // Non dovrebbero arrivare ruoli non epas....
        continue;
      }

      Set<String> personRoles = peoplePerseoRoles.get(perseoRole.personPerseoId);
      if (personRoles == null) {
        personRoles = Sets.newHashSet();
        personRoles.add(role.toString() + " - " + officeRole.getName());
        peoplePerseoRoles.put(perseoRole.personPerseoId, personRoles);
      } else {
        personRoles.add(role.toString() + " - " + officeRole.getName());
      }
    }

    return peoplePerseoRoles;
  }


  /**
   * La lista dei ruoli in perseo per l'office, sotto forma di usersRolesOffices.
   *
   * @return la lista
   */
  public List<UsersRolesOffices> perseoUsersRolesOffices(Office office) {

    // Ruoli sede di perseo
    List<PerseoRole> perseoRoles = Lists.newArrayList();
    try {
      perseoRoles = perseoRoles().get();
    } catch (InterruptedException | ExecutionException ex) {
      String error = String
          .format("Impossibile recuperare i ruoli da Perseo - %s", ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    //Persone epas per perseoId.
    Map<Long, Person> epasPeople = Maps.newHashMap();
    for (Person person : personDao.list(Optional.of(office)).list()) {
      if (person.getPerseoId() != null) {
        epasPeople.put(person.getPerseoId(), person);
      }
    }

    // Sedi per perseoId
    Map<Long, Office> epasOffices = Maps.newHashMap();
    for (Office seat : officeDao.allOffices().list()) {
      if (seat.getPerseoId() != null) {
        epasOffices.put(seat.getPerseoId(), seat);
      }
    }

    List<UsersRolesOffices> uroList = Lists.newArrayList();

    for (PerseoRole perseoRole : perseoRoles) {
      Person epasPerson = epasPeople.get(perseoRole.personPerseoId);
      if (epasPerson == null) {
        continue;
      }
      Office epasOffice = epasOffices.get(perseoRole.perseoDepartmentId);
      if (epasOffice == null) {
        continue;
      }
      Role role = roleFromPerseo(perseoRole.roleName);
      if (role == null) {
        continue;
      }
      Optional<UsersRolesOffices> uroOpt = uroDao
          .getUsersRolesOffices(epasPerson.getUser(), role, epasOffice);
      if (uroOpt.isPresent()) {
        uroList.add(uroOpt.get());
      } else {
        UsersRolesOffices uro = new UsersRolesOffices();
        uro.setUser(epasPerson.getUser());
        uro.setOffice(epasOffice);
        uro.setRole(role);
        uroList.add(uro);
      }
    }
    return uroList;
  }

  private Role roleFromPerseo(String perseoName) {

    if (perseoName.equals("Responsabile Presenze")) {
      return roleDao.getRoleByName(Role.PERSONNEL_ADMIN);
    } else if (perseoName.equals("Responsabile Presenze sola lettura")) {
      return roleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);
    } else if (perseoName.equals("Admin Sede")) {
      return roleDao.getRoleByName(Role.TECHNICAL_ADMIN);
    }
    return null;
  }
}
