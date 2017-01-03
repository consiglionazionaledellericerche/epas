package jobs;

import com.google.common.base.Optional;

import dao.OfficeDao;
import dao.absences.AbsenceComponentDao;

import lombok.extern.slf4j.Slf4j;

import manager.configurations.ConfigurationManager;
import manager.services.absences.enums.CategoryTabEnum;
import manager.services.absences.enums.GroupEnum;

import models.Person;
import models.absences.CategoryGroupAbsenceType;
import models.absences.CategoryTab;
import models.absences.GroupAbsenceType;

import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

import java.util.List;

import javax.inject.Inject;

/**
 * Crea tab categorie gruppi secondo enums.
 * @author alessandro
 * 
 */
@Slf4j
@OnApplicationStart(async = true)
public class AbsenceGroupConfigurations extends Job<Void> {

  @Inject
  static AbsenceComponentDao absenceComponentDao;

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    //1 costruzione gruppi non esistenti
    for (GroupEnum groupEnum : GroupEnum.values()) {
      Optional<GroupAbsenceType> group = 
          absenceComponentDao.groupAbsenceTypeByName(groupEnum.name());
      if (!group.isPresent()) {
        //TODO: costruzione
        // Ferie che riducono i giorni
        // Legge 104 codice 26
      }

      //1b assegnazione categoria corretta (e sua eventuale costruzione)
      CategoryGroupAbsenceType category = absenceComponentDao
          .categoryByName(groupEnum.category.name());
      if (category == null) {
        category = new CategoryGroupAbsenceType();
        category.name = groupEnum.category.name();
        category.description = groupEnum.category.label; 
        category.priority = groupEnum.category.priority;
        category.save();
      }
      
      group.get().category = category;
      group.get().save();
      
      //1c assegnazione tab (e sua eventuale costruzione)
      CategoryTab tab = absenceComponentDao.tabByName(groupEnum.category.categoryTab.name());
      if (tab == null) {
        tab = new CategoryTab();
        tab.name = groupEnum.category.categoryTab.name();
        tab.description = groupEnum.category.categoryTab.label;
        tab.priority = groupEnum.category.categoryTab.priority;
        tab.save();
      }
      
      group.get().category.tab = tab;
      group.get().category.save();
    }

        

    
    //rimozione rimozione categorie non associate ad alcun gruppo
    
    //rimozione tab non associate ad alcuna categoria
    
    

    

  }
}

