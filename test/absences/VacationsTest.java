package absences;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;

import db.h2support.H2Examples;
import db.h2support.base.H2AbsenceSupport;

import injection.StaticInject;

import manager.services.absences.AbsenceService;
import manager.services.absences.model.VacationSituation;

import models.Contract;
import models.Person;
import models.absences.AbsenceType.DefaultAbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.DefaultGroup;

import org.joda.time.LocalDate;
import org.junit.Test;

import play.test.UnitTest;


@StaticInject
public class VacationsTest extends UnitTest {
  
  public static final LocalDate EXPIRE_DATE_LAST_YEAR = new LocalDate(2016, 8, 31);
  public static final LocalDate EXPIRE_DATE_CURRENT_YEAR = new LocalDate(2017, 8, 31);
  
  @Inject 
  private static H2Examples h2Examples;
  @Inject 
  private static H2AbsenceSupport h2AbsenceSupport;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
    
  @Test
  public void vacationsTestBase() {
    
    absenceService.enumInitializator();

    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();

    final LocalDate today = new LocalDate(2016, 9, 1);
    
    //un tempo determinato
    Person person = h2Examples.normalEmployee(new LocalDate(2009, 2, 01), Optional.absent());
    
    h2AbsenceSupport.absence(DefaultAbsenceType.A_31, 
        new LocalDate(2016, 1, 1), Optional.absent(), 0, person);
    h2AbsenceSupport.absence(DefaultAbsenceType.A_37, 
        new LocalDate(2016, 9, 1), Optional.absent(), 0, person);
    h2AbsenceSupport.absence(DefaultAbsenceType.A_32, 
        new LocalDate(2016, 9, 10), Optional.absent(), 0, person);
    h2AbsenceSupport.absence(DefaultAbsenceType.A_94, 
        new LocalDate(2016, 9, 11), Optional.absent(), 0, person);

    VacationSituation vacationSituation = absenceService.buildVacationSituation(
        person.contracts.get(0), 2016, vacationGroup, Optional.of(today), false, null);

    assertTrue(vacationSituation.lastYear.expired());
    assertEquals(vacationSituation.lastYear.total(), 28);
    assertEquals(vacationSituation.lastYear.used(), 2);
    assertEquals(vacationSituation.lastYear.usableTotal(), 26);
    assertEquals(vacationSituation.lastYear.usable(), 0);
    
    assertFalse(vacationSituation.currentYear.expired());
    assertEquals(vacationSituation.currentYear.total(), 28);
    assertEquals(vacationSituation.currentYear.used(), 1);
    assertEquals(vacationSituation.currentYear.usableTotal(), 27);
    assertEquals(vacationSituation.currentYear.usable(), 27);
    
    assertFalse(vacationSituation.permissions.expired());
    assertEquals(vacationSituation.permissions.total(), 4);
    assertEquals(vacationSituation.permissions.used(), 1);
    assertEquals(vacationSituation.permissions.usableTotal(), 3);
    assertEquals(vacationSituation.permissions.usable(), 3);
    
    //un tempo determinato
    Person person2 = h2Examples.normalEmployee(new LocalDate(2011, 10, 1), 
        Optional.of(new LocalDate(2016, 10, 1)));
    
    h2AbsenceSupport.absence(DefaultAbsenceType.A_31, 
        new LocalDate(2016, 1, 1), Optional.absent(), 0, person2);
    h2AbsenceSupport.absence(DefaultAbsenceType.A_37, 
        new LocalDate(2016, 9, 1), Optional.absent(), 0, person2);
    h2AbsenceSupport.absence(DefaultAbsenceType.A_32, 
        new LocalDate(2016, 9, 10), Optional.absent(), 0, person2);
    h2AbsenceSupport.absence(DefaultAbsenceType.A_94, 
        new LocalDate(2016, 9, 11), Optional.absent(), 0, person2);
    
    VacationSituation vacationSituation2 = absenceService.buildVacationSituation(
        person2.contracts.get(0), 2016, vacationGroup, Optional.of(today), false, null);
    
    assertTrue(vacationSituation2.lastYear.expired());
    assertEquals(vacationSituation2.lastYear.total(), 28);
    assertEquals(vacationSituation2.lastYear.used(), 2);
    assertEquals(vacationSituation2.lastYear.usableTotal(), 26);
    assertEquals(vacationSituation2.lastYear.usable(), 0);
    
    assertFalse(vacationSituation2.currentYear.expired());
    assertEquals(vacationSituation2.currentYear.total(), 21);
    assertEquals(vacationSituation2.currentYear.used(), 1);
    assertEquals(vacationSituation2.currentYear.usableTotal(), 20);
    assertEquals(vacationSituation2.currentYear.usable(), 17);
    
    assertFalse(vacationSituation2.permissions.expired());
    assertEquals(vacationSituation2.permissions.total(), 3);
    assertEquals(vacationSituation2.permissions.used(), 1);
    assertEquals(vacationSituation2.permissions.usableTotal(), 2);
    assertEquals(vacationSituation2.permissions.usable(), 2);
  }
  
