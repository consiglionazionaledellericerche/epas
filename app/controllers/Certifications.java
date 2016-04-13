package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

import lombok.extern.slf4j.Slf4j;

import manager.PersonDayManager;
import manager.attestati.dto.SeatCertification;
import manager.attestati.dto.TokenDTO;

import models.Absence;
import models.Certification;
import models.Competence;
import models.Office;
import models.Person;
import models.PersonMonthRecap;
import models.enumerate.CertificationType;

import org.assertj.core.util.Maps;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Il controller per l'invio dei dati certificati al nuovo attestati.
 * @author alessandro
 *
 */
@Slf4j
@With({Resecure.class, RequestInit.class})
public class Certifications extends Controller {
  
  //Attestati api
  private final static String BASE_URL = "http://as2dock.si.cnr.it";
  private final static String ATTESTATO_URL = "/api/ext/attestato";
  private final static String API_URL = "/api/ext";
  private final static String JSON_CONTENT_TYPE = "application/json";
  
  //OAuh
  private final static String OAUTH_CLIENT_SECRET = "mySecretOAuthSecret";
  private final static String OAUTH_CONTENT_TYPE = "application/x-www-form-urlencoded";
  private final static String OAUTH_URL = "/oauth/token";
  private final static String OAUTH_AUTHORIZATION = "YXR0ZXN0YXRpYXBwOm15U2VjcmV0T0F1dGhTZWNyZXQ=";
  private final static String OAUTH_USERNAME = "app.epas";
  private final static String OAUTH_PASSWORD = "trapocolapuoicambiare";
  private final static String OAUTH_GRANT_TYPE = "password";
  private final static String OAUTH_CLIENT_ID= "attestatiapp";
  
  //Test
  private final static int NUMBER = 9891;
  private final static int SEAT = 603240;
  
  @Inject
  private static SecurityRules rules;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static IWrapperFactory factory;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static PersonMonthRecapDao personMonthRecapDao;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static PersonDayManager personDayManager;
  @Inject
  private static CompetenceDao competenceDao;
  @Inject
  private static AbsenceDao absenceDao;
  
  public static void newAttestati(Long officeId){
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    
    Optional<String> token = getToken();
    if (!token.isPresent()) {
      flash.error("Impossibile autenticarsi a attestati.");
      UploadSituation.uploadData(officeId);
    }
    
    IWrapperOffice wrOffice = factory.create(office);
    Optional<YearMonth> monthToUpload = wrOffice.nextYearMonthToUpload();
    
    render(wrOffice, monthToUpload, token);
  }
   
  /**
   * Per l'ottenenere il Bearer Token:
   * curl -s -X POST -H "Content-Type: application/x-www-form-urlencoded" -H 
   * "Authorization: Basic YXR0ZXN0YXRpYXBwOm15U2VjcmV0T0F1dGhTZWNyZXQ="  -d 'username=app.epas&password=.............
   * &grant_type=password&scope=read%20write&client_secret=mySecretOAuthSecret&client_id=attestatiapp' 
   * "http://as2dock.si.cnr.it/oauth/token"
   * @return
   */
  private static Optional<String> getToken(){

    try {
      
      String body = String.format("username=%s&password=%s&grant_type=%s&client_secret=%s&client_id=%s", 
          OAUTH_USERNAME, OAUTH_PASSWORD, OAUTH_GRANT_TYPE, OAUTH_CLIENT_SECRET, OAUTH_CLIENT_ID);
      
      WSRequest req = WS.url(BASE_URL + OAUTH_URL)
          .setHeader("Content-Type", OAUTH_CONTENT_TYPE)
          .setHeader("Authorization", "Basic "+ OAUTH_AUTHORIZATION)
          .body(body);
      HttpResponse response = req.post();
      Gson gson = new Gson();
      TokenDTO token = gson.fromJson(response.getJson(), TokenDTO.class);

      return Optional.fromNullable(token.access_token);
    } catch(Exception e) {
      return Optional.<String>absent();
    }
  }
  
  /**
   * curl -X GET -H "Authorization: Bearer cf24c413-9cf7-485d-a10b-87776e5659c7" 
   * -H "Content-Type: application/json" 
   * http://as2dock.si.cnr.it/api/ext/attestato/{{CODICESEDE}}/{{MATRICOLA}}/{{ANNO}}/{{MESE}}
   * @param office
   * @param month
   * @param year
   * @param token
   */
  public static void personCertificated(Office office, int month, int year, String token){
    
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    
    String url = ATTESTATO_URL + "/" + SEAT + "/" + NUMBER + "/" + year + "/" + month;
    
    WSRequest wsRequest = prepareOAuthRequest(token, url, JSON_CONTENT_TYPE);
    HttpResponse httpResponse = wsRequest.get();
    
    SeatCertification seatCertification = 
        new Gson().fromJson(httpResponse.getJson(), SeatCertification.class);
    
    renderText(seatCertification.toString());
    
  }
  
  
  /**
   * Costruisce una WSRequest predisposta alla comunicazione con le api attestati.
   * @param token
   * @param url
   * @param contentType
   * @return
   */
  private static WSRequest prepareOAuthRequest(String token, String url, String contentType) {
    WSRequest wsRequest = WS.url( BASE_URL + url)
        .setHeader("Content-Type", contentType)
        .setHeader("Authorization", "Bearer "+ token);
    return wsRequest;
  }
  
  /**
   * Pagina principale nuovo invio attestati.
   * @param officeId
   * @param year
   * @param month
   */
  public static void certifications(Long officeId, Integer year, Integer month) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    
    Optional<YearMonth> monthToUpload = factory.create(office).nextYearMonthToUpload();
    Verify.verify(monthToUpload.isPresent());
    
