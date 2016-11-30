package absences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import manager.PersonDayManager;
import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.model.PeriodChain;
import manager.services.absences.model.ServiceFactories;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;

import org.joda.time.LocalDate;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import absences.AbsenceDefinitions.AbsenceTypeDefinition;
import absences.AbsenceDefinitions.GroupAbsenceTypeDefinition;



public class AbsencesBasicTest {
  
  public static final LocalDate BEGIN_2016 = new LocalDate(2016, 1, 1);
  public static final LocalDate END_2016 = new LocalDate(2016, 12, 31);
  
  public static final LocalDate FERIAL_1_2016 = new LocalDate(2016, 11, 7); //lun
  public static final LocalDate FERIAL_2_2016 = new LocalDate(2016, 11, 8); //mar
  public static final LocalDate FERIAL_3_2016 = new LocalDate(2016, 11, 9); //mer
  
  @Test
  public void test() {
    
    Map<String, AbsenceType> mapAbsenceTypes = Maps.newConcurrentMap();
    
    //creare il gruppo
    GroupAbsenceType group661 = AbsencesMocker.mockedGroup(mapAbsenceTypes, 
        GroupAbsenceTypeDefinition.Group_661);
    
    //creare la persona
    Person person = AbsencesMocker.mockedNormalUndefinedEmployee(BEGIN_2016);
    
    ServiceFactories serviceFactories = getServiceFactories(person, Lists.newArrayList());
    
    //creare la periodChain
    PeriodChain periodChain = serviceFactories.buildPeriodChainPhase1(person, group661, 
        new LocalDate(2016, 11, 15), 
        Lists.newArrayList(), 
        Lists.newArrayList(), 
        Lists.newArrayList());
    
    assertThat(periodChain.from).isEqualTo(BEGIN_2016);
    assertThat(periodChain.to).isEqualTo(END_2016);
    assertThat(!periodChain.periods.isEmpty());
    assertThat(periodChain.periods.get(0).getPeriodTakableAmount()).isEqualTo(1080);
    
    //creare le assenze da considerare
    Absence absence1 = AbsencesMocker.mockedAbsence(mapAbsenceTypes, AbsenceTypeDefinition._661M, 
        FERIAL_1_2016, Optional.of(JustifiedTypeName.specified_minutes), 80);
    Absence absence2 = AbsencesMocker.mockedAbsence(mapAbsenceTypes, AbsenceTypeDefinition._661H1, 
        FERIAL_1_2016, Optional.of(JustifiedTypeName.nothing), 0);
    List<Absence> allPersistedAbsences = Lists.newArrayList(absence1, absence2);
    List<Absence> groupPersistedAbsences = Lists.newArrayList(absence1, absence2);
    
    //creare la assenza da inserire
    Absence toInsert = AbsencesMocker.mockedAbsence(mapAbsenceTypes, AbsenceTypeDefinition._661M, 
        FERIAL_3_2016, Optional.of(JustifiedTypeName.specified_minutes), 40);
    
    serviceFactories.buildPeriodChainPhase2(periodChain, toInsert, 
        allPersistedAbsences, groupPersistedAbsences);
    
    assertThat(periodChain.successPeriodInsert).isNotNull();
    assertThat(periodChain.successPeriodInsert.attemptedInsertAbsence).isEqualTo(toInsert);
    assertThat(periodChain.periods.get(0).getPeriodTakenAmount()).isEqualTo(120);
    
  }
  
  public AbsenceEngineUtility getUtility() {
    return new AbsenceEngineUtility();
  }
  
  public PersonDayManager getPersonDayManager(Person person, List<LocalDate> holidays) {
    PersonDayManager personDayManager = mock(PersonDayManager.class);
    for (LocalDate date : holidays) {
      when(personDayManager.isHoliday(person, date)).thenReturn(true);
    }
    return personDayManager;
  }
  
  public ServiceFactories getServiceFactories(Person person, List<LocalDate> holidays) {
    return new ServiceFactories(getUtility(), null, 
        getPersonDayManager(person, holidays), null, null);
  }
  
  
  
  
}
