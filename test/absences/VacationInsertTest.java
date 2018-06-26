package absences;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;

import db.h2support.H2Examples;
import db.h2support.base.H2AbsenceSupport;

import injection.StaticInject;

import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;

import models.Person;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultGroup;

import org.joda.time.LocalDate;
import org.junit.Test;

import play.test.UnitTest;


@StaticInject
public class VacationInsertTest extends UnitTest {
  
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
    
  /**
   * Issue #258.
   */
  @Test
  public void bucciCase() {
    
    absenceService.enumInitializator();
    
    Person person = h2Examples.normalEmployee(new LocalDate(2014, 3, 17), 
        Optional.of(new LocalDate(2019, 3, 16)));
    
    //le ferie del 2015 utilizzate
    h2AbsenceSupport.multipleAllDayInstances(person, DefaultAbsenceType.A_31, 
        ImmutableSet.of(
            new LocalDate(2016, 1, 4),
            new LocalDate(2016, 1, 5),
            new LocalDate(2016, 3, 24),
            new LocalDate(2016, 3, 25),
            new LocalDate(2016, 3, 29),
            new LocalDate(2016, 5, 11),
            new LocalDate(2016, 7, 13),
            new LocalDate(2016, 7, 25),
            new LocalDate(2016, 7, 26),
            new LocalDate(2016, 7, 27)
            ));
    
    h2AbsenceSupport.multipleAllDayInstances(person, DefaultAbsenceType.A_32, 
        ImmutableSet.of(
            new LocalDate(2015, 7, 29),
            new LocalDate(2015, 7, 30),
            new LocalDate(2015, 7, 31),
            new LocalDate(2015, 8, 3),
            new LocalDate(2015, 8, 4),
            new LocalDate(2015, 8, 5),
            new LocalDate(2015, 8, 6),
            new LocalDate(2015, 8, 7),
            new LocalDate(2015, 8, 26),
            new LocalDate(2015, 8, 27),
            new LocalDate(2015, 8, 28),
            new LocalDate(2015, 8, 31),
            new LocalDate(2015, 12, 24),
            new LocalDate(2015, 12, 30),
            new LocalDate(2015, 12, 31)
            ));
    
    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    
    JustifiedType allDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day);
    
    AbsenceType absenceType = absenceComponentDao
        .absenceTypeByCode(DefaultAbsenceType.A_32.getCode()).get();
    
    LocalDate today = new LocalDate(2015, 8, 24);
    
    InsertReport insertReport = absenceService.insert(person, vacationGroup, today, null, 
        absenceType, allDay, null, null, false, null);
    
    assertEquals(insertReport.howManySuccess(), 1);
  }
  
}
