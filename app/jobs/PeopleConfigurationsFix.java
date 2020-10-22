package jobs;

import com.google.common.base.Optional;
import java.util.List;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.Person;
import models.PersonConfiguration;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Aggiorna i parametri di configurazione di tutte le persone presenti nel db.
 * Gli aggiornamenti possono essere necessari in casi di inserimento di nuovi
 * parametri.
 * 
 * @author daniele
 * @since 30/06/16.
 */
@Slf4j
@OnApplicationStart(async = true)
public class PeopleConfigurationsFix extends Job<Void> {

  @Inject
  static ConfigurationManager configurationManager;

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    List<Person> people = Person.findAll();
    for (Person person : people) {
      log.debug("Fix parametri di configurazione per {}", person.fullName());
      configurationManager.updateConfigurations(person);
    }
  }
}

