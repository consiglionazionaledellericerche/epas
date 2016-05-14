package synch.perseoconsumers.roles;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;

import helpers.rest.ApiRequestException;

import lombok.extern.slf4j.Slf4j;

import models.Office;

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

  /**
   * Costruttore.
   * @param personDao inject
   * @param wrapperFunctionFactory inject
   */
  @Inject
  public RolePerseoConsumer() {
    
  }

 

  /**
   * @return La lista dei ruoli assegnati su perseo alle sedi.
   */
  private ListenableFuture<List<PerseoRole>> perseoContracts() {

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
      perseoRoles = perseoContracts().get();
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
      String roleName = null;
      if (perseoRole.roleName.equals("Responsabile Presenze")) {
        roleName = "Amministratore Personale";
      } else if (perseoRole.roleName.equals("Responsabile Presenze sola lettura")) {
        roleName = "Amministratore Personale Sola lettura";
      } else if (perseoRole.roleName.equals("Admin Sede")) {
        roleName = "Amministratore Tecnico";
      }
      if (roleName == null) {
        // Non dovrebbero arrivare ruoli non epas....
        continue;
      }
      
      Set<String> personRoles = peoplePerseoRoles.get(perseoRole.personPerseoId);
      if (personRoles == null) {
        personRoles = Sets.newHashSet();
        personRoles.add(roleName + " - " + officeRole.name);
        peoplePerseoRoles.put(perseoRole.personPerseoId, personRoles);
      } else {
        personRoles.add(roleName + " - " + officeRole.name);
      }
    }
    
    return peoplePerseoRoles;
  }
}
