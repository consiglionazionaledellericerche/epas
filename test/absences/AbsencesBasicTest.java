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
  
  @Test
  public void test() {
    
    Map<String, AbsenceType> mapAbsenceTypes = Maps.newConcurrentMap();
    
    //creare il gruppo
    GroupAbsenceType group661 = AbsencesMocker.mockedGroup(mapAbsenceTypes, 
        GroupAbsenceTypeDefinition.Group_661);
    
    //creare la persona
    Person person = AbsencesMocker.mockNormalUndefinedEmployee(DATE_1);
    
    //creare la periodChain
    PeriodChain periodChain = getServiceFactories().buildPeriodChainPhase1(person, group661, 
        new LocalDate(2016, 11, 15), 
        Lists.newArrayList(), 
        Lists.newArrayList(), 
        Lists.newArrayList());
    
    assertThat(periodChain.from).isEqualTo(new LocalDate(2016, 1 ,1));
    assertThat(periodChain.to).isEqualTo(new LocalDate(2016, 12, 31));
    
    getServiceFactories().buildPeriodChainPhase2(periodChain, null, 
        Lists.newArrayList(), Lists.newArrayList());
    
    assertThat(!periodChain.periods.isEmpty());
    assertThat(periodChain.periods.get(0).getPeriodTakenAmount()).isEqualTo(0);
    assertThat(periodChain.periods.get(0).getPeriodTakableAmount()).isEqualTo(1080);
    
  }
  
  public AbsenceEngineUtility getUtility() {
    return new AbsenceEngineUtility(null, null, null, null);
  }
  
  public ServiceFactories getServiceFactories() {
    return new ServiceFactories(getUtility(), null, null);
  }
  
  
  
  
}
