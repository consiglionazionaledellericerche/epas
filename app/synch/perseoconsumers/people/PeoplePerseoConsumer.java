package synch.perseoconsumers.people;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Maps;

import dao.QualificationDao;

import lombok.extern.slf4j.Slf4j;

import models.Institute;
import models.Office;
import models.Person;
import models.Qualification;

import org.assertj.core.util.Lists;

import play.Play;
import play.libs.WS;
import play.libs.WS.HttpResponse;

import java.util.List;
import java.util.Map;

@Slf4j
public class PeoplePerseoConsumer {

  private final QualificationDao qualificationDao;

  @Inject
  public PeoplePerseoConsumer(QualificationDao qualificationDao) {
    this.qualificationDao = qualificationDao;
  }
  
  private static final String URL_BASE = Play.configuration.getProperty("perseo.base");
 
  private static final String ALL_PEOPLE_FOR_EPAS_ENDPOINT = 
      Play.configuration.getProperty("perseo.rest.allpeopleforepas");
  
  
  /**
   * Perseo Json relativo a tutte le persone (ridotte).
   * @return
   */
  private Optional<String> perseoAllPerseoPeopleJson() {

    String endPoint = URL_BASE + ALL_PEOPLE_FOR_EPAS_ENDPOINT;
    HttpResponse restResponse = WS.url(endPoint).get();
    log.info("Perseo: prelevo la lista di tutti gli istituti presenti da {}.", endPoint);
    
    if (!restResponse.success()) {
      log.error("Impossibile prelevare la lista degli istituti presenti da {}", endPoint);
      return Optional.<String>absent();
    }
    
    try {
      return Optional.fromNullable(restResponse.getJson().toString());
    } catch(Exception e) {
      log.info("Url={} non json.", endPoint);
    }
    
    return null;
  }
  
  
  
  /**
   * La lista dei PerseoOffice da perseo.
   * @return
   */
  private List<PerseoPerson> getAllPerseoSimplePeople() {

    //Json della richiesta
    Optional<String> json = perseoAllPerseoPeopleJson();
    if (json == null || !json.isPresent()) {
      return null;
    }
    
    List<PerseoPerson> perseoSimplePeople = null;
    try {
      perseoSimplePeople = new Gson().fromJson(json.get(), new TypeToken<List<PerseoPerson>>(){}.getType());
    } catch(Exception e) {
      log.info("Impossibile caricare da perseo la lista degli istituti.");
      return Lists.newArrayList();
    }
    
    return perseoSimplePeople;
  }
 
  
  /**
   * Conversione a oggetti epas. PerseoInstitute.
   * @param perseoInstitute
   * @return
   */
  private Person epasConverter(PerseoPerson perseoPerson,  
      Map<Integer, Qualification> qualificationsMap) {
    
    Person person = new Person();
    person.name = perseoPerson.firstname;
    person.surname = perseoPerson.surname;
    person.number = perseoPerson.number;
    //person.email = perseoPerson.email; per adesso le email non combaciano @iit.cnr.it vs @cnr.it
    //person.eppn = perseoPerson.email;
    person.qualification = qualificationsMap.get(perseoPerson.qualification);
    person.perseoId = perseoPerson.id;
    
    return person;
  }
  
  /**
   * Importa tutti le persone da  da perseo come mappa perseoId -> person.
   * @return
   */
  public Map<Long, Person> perseoPeopleByPerseoId() {
    Map<Integer, Qualification> qualificationsMap = qualificationDao.allQualificationMap();
    List<PerseoPerson> perseoPeople = getAllPerseoSimplePeople();
    Map<Long, Person> perseoPeopleMap = Maps.newHashMap();
    for (PerseoPerson perseoPerson : perseoPeople) {
      Person person = epasConverter(perseoPerson, qualificationsMap);
      if (person.number == null) {
        //non dovrebbe succedere...
        log.info("Giunta da Siper persona senza matricola... {}.", person.toString());
      } else {
        perseoPeopleMap.put(person.perseoId, person);
      }
    }
    return perseoPeopleMap;
  }
  
  /**
   * Importa tutti le persone da  da perseo come mappa number -> person.
   * @return
   */
  public Map<Integer, Person> perseoPeopleByNumber() {
    
    Map<Integer, Person> perseoPeopleMap = Maps.newHashMap();
    for (Person person : perseoPeopleByPerseoId().values()) {
      perseoPeopleMap.put(person.number, person);
    }
    return perseoPeopleMap;
  }
   
}
