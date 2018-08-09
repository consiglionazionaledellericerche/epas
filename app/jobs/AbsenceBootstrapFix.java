package jobs;

import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;
import injection.StaticInject;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import manager.ConsistencyManager;
import models.Person;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultAbsenceType;
import play.db.jpa.JPAPlugin;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@StaticInject
@OnApplicationStart(async = false)
public class AbsenceBootstrapFix extends Job<Void> {


  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  
  @Inject
  private static ConsistencyManager consistencyManager;
  
  @Inject
  private static IWrapperFactory wrapperFactory;
  
  @SuppressWarnings("deprecation")
  @Override
  public void doJob() {
    
    List<Absence> absences = absenceComponentDao
        .absences(Lists.newArrayList(DefaultAbsenceType.A_661MO.getCode()));
    
    DateInterval yearInterval = DateUtility.getYearInterval(2018);
    
    Set<Person> toUpdate = Sets.newHashSet();
    
    Set<Person> toScan = Sets.newHashSet();
    
    JustifiedType allDay = absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.all_day);
    Optional<AbsenceType> a661G = absenceComponentDao
        .absenceTypeByCode(DefaultAbsenceType.A_661G.getCode());
    
    // tutte le assenze 661MO
    for (Absence absence : absences) {
      
      // utilizzate dal 1/1/2018 al 31/12/2018
      if (!DateUtility.isDateIntoInterval(absence.personDay.getDate(), yearInterval)) {
        continue;
      }
      
      IWrapperPersonDay wPersonDay = wrapperFactory.create(absence.personDay);  
      
      Optional<WorkingTimeTypeDay>  wttd = wPersonDay.getWorkingTimeTypeDay();
      
      if (!wttd.isPresent()) {
        continue;
      }
      
      // di 7:12 quando la persona ha un orario 7:12
      if (wttd.get().workingTime == 432 && absence.justifiedMinutes == 432) {

        // sono convertite in 661G
        absence.justifiedMinutes = null;
        absence.justifiedType = allDay;
        absence.absenceType = a661G.get();
        absence.save();
        toUpdate.add(absence.personDay.person);
        continue;
      }
      
      // non di 7:12 ma di un valore uguale o superiore a 6 ore
      if (absence.justifiedMinutes > 300) {
        
        // saranno inserite in uno stato di warning
        toScan.add(absence.personDay.person);
      }
      
    }
    
    JPAPlugin.closeTx(false);
    JPAPlugin.startTx(false);
    
    if (!toUpdate.isEmpty()) {
      for (Person person : toUpdate) {
        consistencyManager.updatePersonSituation(person.id, yearInterval.getBegin());
      }
      for (Person person : toScan) {
        consistencyManager.updatePersonSituation(person.id, yearInterval.getBegin());
      }
    }
  }
}