    if (year != null && month != null) {
      monthToUpload = Optional.fromNullable(new YearMonth(year, month));
    }
    
    @SuppressWarnings("deprecation")
    List<Person> people = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, new LocalDate(year, month, 1),
        new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(), true).list();
    
    Map<Person, List<Certification>> personCertifications = Maps.newHashMap();
    
    //TODO: costruire la certificated situatione delle persone.
    for (Person person : people) {

      List<Certification> certifications = Lists.newArrayList();
      
      certifications.addAll(trainingHours(person, year, month));
      certifications.addAll(absences(person, year, month));
      certifications.addAll(competences(person, year, month));
      certifications.add(mealTicket(person, year, month));
        
      personCertifications.put(person, certifications);

    }
    
    render(office, people);
  }
  
  /**
   * Produce le certification delle ore di formazione per la persona.
   * @param person
   * @param year
   * @param month
   * @return
   */
  private static List<Certification> trainingHours(Person person, int year, int month) {
 
    List<Certification> certifications = Lists.newArrayList();
    
    List<PersonMonthRecap> trainingHoursList = personMonthRecapDao
        .getPersonMonthRecapInYearOrWithMoreDetails(person, year, 
            Optional.fromNullable(month), Optional.<Boolean>absent());
    for (PersonMonthRecap personMonthRecap : trainingHoursList) {
      
      Certification certification = new Certification();
      certification.person = person;
      certification.year = year;
      certification.month = month;
      certification.certificationType = CertificationType.FORMATION;
      // TODO: serializer e deserializer
      certification.content = personMonthRecap.fromDate + ";" + 
          personMonthRecap.toDate + ";" + personMonthRecap.trainingHours;
      
      // TODO: il content va cercato fra quelle già presenti se ci sono...
      
      certifications.add(certification);
    }
    
    return certifications;
  }
  
  /**
   * Produce le certification delle assenze per la persona.
   * @param person
   * @param year
   * @param month
   * @return
   */
  private static List<Certification> absences(Person person, int year, int month) {
    
    List<Absence> absences = absenceDao
        .getAbsencesNotInternalUseInMonth(person, year, month);
    
    List<Certification> certifications = Lists.newArrayList();

    Certification certification = null;
    LocalDate previousDate = null;
    String previousAbsenceCode = null;
    Integer dayBegin = null;
    Integer dayEnd = null;

    for (Absence absence : absences) {
      
      //codici a uso interno li salto
      if (absence.absenceType.internalUse) {
        continue;
      }
      
      //codice per attestati
      String absenceCodeToSend = absence.absenceType.code.toUpperCase();
      if (absence.absenceType.certificateCode != null 
          && !absence.absenceType.certificateCode.trim().isEmpty()) { 
        absenceCodeToSend = absence.absenceType.certificateCode.toUpperCase();
      }

      // Nuovo Item  
      if (previousDate == null) {
        dayBegin =  absence.personDay.date.getDayOfMonth();
        dayEnd = absence.personDay.date.getDayOfMonth();
        previousDate = absence.personDay.date;
        previousAbsenceCode = absenceCodeToSend;
        
        certification = new Certification();
        certification.person = person;
        certification.year = year;
        certification.month = month;
        certification.certificationType = CertificationType.ABSENCE;
        // TODO: serializer e deserializer
        certification.content = absenceCodeToSend + ";" + dayBegin + ";" + dayEnd;
        continue;
      }

      //Assenza più giorni
      if (previousDate.plusDays(1).equals(absence.personDay.date)
          && previousAbsenceCode.equals(absenceCodeToSend)) {
        dayEnd = absence.personDay.date.getDayOfMonth();
        certification.content = absenceCodeToSend + ";" + dayBegin + ";" + dayEnd;
      } else {
        //Inserimento Item
        // TODO: il content va cercato fra quelle già presenti se ci sono...
        certifications.add(certification);
        certification = null;
        previousDate = null;
      }
      previousDate = absence.personDay.date;
    }
    
    //Ultimo elemento
    if (certification != null) {
      // TODO: il content va cercato fra quelle già presenti se ci sono...
      certifications.add(certification);
    }
    
   

    return certifications;
  }
  
  private static List<Certification> competences(Person person, int year, int month) {

    List<Certification> certifications = Lists.newArrayList();
    
    List<Competence> competences = competenceDao
        .getCompetenceInMonthForUploadSituation(person, year, month);
    
    for (Competence competence : competences) {
      Certification certification = new Certification();
      certification.person = person;
      certification.year = year;
      certification.month = month;
      certification.certificationType = CertificationType.COMPETENCE;
      // TODO: serializer e deserializer
      certification.content = competence.competenceCode.code + ";" + competence.valueApproved;
      
      // TODO: il content va cercato fra quelle già presenti se ci sono...
      certifications.add(certification);
    }
    
    return certifications;
  }
  
  /**
   * Produce la certificazione buoni pasto della persona.
   * @param person
   * @param year
   * @param month
   * @return
   */
  private static Certification mealTicket(Person person, int year, int month) {
    
    Integer mealTicket = personDayManager.numberOfMealTicketToUse(personDayDao
        .getPersonDayInMonth(person, new YearMonth(year, month)));
    
    Certification certification = new Certification();
    certification.person = person;
    certification.year = year;
    certification.month = month;
    certification.certificationType = CertificationType.MEAL;
    
    // TODO: serializer e deserializer
    certification.content = year + ";" + month + ";" + mealTicket;
    
    // TODO: il content va cercato fra quelle già presenti se ci sono...
    
    return certification;
  }
  
}
