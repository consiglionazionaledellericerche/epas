package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import controllers.RequestInit.CurrentData;

import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

import lombok.extern.slf4j.Slf4j;

import manager.ConfigurationManager;
import manager.attestati.service.CertificationService;
import manager.attestati.service.PersonCertificationStatus;

import models.Office;
import models.Person;
import models.enumerate.EpasParam;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Il controller per l'invio dei dati certificati al nuovo attestati.
 * @author alessandro
 *
 */
@Slf4j
@With({Resecure.class, RequestInit.class})
public class Certifications extends Controller {

  @Inject
  private static SecurityRules rules;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static IWrapperFactory factory;
  @Inject
  private static PersonDao personDao;
  @Inject 
  private static ConfigurationManager configurationManager;

  @Inject 
  private static CertificationService certificationService;
  
  public static void newAttestati(Long officeId){
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    
    Optional<String> token = certificationService.buildToken();
    if (!token.isPresent()) {
      flash.error("Impossibile autenticarsi a attestati.");
      UploadSituation.uploadData(officeId);
    }
    
    IWrapperOffice wrOffice = factory.create(office);
    Optional<YearMonth> monthToUpload = wrOffice.nextYearMonthToUpload();
    
    render(wrOffice, monthToUpload, token);
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
    
    //Nuovo attestati?
    if (!(Boolean)configurationManager.configValue(office, EpasParam.NEW_ATTESTATI)) {
      forbidden();
    }
  
    //Mese selezionato
    Optional<YearMonth> monthToUpload = factory.create(office).nextYearMonthToUpload();
    Verify.verify(monthToUpload.isPresent());
    
    if (year != null && month != null) {
      monthToUpload = Optional.fromNullable(new YearMonth(year, month));
    }
    
    LocalDate monthBegin = new LocalDate(monthToUpload.get().getYear(), monthToUpload.get().getMonthOfYear(), 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    year = monthToUpload.get().getYear();
    month = monthToUpload.get().getMonthOfYear();
    
    // Patch per la navigazione del menù ... ####################################
    // Al primo accesso (da menù) dove non ho mese e anno devo prendere il default
    // (NextMonthToUpload). In quel caso aggiorno la sessione nel cookie. Dovrebbe
    // occuparsene la RequestInit.
    session.put("monthSelected", monthToUpload.get().getMonthOfYear());
    session.put("yearSelected", monthToUpload.get().getYear());
    renderArgs.put("currentData", new CurrentData(monthToUpload.get().getYear(), 
        monthToUpload.get().getMonthOfYear(), 
        Integer.parseInt(session.get("daySelected")), 
        Long.parseLong(session.get("personSelected")), 
        office.id));
    // ##########################################################################
    
    //Il mese selezionato è abilitato?
    Optional<String> token = certificationService.buildToken();
    boolean autenticate = certificationService.authentication(office, token, true);
    if (!autenticate) {
      flash.error("L'utente app.epas non è abilitato alla sede selezionata");
      render(office, year, month, autenticate);
    }
    
    //Lo stralcio è stato effettuato?
    Set<Integer> numbers = certificationService.peopleList(office, year, month, token);
    if (numbers.isEmpty()) {
      flash.error("E' necessario effettuare lo stralcio dei dati per processare "
          + "gli attestati (sede %s, anno %s, mese %s).", office.name, year, month);
      render(office, year, month, autenticate, numbers);
    }
    
    @SuppressWarnings("deprecation")
    List<Person> people = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();
    
    List<PersonCertificationStatus> peopleCertificationStatus = Lists.newArrayList();
    boolean peopleNotInAttestati = false;
    for (Person person : people) {
      
      // Costruisco lo status generale
      PersonCertificationStatus personCertificationStatus = certificationService
          .buildPersonStaticStatus(person, year, month, numbers, token);

      if (personCertificationStatus.match()) {
        // La matricola la rimuovo da quelle in attestati (alla fine rimangono quelle non trovate)
        numbers.remove(person.number);        
      }
      
      if (personCertificationStatus.notInAttestati) {
        peopleNotInAttestati = true;
      }
      
      peopleCertificationStatus.add(personCertificationStatus);
    }
    
    render(office, year, month, peopleCertificationStatus, numbers, peopleNotInAttestati);
  }
  
  
  public static void processAll(Long officeId, Integer year, Integer month) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    
    //Nuovo attestati
    if (!(Boolean)configurationManager.configValue(office, EpasParam.NEW_ATTESTATI)) {
      forbidden();
    }

    LocalDate monthBegin = new LocalDate(year, month, 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();

    //Il mese selezionato è abilitato?
    Optional<String> token = certificationService.buildToken();
    boolean autenticate = certificationService.authentication(office, token, true);
    if (!autenticate) {
      flash.error("L'utente app.epas non è abilitato alla sede selezionata");
      renderTemplate("@certifications", office, year, month, autenticate);
    }
    
    //Lo stralcio è stato effettuato?
    Set<Integer> numbers = certificationService.peopleList(office, year, month, token);
    if (numbers.isEmpty()) {
      flash.error("E' necessario effettuare lo stralcio dei dati per processare "
          + "gli attestati (sede %s, anno %s, mese %s).", office.name, year, month);
      renderTemplate("@certifications", office, year, month, autenticate, numbers);
    }
    
    @SuppressWarnings("deprecation")
    List<Person> people = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();

    List<PersonCertificationStatus> peopleCertificationStatus = Lists.newArrayList();

    for (Person person : people) {
      
      // Costruisco lo status generale
      PersonCertificationStatus personCertificationStatus = certificationService
          .buildPersonStaticStatus(person, year, month, numbers, token);
      
      if (personCertificationStatus.match()) {
        // Applico il process
        certificationService.process(personCertificationStatus, token);
        // La matricola la rimuovo da quelle in attestati (alla fine rimangono quelle non trovate)
        numbers.remove(person.number);
      }
      
      peopleCertificationStatus.add(personCertificationStatus);

    }
    
    renderTemplate("@certifications", office, year, month, numbers, peopleCertificationStatus);
  }
  
  
}
