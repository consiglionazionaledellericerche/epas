package absences;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.model.PeriodChain;
import manager.services.absences.model.ServiceFactories;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;

import org.joda.time.LocalDate;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import absences.AbsenceDefinitions.GroupAbsenceTypeDefinition;



public class AbsencesBasicTest {
  
  public static final LocalDate DATE_1 = new LocalDate(2016, 1, 1);
  
  /*
  PersonDay personDay = new PersonDay(null, second);
    List<Stamping> stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 30, WayType.in, null));
    stampings.add(stampings(personDay, 11, 30, WayType.out, null));
    
    stampings.add(stampings(personDay, 15, 30, WayType.in, null));
    stampings.add(stampings(personDay, 19, 30, WayType.out, null));
    
    personDay.setStampings(stampings);
    
    personDayManager.updateTimeAtWork(personDay, normalDay(), false, 
        startLunch, endLunch, startWork, endWork);
    personDayManager.updateTicketAvailable(personDay, normalDay(), false);
    
    assertThat(personDay.getTimeAtWork()).isEqualTo(420);   //7:00 ore
    assertThat(personDay.getStampingsTime()).isEqualTo(420);//7:00 ore     
    assertThat(personDay.getDecurted()).isEqualTo(null);      //00 minuti
    assertThat(personDay.isTicketAvailable).isEqualTo(true);
  */
  
  @Test
  public void test() {
    
    Map<String, AbsenceType> mapAbsenceTypes = Maps.newConcurrentMap();
    
    //creare il gruppo
    GroupAbsenceType group661 = AbsencesMocker.mockedGroup(mapAbsenceTypes, 
        GroupAbsenceTypeDefinition.Group_661);
    
    //creare la persona
    Person person = AbsencesMocker.mockNormalUndefinedEmployee(DATE_1);
    
    List<Absence> orderedAbsence = Lists.newArrayList();
    List<Absence> allOrderedAbsence = Lists.newArrayList();
    
    AbsenceEngineUtility absenceEngineUtility = 
        new AbsenceEngineUtility(null, null, null, null);
    ServiceFactories serviceFactories = new ServiceFactories(
        absenceEngineUtility, 
        DependencyMocker.absenceComponentDao(orderedAbsence,allOrderedAbsence),
        null);
    
    //creare la periodChain
    PeriodChain periodChain = serviceFactories.buildPeriodChain(person, group661, 
        new LocalDate(2016, 11, 15), 
        Lists.newArrayList(), null, Lists.newArrayList(), person.getContracts(), 
        Lists.newArrayList());
    
    assertThat(!periodChain.periods.isEmpty());
    assertThat(periodChain.periods.get(0).getPeriodTakenAmount()).isEqualTo(0);
    assertThat(periodChain.periods.get(0).getPeriodTakableAmount()).isEqualTo(1080);
    
  }
  
  
}
