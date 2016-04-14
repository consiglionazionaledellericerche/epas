package synch.perseoconsumers.office;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Maps;

import lombok.extern.slf4j.Slf4j;

import models.Institute;
import models.Office;

import org.assertj.core.util.Lists;

import play.Play;
import play.libs.WS;
import play.libs.WS.HttpResponse;

import java.util.List;
import java.util.Map;

@Slf4j
public class OfficePerseoConsumer {

  private static final String PERSEO_BASE_URL = "perseo.base";
  private static final String OFFICES_ENDPOINT =
      Play.configuration.getProperty("perseo.rest.departments");
  private static final String OFFICE_ENDPOINT =
      Play.configuration.getProperty("perseo.rest.departmentbyperseoid");
  private static final String INSTITUTE_ENDPOINT =
      Play.configuration.getProperty("perseo.rest.institutebyperseoid");

  @Inject
  public OfficePerseoConsumer() {
  }

  private static String getPerseoBaseUrl() {
    return Play.configuration.getProperty(PERSEO_BASE_URL);
  }

  /**
   * Perseo Json relativo agli istituti.
   */
  private Optional<String> perseoOfficesJson() {

    String endPoint = getPerseoBaseUrl() + OFFICES_ENDPOINT + "list";
    HttpResponse restResponse = WS.url(endPoint).get();
    log.info("Perseo: prelevo la lista di tutti gli istituti presenti da {}.", endPoint);

    if (!restResponse.success()) {
      log.error("Impossibile prelevare la lista degli istituti presenti da {}", endPoint);
      return Optional.<String>absent();
    }

    try {
      return Optional.fromNullable(restResponse.getJson().toString());
    } catch (Exception e) {
      log.info("Url={} non json.", endPoint);
    }

    return null;
  }

  /**
   * Perseo Json relativo alla sede con perseoId.
   */
  private Optional<String> perseoOfficeByPerseoIdJson(Long perseoId) {

    String endPoint = getPerseoBaseUrl() + OFFICE_ENDPOINT + perseoId;
    HttpResponse restResponse = WS.url(endPoint).get();
    log.info("Perseo: prelevo la sede da perseo da {}.", endPoint);

    if (!restResponse.success()) {
      log.error("Impossibile prelevare la sede da perseo da {}", endPoint);
      return Optional.<String>absent();
    }

    try {
      return Optional.fromNullable(restResponse.getJson().toString());
    } catch (Exception e) {
      log.info("Url={} non json.", endPoint);
    }

    return null;
  }

  /**
   * Perseo Json relativo all istituto con perseoId.
   */
  private Optional<String> perseoInstituteByPerseoIdJson(Long perseoId) {

    String endPoint = getPerseoBaseUrl() + INSTITUTE_ENDPOINT + perseoId;
    HttpResponse restResponse = WS.url(endPoint).get();
    log.info("Perseo: prelevo la sede da perseo da {}.", endPoint);

    if (!restResponse.success()) {
      log.error("Impossibile prelevare la sede da perseo da {}", endPoint);
      return Optional.<String>absent();
    }

    try {
      return Optional.fromNullable(restResponse.getJson().toString());
    } catch (Exception e) {
      log.info("Url={} non json.", endPoint);
    }

    return null;
  }

  /**
   * La lista dei PerseoOffice da perseo.
   */
  private List<PerseoOffice> getPerseoOffices() {

    //Json della richiesta
    Optional<String> json = perseoOfficesJson();
    if (json == null || !json.isPresent()) {
      return null;
    }

    List<PerseoOffice> perseoOffices = null;
    try {
      perseoOffices = new Gson().fromJson(json.get(), new TypeToken<List<PerseoOffice>>() {
      }.getType());
    } catch (Exception e) {
      log.info("Impossibile caricare da perseo la lista degli istituti.");
      return Lists.newArrayList();
    }

    return perseoOffices;

  }

  /**
   * Il perseoOffice con perseoId
   */
  private Optional<PerseoOffice> getPerseoOfficeByPerseoId(Long perseoId) {

    //Json della richiesta
    Optional<String> json = perseoOfficeByPerseoIdJson(perseoId);
    if (json == null || !json.isPresent()) {
      return null;
    }

    PerseoOffice perseoOffice = null;
    try {
      perseoOffice = new Gson().fromJson(json.get(), new TypeToken<PerseoOffice>() {
      }.getType());
    } catch (Exception e) {
      log.info("Impossibile caricare da perseo la sede con perseoId={}.", perseoId);
      return Optional.<PerseoOffice>absent();
    }
    if (perseoOffice == null) {
      return Optional.<PerseoOffice>absent();
    }
    return Optional.fromNullable(perseoOffice);

  }

  /**
   * Il perseoOffice con perseoId
   */
  private Optional<PerseoInstitute> getPerseoInstituteByPerseoId(Long perseoId) {

    //Json della richiesta
    Optional<String> json = perseoInstituteByPerseoIdJson(perseoId);
    if (json == null || !json.isPresent()) {
      return null;
    }

    PerseoInstitute perseoInstitute = null;
    try {
      perseoInstitute = new Gson().fromJson(json.get(), new TypeToken<PerseoInstitute>() {
      }.getType());
    } catch (Exception e) {
      log.info("Impossibile caricare da perseo la sede con perseoId={}.", perseoId);
      return Optional.<PerseoInstitute>absent();
    }
    if (perseoInstitute == null) {
      return Optional.<PerseoInstitute>absent();
    }
    return Optional.fromNullable(perseoInstitute);

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
      office.name = institute.code + " - " + perseoOffice.city;
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
    List<PerseoOffice> perseoOffices = getPerseoOffices();
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
    Optional<PerseoOffice> perseoOffice = getPerseoOfficeByPerseoId(officePerseoId);
    if (!perseoOffice.isPresent()) {
      return Optional.<Institute>absent();
    }
    Optional<Institute> institute = Optional.fromNullable(epasConverter(
        Lists.newArrayList(perseoOffice.get())).values().iterator().next());

    if (!institute.isPresent() || institute.get().seats.isEmpty()) {
      return Optional.<Institute>absent();
    }
    return institute;
  }

  /**
   * Importa l'istituto institutePerseoId. Absent se non è disponibile.
   */
  public Optional<Institute> perseoInstituteByInstitutePerseoId(Long institutePerseoId) {
    Optional<PerseoInstitute> perseoInstitute = getPerseoInstituteByPerseoId(institutePerseoId);
    if (!perseoInstitute.isPresent()) {
      return Optional.<Institute>absent();
    }
    return Optional.fromNullable(epasConverter(perseoInstitute.get()));
  }

}