  /**
   * Cambiando piano ferie nel corso dell'anno 2015 per quell'anno disponeva di soli 25 giorni.
   * Quindi gliene diamo uno in più che viene maturata immediatamente.
   * Il test costruisce il recap al primo giorno del 2015 e dimostra che in quel momento taverniti
   * ha un giorno di ferie immediatamente maturato, e 26 totali (subito prendibili perchè è un tempo
   * indeterminato).
   * Piano Ferie 26+4
   * Intervallo  Dal 01/01/2015 al 29/10/2015
   * Giorni considerati  302
   * Giorni calcolati    21
   * Piano Ferie 28+4
   * Intervallo  Dal 30/10/2015 al 31/12/2015
   * Giorni considerati  63
   * Giorni calcolati    4
   */
  @Test
  public void theCuriousCaseOfMariaTaverniti() {

    absenceService.enumInitializator();

    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();

    //un tempo determinato
    Person person = h2Examples.normalEmployee(new LocalDate(2012, 10, 30), Optional.absent());

    final LocalDate today = new LocalDate(2015, 1, 1); //recap date

    VacationSituation vacationSituation = absenceService.buildVacationSituation(
        person.contracts.get(0), 2015, vacationGroup, Optional.of(today), false, null);

    assertEquals(vacationSituation.currentYear.total(), 26);
    assertEquals(vacationSituation.currentYear.accrued(), 1);
    assertEquals(vacationSituation.currentYear.usableTotal(), 26);

  }
  
  /**
   * Quando il cambio di piano durante l'anno porta ad avere un numero superiore di ferie
   * rispetto al valore massimo fra i piani ferie.
   * Si adotta l'aggiustamento.
   */
  @Test
  public void tooLucky() {

    absenceService.enumInitializator();

    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();

    //un tempo determinato
    Person person = h2Examples.normalEmployee(new LocalDate(2013, 4, 17), Optional.absent());

    final LocalDate today = new LocalDate(2016, 1, 1); //recap date

    VacationSituation vacationSituation = absenceService.buildVacationSituation(
        person.contracts.get(0), 2016, vacationGroup, Optional.of(today), false, null);

    assertEquals(vacationSituation.currentYear.total(), 28);
    assertEquals(vacationSituation.currentYear.accrued(), 0);
    assertEquals(vacationSituation.currentYear.usableTotal(), 28);

  }
  
  /**
   * Il source contract va utilizzato in modo appropriato nel caso in cui si debba costruire 
   * il riepilogo per l'anno successivo l'inizializzazione.
   * Esempio con data inizializzazione nel 2016 
   * A) Se voglio costruire il riepilogo dell'anno 2016 il bind è automatico. 
   * ferie da inizializzazione 2015 = contract.getSourceVacationLastYearUsed()      
   * ferie da inizializzazione 2016 = contract.getSourceVacationCurrentYearUsed() 
   * permessi da inizializzazione 2016 = contact.getSourcePermissionCurrentYearUsed()
   * B) Se voglio costruire il riepilogo dell'anno 2017 il bind corretto è 
   * ferie da inizializzazione 2016 = contract.getSourceVacationCurrentYearUsed()
   * (gli altri campi sono inutili)                  
   */
  @Test
  public void initializationShouldWorksTheNextYear() {

    //Esempio Pinna IMM - Lecce
    
    absenceService.enumInitializator();

    //un tempo determinato
    Person person = h2Examples.normalEmployee(new LocalDate(2001, 1, 16), Optional.absent());
    Contract contract = person.contracts.get(0);
    contract.sourceDateResidual = new LocalDate(2016, 10, 31);
    contract.sourceVacationLastYearUsed = 28;
    contract.sourceVacationCurrentYearUsed = 5;
    contract.sourcePermissionUsed = 4;
    
    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    
    VacationSituation vacationSituation = absenceService.buildVacationSituation(
        contract, 2016, vacationGroup, Optional.of(new LocalDate(2016, 1, 1)), false, null);

    assertEquals(vacationSituation.currentYear.usable(), 23);

    VacationSituation vacationSituation2 = absenceService.buildVacationSituation(
        contract, 2017, vacationGroup, Optional.of(new LocalDate(2017, 1, 1)), false, null);
    
    assertEquals(vacationSituation2.lastYear.usable(), 23);
    
  }
  
}
