package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

import manager.attestati.service.CertificationService;
import manager.attestati.service.PersonCertificationStatus;

import models.Certification;
import models.Office;
import models.Person;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;

import javax.inject.Inject;

/**
 * Il controller per l'invio dei dati certificati al nuovo attestati.
 * @author alessandro
 *
 */
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
    
    Optional<YearMonth> monthToUpload = factory.create(office).nextYearMonthToUpload();
    Verify.verify(monthToUpload.isPresent());
    
    if (year != null && month != null) {
      monthToUpload = Optional.fromNullable(new YearMonth(year, month));
    }
    
    LocalDate monthBegin = new LocalDate(monthToUpload.get().getYear(), monthToUpload.get().getMonthOfYear(), 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    year = monthToUpload.get().getYear();
    month = monthToUpload.get().getMonthOfYear();
    
    @SuppressWarnings("deprecation")
    List<Person> people = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();
    
    List<PersonCertificationStatus> peopleCertificationStatus = Lists.newArrayList();
    
    Optional<String> token = certificationService.buildToken();
    
    for (Person person : people) {
      
      // Costruisco lo status generale
      PersonCertificationStatus personCertificationStatus = certificationService
          .buildPersonStaticStatus(person, year, month, token);
      
      peopleCertificationStatus.add(personCertificationStatus);
    }
    
    render(office, year, month, peopleCertificationStatus);
  }
  
  
  public static void processAll(Long officeId, Integer year, Integer month) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    
    Optional<YearMonth> monthToUpload = factory.create(office).nextYearMonthToUpload();
    Verify.verify(monthToUpload.isPresent());
    
    if (year != null && month != null) {
      monthToUpload = Optional.fromNullable(new YearMonth(year, month));
    }

    LocalDate monthBegin = new LocalDate(monthToUpload.get().getYear(), monthToUpload.get().getMonthOfYear(), 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    year = monthToUpload.get().getYear();
    month = monthToUpload.get().getMonthOfYear();

    @SuppressWarnings("deprecation")
    List<Person> people = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();

    List<PersonCertificationStatus> peopleCertificationStatus = Lists.newArrayList();

    Optional<String> token = certificationService.buildToken();

    for (Person person : people) {

      // Costruisco lo status generale
      PersonCertificationStatus personCertificationStatus = certificationService
          .buildPersonStaticStatus(person, year, month, token);

      // Applico il process
      certificationService.process(personCertificationStatus, token);

      peopleCertificationStatus.add(personCertificationStatus);

    }
    
    renderTemplate("@certifications", office, year, month, peopleCertificationStatus);
  }
  
  
}
