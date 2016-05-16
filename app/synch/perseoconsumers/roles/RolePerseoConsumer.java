package synch.perseoconsumers.roles;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;

import dao.OfficeDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperPerson;

import helpers.rest.ApiRequestException;

import lombok.extern.slf4j.Slf4j;

import models.Office;
import models.Person;
import models.Role;
import models.UsersRolesOffices;

import org.assertj.core.util.Lists;

import play.libs.WS;
import play.libs.WS.HttpResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import synch.perseoconsumers.PerseoApis;

@Slf4j
public class RolePerseoConsumer {

  private final PersonDao personDao;
  private final OfficeDao officeDao;
  private final RoleDao roleDao;
  private final UsersRolesOfficesDao uroDao;

  /**
   * Costruttore.
   * @param personDao inject
   * @param wrapperFunctionFactory inject
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
   * @return La lista dei ruoli assegnati su perseo alle sedi.
   */
  private ListenableFuture<List<PerseoRole>> perseoRoles() {

    final String url;
    final String user;
    final String pass;

    try {
      url = PerseoApis.getAllRolesEpasEndpoint();
      user = PerseoApis.getPerseoUser();
      pass = PerseoApis.getPerseoPass();
    } catch (NoSuchFieldException e) {
      final String error = String.format("Parametro necessario non trovato: %s", e.getMessage());
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
          }.getType());
        } catch (JsonSyntaxException e) {
          final String error = String.format("Errore nel parsing del json: %s", e.getMessage());
          log.warn(error);
          throw new ApiRequestException(error);
        }
      }
    });
  }

  /**
   * Serve per sincronizzare i ruoli epas.
   *
   * @param perseoDepartmentId department perseo id
   * @param office             ?
   * @return mappa
   */
  public Map<Long, Set<String>> perseoRoles(Optional<Office> office) {

    List<PerseoRole> perseoRoles = Lists.newArrayList();

    try {
      perseoRoles = perseoRoles().get();
    } catch (InterruptedException | ExecutionException e) {
      String error = String
          .format("Impossibile recuperare i ruoli da Perseo - %s", e.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }


    //Mappa per la ricerca degli office by perseoId
    Map<Long, Office> mapOffices = Maps.newHashMap();
    if (office.isPresent()) {
      mapOffices.put(office.get().perseoId, office.get());
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
        personRoles.add(role.toString() + " - " + officeRole.name);
        peoplePerseoRoles.put(perseoRole.personPerseoId, personRoles);
      } else {
        personRoles.add(role.toString() + " - " + officeRole.name);
      }
    }
    
    return peoplePerseoRoles;
  }
  
  
  /**
   * La lista dei ruoli in perseo per l'office, sotto forma di usersRolesOffices
   * @param office
   * @return la lista
   */
  public List<UsersRolesOffices> perseoUsersRolesOffices(Office office) {
    
    // Ruoli sede di perseo
    List<PerseoRole> perseoRoles = Lists.newArrayList();
    try {
      perseoRoles = perseoRoles().get();
    } catch (InterruptedException | ExecutionException e) {
      String error = String
          .format("Impossibile recuperare i ruoli da Perseo - %s", e.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }
   
    //Persone epas per perseoId.
    Map<Long, Person> epasPeople = Maps.newHashMap();
    for (Person person : personDao.list(Optional.of(office)).list()) {
      if (person.perseoId != null) {
        epasPeople.put(person.perseoId, person);
      }
    }
    
    // Sedi per perseoId
    Map<Long, Office> epasOffices = Maps.newHashMap();
    for (Office seat: officeDao.allOffices().list()) {
      if (seat.perseoId != null) {
        epasOffices.put(seat.perseoId, seat);
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
      Role role  = roleFromPerseo(perseoRole.roleName);
      if (role == null) {
        continue;
      }
      Optional<UsersRolesOffices> uroOpt = uroDao
          .getUsersRolesOffices(epasPerson.user, role, epasOffice); 
      if(uroOpt.isPresent()) {
        uroList.add(uroOpt.get());
      } else {
        UsersRolesOffices uro = new UsersRolesOffices();
        uro.user = epasPerson.user;
        uro.office = epasOffice;
        uro.role = role;
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
      return roleDao.getRoleByName(Role.TECNICAL_ADMIN);
    }
    return null;
  }
}
