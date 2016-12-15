package absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import db.h2support.H2Examples;
import db.h2support.base.AbsenceDefinitions.AbsenceTypeDefinition;
import db.h2support.base.AbsenceDefinitions.GroupAbsenceTypeDefinition;
import db.h2support.base.H2AbsenceSupport;

import injection.StaticInject;

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
  public static final LocalDate MID_2016 = new LocalDate(2016, 7, 1);
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
  
  /**
   * Quando un dipendente non lavora per tutto l'anno e/o ha un tempo a lavoro part time, le 18
   * ore annue di 661 si riducono proporzionalmente.
   */
  @Test
  public void adjustmentLimit() {
    
    Fixtures.deleteDatabase();
    
    //creare il gruppo
    GroupAbsenceType group661 = h2AbsenceSupport
        .getGroupAbsenceType(GroupAbsenceTypeDefinition.Group_661);
    
    // CASO 1 
    //la persona inizia a lavorare a metà anno
    Person person = h2Examples.normalUndefinedEmployee(MID_2016);
    
    //creare la periodChain
    PeriodChain periodChain = serviceFactories.buildPeriodChainPhase1(person, group661, 
        new LocalDate(2016, 11, 15), 
        Lists.newArrayList(), 
        Lists.newArrayList(), 
        Lists.newArrayList());
    
    //dal 2016-7-1 al 2016-12-31 sono 184 giorni su 366. da 1080 si passa a 542
    // 366 : 1080 = 184 : x
    assertEquals(periodChain.periods.get(0).getPeriodTakableAmount(), 542);
    
    //CASO 2 
    //la persona ha il part time 50%
    person = h2Examples.partTime50UndefinedEmployee(BEGIN_2016);

    periodChain = serviceFactories.buildPeriodChainPhase1(person, group661, 
        new LocalDate(2016, 11, 15), 
        Lists.newArrayList(), 
        Lists.newArrayList(), 
        Lists.newArrayList());

    //1080 * 50 / 100 = 540
    assertEquals(periodChain.periods.get(0).getPeriodTakableAmount(), 540);
    
    //CASO 3 
    //la persona inizia a lavorare a metà anno con part time 50% 

    person = h2Examples.partTime50UndefinedEmployee(MID_2016);

    periodChain = serviceFactories.buildPeriodChainPhase1(person, group661, 
        new LocalDate(2016, 11, 15), 
        Lists.newArrayList(), 
        Lists.newArrayList(), 
        Lists.newArrayList());

    //Si parte dai 542 di prima ottenuti lavorando 184 giorni, ridotti al 50 % 
    assertEquals(periodChain.periods.get(0).getPeriodTakableAmount(), 271);

    
  }
  
}
