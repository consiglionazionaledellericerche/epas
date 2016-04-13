package controllers;

import com.google.common.base.Optional;

import dao.OfficeDao;
import dao.PersonMonthRecapDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

import lombok.extern.slf4j.Slf4j;

import manager.UploadSituationManager;
import manager.attestati.NuovoAttestatiManager;
import manager.attestati.old.AttestatiClient;

import models.Office;

import org.joda.time.YearMonth;

import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

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
  private static NuovoAttestatiManager nuovoAttestatiManager;

  public static void newAttestati(Long officeId){
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    String token = nuovoAttestatiManager.getToken();
    log.info("Token ricevuto: {}", token);
    IWrapperOffice wrOffice = factory.create(office);
    Optional<YearMonth> monthToUpload = wrOffice.nextYearMonthToUpload();
    render(wrOffice, monthToUpload);
  }
  
  public static void inserisciAssenza(Office office, int month, int year){
    
    int result = nuovoAttestatiManager.inserisciAssenza("ebf93f8c-6247-429e-82c7-582f2d4e713e",
        month, year, new Integer(office.codeId));
  }
  
}
