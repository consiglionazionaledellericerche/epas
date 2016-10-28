package jobs;

import lombok.extern.slf4j.Slf4j;

import manager.configurations.ConfigurationManager;

import models.Office;

import play.jobs.Job;
import play.jobs.OnApplicationStart;

import java.util.List;

import javax.inject.Inject;

/**
 * @author daniele
 * @since 30/06/16.
 */
@Slf4j
@OnApplicationStart(async = true)
public class OfficeConfigurationFix extends Job<Void> {

  @Inject
  static ConfigurationManager configurationManager;

  public void doJob() {
    List<Office> offices = Office.findAll();
    for (Office office : offices) {
      log.debug("Fix parametri di configurazione della sede {}", office);
      configurationManager.updateConfigurations(office);
    }
  }
}
