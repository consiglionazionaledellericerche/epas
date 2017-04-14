package absences;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import injection.StaticInject;

import manager.services.absences.AbsenceService;
import manager.services.absences.model.VacationSituation;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType.DefaultAbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.DefaultGroup;

import org.joda.time.LocalDate;
import org.junit.Test;

import play.db.jpa.JPA;
import play.test.UnitTest;

import db.h2support.H2Examples;
import db.h2support.base.H2AbsenceSupport;


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
    
  @Test
  public void vacationsTestBase() {
    
    //creare il gruppo 
    GroupAbsenceType groupVacations = h2AbsenceSupport
        .getGroupAbsenceType(DefaultGroup.FERIE_CNR);
    
    //creare la persona
    Person person = h2Examples.normalUndefinedEmployee(new LocalDate(2009, 2, 01));
    
    //creare le assenze da considerare
    Absence absence31 = h2AbsenceSupport
        .absence(DefaultAbsenceType.A_31, new LocalDate(2016, 1, 1), Optional.absent(), 0, person);
    Absence absence37 = h2AbsenceSupport
        .absence(DefaultAbsenceType.A_37, new LocalDate(2016, 9, 1), Optional.absent(), 0, person);
    Absence absence32 = h2AbsenceSupport
        .absence(DefaultAbsenceType.A_32, new LocalDate(2016, 9, 10), Optional.absent(), 0, person);
    Absence absence94 = h2AbsenceSupport
        .absence(DefaultAbsenceType.A_94, new LocalDate(2016, 9, 11), Optional.absent(), 0, person);

    final LocalDate today = new LocalDate(2016, 9, 1);

    JPA.em().flush();
    
    VacationSituation vacationSituation = new VacationSituation(person, 
        person.contracts.get(0), 2016, groupVacations, Optional.of(today), absenceService, null);

    assertTrue(vacationSituation.lastYear.expired(Optional.of(today)));
    assertEquals(vacationSituation.lastYear.total(), 28);
    assertEquals(vacationSituation.lastYear.used(), 2);
  }
  
}
