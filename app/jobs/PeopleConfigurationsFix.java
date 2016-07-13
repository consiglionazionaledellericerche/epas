package jobs;

import lombok.extern.slf4j.Slf4j;

import manager.configurations.ConfigurationManager;

import models.Person;

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
public class PeopleConfigurationsFix extends Job {

  @Inject
  static ConfigurationManager configurationManager;

  public void doJob() {
    List<Person> people = Person.findAll();
    for (Person person : people) {
      log.debug("Fix parametri di configurazione per {}", person.fullName());
      configurationManager.updateConfigurations(person);
    }
  }
}

