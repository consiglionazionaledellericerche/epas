/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import common.injection.StaticInject;
import dao.absences.AbsenceComponentDao;
import db.h2support.H2Examples;
import db.h2support.base.H2AbsenceSupport;
import java.util.List;
import manager.services.absences.AbsenceService;
import manager.services.absences.model.PeriodChain;
import manager.services.absences.model.ServiceFactories;
import models.Person;
import models.absences.Absence;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultGroup;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.test.UnitTest;


@StaticInject
public class Absences661Test extends UnitTest {
  
  public static final LocalDate BEGIN_2016 = new LocalDate(2016, 1, 1);
  public static final LocalDate MID_2016 = new LocalDate(2016, 7, 1);
  public static final LocalDate END_2016 = new LocalDate(2016, 12, 31);
  
  public static final LocalDate FERIAL_1_2016 = new LocalDate(2016, 11, 7); //lun
  public static final LocalDate FERIAL_2_2016 = new LocalDate(2016, 11, 8); //mar
  public static final LocalDate FERIAL_3_2016 = new LocalDate(2016, 11, 9); //mer
  
  public static final LocalDate BEGIN_2018 = new LocalDate(2018, 1, 1);
  public static final LocalDate FERIAL_1_2018 = new LocalDate(2018, 8, 6); //lun
  
  @Inject 
  private static H2Examples h2Examples;
  @Inject 
  private static H2AbsenceSupport h2AbsenceSupport;
  @Inject 
  private static ServiceFactories serviceFactories;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  @Inject
  private static AbsenceService absenceService;
  
  @Test
  public void test() {
    
    absenceService.enumInitializator();
    
    //creare il gruppo
    GroupAbsenceType group661 = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.G_661.name()).get();
    
    //creare la persona
    Person person = h2Examples.normalEmployee(BEGIN_2016, Optional.absent());
    
    //creare la periodChain
    PeriodChain periodChain = serviceFactories.buildPeriodChainPhase1(person, group661, 
        new LocalDate(2016, 11, 15), 
        Lists.newArrayList(), 
        person.contracts,
        Lists.newArrayList(), 
        Lists.newArrayList());
    
    assertEquals(periodChain.from, BEGIN_2016);
    assertEquals(periodChain.to, END_2016);
    assertTrue(!periodChain.periods.isEmpty());
    assertEquals(periodChain.periods.get(0).getPeriodTakableAmount(), 1080);
    
    //creare le assenze da considerare
    Absence absence1 = h2AbsenceSupport.absenceInstance(DefaultAbsenceType.A_661MO, 
        FERIAL_1_2016, Optional.of(JustifiedTypeName.specified_minutes), 80);
    Absence absence2 = h2AbsenceSupport.absenceInstance(DefaultAbsenceType.A_661H1, 
        FERIAL_1_2016, Optional.of(JustifiedTypeName.nothing), 0);
    List<Absence> allPersistedAbsences = Lists.newArrayList(absence1, absence2);
    List<Absence> groupPersistedAbsences = Lists.newArrayList(absence1, absence2);
    
    //creare la assenza da inserire
    Absence toInsert = h2AbsenceSupport.absenceInstance(DefaultAbsenceType.A_661MO, 
        FERIAL_3_2016, Optional.of(JustifiedTypeName.specified_minutes), 40);
    
    serviceFactories.buildPeriodChainPhase2(periodChain, toInsert, 
        allPersistedAbsences, groupPersistedAbsences, person.contracts);
    
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
        
    absenceService.enumInitializator();
    
    //creare il gruppo
    GroupAbsenceType group661 = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.G_661.name()).get();
    
    // CASO 1 
    //la persona inizia a lavorare a metà anno
    Person person = h2Examples.normalEmployee(MID_2016, Optional.absent());
    
    //creare la periodChain
    PeriodChain periodChain = serviceFactories.buildPeriodChainPhase1(person, group661, 
        new LocalDate(2016, 11, 15), 
        Lists.newArrayList(), 
        person.contracts,
        Lists.newArrayList(), 
        Lists.newArrayList());
    
    //dal 2016-7-1 al 2016-12-31 sono 184 giorni su 366. 
    // Col corretto comportamento il codice 661 non si riproporziona
    assertEquals(periodChain.periods.get(0).getPeriodTakableAmount(), 1080);
    
    //CASO 2 
    //la persona ha il part time 50%
    person = h2Examples.partTime50Employee(BEGIN_2016);

    periodChain = serviceFactories.buildPeriodChainPhase1(person, group661, 
        new LocalDate(2016, 11, 15), 
        Lists.newArrayList(), 
        person.contracts,
        Lists.newArrayList(), 
        Lists.newArrayList());

    //1080 * 50 / 100 = 540
    // In questo caso il riproporzionamento è corretto: vale solo sull'orario di lavoro
    assertEquals(periodChain.periods.get(0).getPeriodTakableAmount(), 540);
    
    //CASO 3 
    //la persona inizia a lavorare a metà anno con part time 50% 

    person = h2Examples.partTime50Employee(MID_2016);

    periodChain = serviceFactories.buildPeriodChainPhase1(person, group661, 
        new LocalDate(2016, 11, 15), 
        Lists.newArrayList(), 
        person.contracts,
        Lists.newArrayList(), 
        Lists.newArrayList());

    //Anche in questo caso consideriamo solo il riproporzionamento dovuto all'orario di lavoro
    assertEquals(periodChain.periods.get(0).getPeriodTakableAmount(), 540);

    
  }
  
  /**
   * Quando il mio orario di lavoro è 7:12 la conversione del 661G deve essere 6 ore.
   */
  @Test
  public void sixHourRule() {
    
    absenceService.enumInitializator();
    
    //creare la persona con orario normale (7:12)
    Person person = h2Examples.normalEmployee(BEGIN_2016, Optional.absent());
    
    // persisto una assenza di tipo 661G
    h2AbsenceSupport.absence(
        DefaultAbsenceType.A_661G, FERIAL_1_2018, Optional.absent(), 0, person);
    
    // eseguo lo scanner
    absenceService.scanner(person, BEGIN_2018);
    
    //fetch del gruppo
    GroupAbsenceType group661 = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.G_661.name()).get();
    
    // calcolo la situazione residuale
    PeriodChain residual = absenceService.residual(person, group661, BEGIN_2018);

    // le ore consumate devono essere 360
    assertEquals(residual.periods.iterator().next().getPeriodTakenAmount(), 360);
    
  }
  
}
