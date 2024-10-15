package jobs;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import org.joda.time.MonthDay;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dao.AbsenceDao;
import dao.OfficeDao;
import dao.absences.AbsenceComponentDao;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.AbsenceManager;
import manager.ConsistencyManager;
import manager.PeriodManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.configurations.EpasParam.EpasParamValueType;
import manager.recaps.recomputation.RecomputeRecap;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import models.Configuration;
import models.Office;
import models.Person;
import models.absences.Absence;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.definitions.DefaultGroup;
import models.base.IPropertyInPeriod;
import play.Play;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@Slf4j
@OnApplicationStart(async=true)
public class ConvertVacations extends Job {

  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static PeriodManager periodManager;
  @Inject
  static ConsistencyManager consistencyManager;
  @Inject
  static AbsenceDao absenceDao;
  @Inject
  static AbsenceManager absenceManager;
  @Inject
  static AbsenceService absenceService;
  @Inject
  static AbsenceComponentDao absenceComponentDao;

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.debug("Start Job convert vacations");

    /*
     * Prima cosa devo verificare che tutte le configurazioni delle sedi abbiano il 31/12 come limite
     * alle ferie dell'anno 2023. Se così non fosse devo modificarlo
     */
    List<Office> officeList = officeDao.allEnabledOffices();
    MonthDay dayMonth = (MonthDay) EpasParamValueType
        .parseValue(EpasParamValueType.DAY_MONTH, "31/12");
    Configuration newConfiguration = null;
    for (Office office : officeList) {
      log.debug("Controllo il parametro della sede {}", office.getName());
      val oldConfiguration = (MonthDay) configurationManager.configValue(office, EpasParam.EXPIRY_VACATION_PAST_YEAR, 2023);
      log.debug("Il valore del parametro è: {}", oldConfiguration);
      newConfiguration = (Configuration) configurationManager.updateDayMonth(EpasParam.EXPIRY_VACATION_PAST_YEAR,
          office, dayMonth.getDayOfMonth(), dayMonth.getMonthOfYear(),
          Optional.fromNullable(new LocalDate(2023,1,1)),
          Optional.fromNullable(new LocalDate(2023,12,31)), false);
      List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(newConfiguration, false);
      RecomputeRecap recomputeRecap =
          periodManager.buildRecap(office.getBeginDate(),
              Optional.fromNullable(LocalDate.now()),
              periodRecaps, Optional.<LocalDate>absent());
      recomputeRecap.epasParam = EpasParam.EXPIRY_VACATION_PAST_YEAR;
      periodManager.updatePeriods(newConfiguration, true);

      consistencyManager.performRecomputation(office,
          EpasParam.EXPIRY_VACATION_PAST_YEAR.recomputationTypes, recomputeRecap.recomputeFrom);
      log.debug("Il nuovo valore del parametro è: {}", newConfiguration.getFieldValue());
    }

    /*
     * Ora occorre trovare tutte le assenze con codice 37 fatte dal 1/9/2024 al 31/12/2024
     * e cambiarle in 31
     */
    log.debug("Cerco le assenze con codice 37...");
    List<Absence> absenceList = absenceDao.getAbsenceByCodeInPeriod(Optional.absent(), 
        Optional.fromNullable("37"), new LocalDate(2024,9,1), new LocalDate(2024,12,31), 
        Optional.absent(), false, false);
    log.debug("Sono state trovate {} assenze con codice 37 dal 1 settembre al 31 dicembre", absenceList.size());
    Map<Person, List<Absence>> map = Maps.newHashMap();
    for (Absence abs : absenceList) {
      List<Absence> list = map.get(abs.getPersonDay().getPerson());
      if (list == null || list.isEmpty()) {
        list = Lists.newArrayList();        
      }
      list.add(abs);
      map.put(abs.getPersonDay().getPerson(), list);
      log.debug("Caricata sulla mappa l'assenza {} di {} del giorno {}", 
          abs.getAbsenceType().getCode(), abs.getPersonDay().getPerson(), abs.getPersonDay().getDate());
    }
    int count = 0;
    JPA.em().flush();
    JPAPlugin.closeTx(false);
    for (Map.Entry<Person, List<Absence>> entry : map.entrySet()) {
      JPAPlugin.startTx(false);
      log.debug("Rimuovo le assenze 37 di {}", entry.getKey().getFullname());
      for (Absence abs : entry.getValue()) {
        int deleted = absenceManager
            .removeAbsencesInPeriod(entry.getKey(), abs.getPersonDay().getDate(), 
                abs.getPersonDay().getDate(), abs.getAbsenceType());
        if (deleted != 0) {
          count++;
          log.debug("Rimossa assenza 37 del giorno {}", abs.getPersonDay().getDate());
        }
      }    
      JPA.em().flush();
      JPAPlugin.closeTx(false);
    }
    log.debug("Rimosse {} assenze.", count);
    /*
     * Ora occorre inserire al posto dei 37 i 31
     */
    JPAPlugin.startTx(false);
    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    InsertReport insertReport = null;
    
    for (Map.Entry<Person, List<Absence>> entry : map.entrySet()) {
      
      for (Absence abs : entry.getValue()) {
        // forziamo l'inserimento per non fare i controlli, magari risparmiamo del tempo
        insertReport = absenceService.insert(entry.getKey(), vacationGroup, 
            abs.getPersonDay().getDate(), abs.getPersonDay().getDate(),
            null, abs.getJustifiedType(), 1, null, false, absenceManager);

        absenceManager.saveAbsences(insertReport, entry.getKey(), abs.getPersonDay().getDate(), null, 
            abs.getJustifiedType(), vacationGroup);

      }
      JPA.em().flush();
      JPAPlugin.closeTx(false);
    }

  }
}
