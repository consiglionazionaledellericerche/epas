package synch.perseoconsumers.office;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import helpers.rest.ApiRequestException;

import lombok.extern.slf4j.Slf4j;

import models.Institute;
import models.Office;

import play.libs.WS;
import play.libs.WS.HttpResponse;
import synch.perseoconsumers.PerseoApis;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
public class OfficePerseoConsumer {


  /**
   * Preleva la lista di tutte le sedi presenti su Perseo.
   */
  private ListenableFuture<List<PerseoOffice>> perseoOffices() {

    final String url;
    final String user;
    final String pass;

    try {
      url = PerseoApis.getOfficesEndpoint() + "list";
      user = PerseoApis.getPerseoUser();
      pass = PerseoApis.getPerseoPass();
    } catch (NoSuchFieldException ex) {
      final String error = String.format("Parametro necessario non trovato: %s", ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    final WS.WSRequest request = WS.url(url).authenticate(user, pass);

    log.info("Invio richiesta lista sedi a Perseo: {}", request.url);

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(request.getAsync());

    return Futures.transform(future, new Function<HttpResponse, List<PerseoOffice>>() {
      @Override
      public List<PerseoOffice> apply(WS.HttpResponse response) {
        if (!response.success()) {
          final String error = String.format("Errore nella risposta del server di Perseo: %s %s",
              response.getStatus(), response.getStatusText());
          log.warn(error);
          throw new ApiRequestException(error);
        }
        log.info("Recuperato Json contenente le sedi da Perseo: {} {}",
            response.getStatus(), response.getStatusText());
        try {
          return new Gson().fromJson(response.getJson(), new TypeToken<List<PerseoOffice>>() {
            private static final long serialVersionUID = 2774884829320865732L;
          }.getType());
        } catch (JsonSyntaxException ex) {
          final String error = String.format("Errore nel parsing del json: %s", ex.getMessage());
          log.warn(error);
          throw new ApiRequestException(error);
        }
      }
    });
  }

  /**
   * PerseoOffice relativo alla sede con perseoId.
   */
  private ListenableFuture<PerseoOffice> perseoOfficeByPerseoId(Long perseoId) {

    final String url;
    final String user;
    final String pass;

    try {
      url = PerseoApis.getOfficeEndpoint() + perseoId;
      user = PerseoApis.getPerseoUser();
      pass = PerseoApis.getPerseoPass();
    } catch (NoSuchFieldException ex) {
      final String error = String.format("Parametro necessario non trovato: %s", ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    final WS.WSRequest request = WS.url(url).authenticate(user, pass);

    log.info("Invio richiesta Sede a Perseo: {}", request.url);

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(request.getAsync());

    return Futures.transform(future, new Function<HttpResponse, PerseoOffice>() {
      @Override
      public PerseoOffice apply(WS.HttpResponse response) {
        if (!response.success()) {
          final String error = String.format("Errore nella risposta del server di Perseo: %s %s",
              response.getStatus(), response.getStatusText());
          log.warn(error);
          throw new ApiRequestException(error);
        }
        log.info("Recuperato Json contenente la Sede con id {} da Perseo.", perseoId);
        try {
          return new Gson().fromJson(response.getJson(), PerseoOffice.class);
        } catch (JsonSyntaxException ex) {
          final String error = String.format("Errore nel parsing del json: %s", ex.getMessage());
          log.warn(error);
          throw new ApiRequestException(error);
        }
      }
    });
  }

  /**
   * PerseoInstitute relativo all'istituto con perseoId.
   */
  private ListenableFuture<PerseoInstitute> perseoInstituteByPerseoId(Long perseoId) {

    final String url;
    final String user;
    final String pass;

    try {
      url = PerseoApis.getInstituteEndpoint() + perseoId;
      user = PerseoApis.getPerseoUser();
      pass = PerseoApis.getPerseoPass();
    } catch (NoSuchFieldException ex) {
      final String error = String.format("Parametro necessario non trovato: %s", ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    final WS.WSRequest request = WS.url(url).authenticate(user, pass);

    log.info("Invio richiesta Istituto a Perseo: {}", request.url);

    ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
        .listenInPoolThread(request.getAsync());

    return Futures.transform(future, new Function<HttpResponse, PerseoInstitute>() {
      @Override
      public PerseoInstitute apply(WS.HttpResponse response) {
        if (!response.success()) {
          final String error = String.format("Errore nella risposta del server di Perseo: %s %s",
              response.getStatus(), response.getStatusText());
          log.warn(error);
          throw new ApiRequestException(error);
        }
        log.info("Recuperato Json contenente l'istituto con id {} da Perseo", perseoId);
        try {
          return new Gson().fromJson(response.getJson(), PerseoInstitute.class);
        } catch (JsonSyntaxException ex) {
          final String error = String.format("Errore nel parsing del json: %s", ex.getMessage());
          log.warn(error);
          throw new ApiRequestException(error);
        }
      }
    });
  }

  /**
   * Conversione a oggetti epas. PerseoInstitute.
   */
  private Institute epasConverter(PerseoInstitute perseoInstitute) {

    Institute institute = new Institute();
    institute.perseoId = new Long(perseoInstitute.id);
    institute.cds = perseoInstitute.cds;
    institute.name = perseoInstitute.name;
    institute.code = perseoInstitute.code;
    return institute;
  }

  /**
   * Conversione a oggetti epas.
   */
  private Map<Integer, Institute> epasConverter(List<PerseoOffice> perseoOffices) {
    Map<Integer, Institute> institutesMap = Maps.newHashMap();
    for (PerseoOffice perseoOffice : perseoOffices) {
      Institute institute;
      if (institutesMap.get(perseoOffice.institute.id) == null) {
        institute = epasConverter(perseoOffice.institute);
        institutesMap.put(perseoOffice.institute.id, institute);
      } else {
        institute = institutesMap.get(perseoOffice.institute.id);
      }

      Office office = new Office();
      office.perseoId = new Long(perseoOffice.id);
      office.codeId = perseoOffice.codeId;
      office.code = perseoOffice.code;
      office.name = perseoOffice.shortName;
      office.address = perseoOffice.street;
      office.institute = institute;
      institute.seats.add(office);
    }
    return institutesMap;
  }

  /**
   * Importa tutti gli istutiti da perseo come mappa perseoId -> istituto.
   */
  public Map<Integer, Institute> perseoInstitutesByPerseoId() {
    List<PerseoOffice> perseoOffices = Lists.newArrayList();

    try {
      perseoOffices = perseoOffices().get();
    } catch (InterruptedException | ExecutionException ex) {
      String error = String.format("Impossibile recuperare la lista degli istituti - %s",
          ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    return epasConverter(perseoOffices);
  }

  /**
   * Importa tutti gli istituti da perseo come lista.
   */
  public List<Institute> perseoInstitutes() {

    Map<Integer, Institute> institutesMap = perseoInstitutesByPerseoId();
    return Lists.newArrayList(institutesMap.values());
  }

  /**
   * Importa tutti gli istutiti da perseo come mappa cds -> istituto.
   */
  public Map<String, Institute> perseoInstitutesByCds() {
    Map<String, Institute> institutesMap = Maps.newHashMap();
    for (Institute institute : perseoInstitutes()) {
      institutesMap.put(institute.cds, institute);
    }
    return institutesMap;
  }

  /**
   * Importa istituto e sede della sede con officePerseoId. Absent se almeno uno dei due non è
   * disponibile.
   */
  public Optional<Institute> perseoInstituteByOfficePerseoId(Long officePerseoId) {

    PerseoOffice perseoOffice = null;

    try {
      perseoOffice = perseoOfficeByPerseoId(officePerseoId).get();
    } catch (InterruptedException | ExecutionException ex) {
      String error = String.format("Impossibile recuperare la sede da %d Perseo - %s",
          officePerseoId, ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    if (perseoOffice == null) {
      return Optional.<Institute>absent();
    }

    Optional<Institute> institute = Optional.fromNullable(epasConverter(
        Lists.newArrayList(perseoOffice)).values().iterator().next());

    if (!institute.isPresent() || institute.get().seats.isEmpty()) {
      return Optional.<Institute>absent();
    }
    return institute;
  }

  /**
   * Importa l'istituto institutePerseoId. Absent se non è disponibile.
   */
  public Optional<Institute> perseoInstituteByInstitutePerseoId(Long institutePerseoId) {

    PerseoInstitute perseoInstitute = null;

    try {
      perseoInstitute = perseoInstituteByPerseoId(institutePerseoId).get();
    } catch (InterruptedException | ExecutionException ex) {
      String error = String.format("Impossibile l'istituto da %d Perseo - %s",
          institutePerseoId, ex.getMessage());
      log.error(error);
      throw new ApiRequestException(error);
    }

    if (perseoInstitute == null) {
      return Optional.<Institute>absent();
    }
    return Optional.fromNullable(epasConverter(perseoInstitute));
  }

}
