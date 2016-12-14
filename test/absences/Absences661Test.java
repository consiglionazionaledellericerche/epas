package absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import db.h2support.H2Examples;
import db.h2support.base.AbsenceDefinitions.AbsenceTypeDefinition;
import db.h2support.base.AbsenceDefinitions.GroupAbsenceTypeDefinition;
import db.h2support.base.H2AbsenceSupport;

import injection.StaticInject;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.model.PeriodChain;
import manager.services.absences.model.ServiceFactories;

import models.Person;
import models.absences.Absence;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;

import org.joda.time.LocalDate;
import org.junit.Test;

import play.test.Fixtures;
import play.test.UnitTest;

import java.util.List;


@StaticInject
public class Absences661Test extends UnitTest {
  
  public static final LocalDate BEGIN_2016 = new LocalDate(2016, 1, 1);
  public static final LocalDate END_2016 = new LocalDate(2016, 12, 31);
  
  public static final LocalDate FERIAL_1_2016 = new LocalDate(2016, 11, 7); //lun
  public static final LocalDate FERIAL_2_2016 = new LocalDate(2016, 11, 8); //mar
  public static final LocalDate FERIAL_3_2016 = new LocalDate(2016, 11, 9); //mer
  
  @Inject private static H2Examples h2Examples;
  @Inject private static H2AbsenceSupport h2AbsenceSupport;
  @Inject private static ServiceFactories serviceFactories;
  
  @Test
  public void test() {
    
    Fixtures.deleteDatabase();
    
    //creare il gruppo
    GroupAbsenceType group661 = h2AbsenceSupport
        .getGroupAbsenceType(GroupAbsenceTypeDefinition.Group_661);
    
    //creare la persona
    Person person = h2Examples.normalUndefinedEmployee(BEGIN_2016);
    
    //creare la periodChain
    PeriodChain periodChain = serviceFactories.buildPeriodChainPhase1(person, group661, 
        new LocalDate(2016, 11, 15), 
        Lists.newArrayList(), 
        Lists.newArrayList(), 
        Lists.newArrayList());
    
    assertEquals(periodChain.from, BEGIN_2016);
    assertEquals(periodChain.to, END_2016);
    assertTrue(!periodChain.periods.isEmpty());
    assertEquals(periodChain.periods.get(0).getPeriodTakableAmount(), 1080);
    
    //creare le assenze da considerare
    Absence absence1 = h2AbsenceSupport.absenceInstance(AbsenceTypeDefinition._661M, 
        FERIAL_1_2016, Optional.of(JustifiedTypeName.specified_minutes), 80);
    Absence absence2 = h2AbsenceSupport.absenceInstance(AbsenceTypeDefinition._661H1, 
        FERIAL_1_2016, Optional.of(JustifiedTypeName.nothing), 0);
    List<Absence> allPersistedAbsences = Lists.newArrayList(absence1, absence2);
    List<Absence> groupPersistedAbsences = Lists.newArrayList(absence1, absence2);
    
    //creare la assenza da inserire
    Absence toInsert = h2AbsenceSupport.absenceInstance(AbsenceTypeDefinition._661M, 
        FERIAL_3_2016, Optional.of(JustifiedTypeName.specified_minutes), 40);
    
    serviceFactories.buildPeriodChainPhase2(periodChain, toInsert, 
        allPersistedAbsences, groupPersistedAbsences);
    
    assertNotNull(periodChain.successPeriodInsert);
    assertEquals(periodChain.successPeriodInsert.attemptedInsertAbsence, toInsert);
    assertEquals(periodChain.periods.get(0).getPeriodTakenAmount(), 120);
    
  }
  
  public AbsenceEngineUtility getUtility() {
    return new AbsenceEngineUtility();
  }
  
}
