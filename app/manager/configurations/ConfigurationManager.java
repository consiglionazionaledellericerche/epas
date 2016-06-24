package manager.configurations;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.extern.slf4j.Slf4j;

import manager.PeriodManager;
import manager.configurations.EpasParam.EpasParamTimeType;
import manager.configurations.EpasParam.EpasParamValueType;
import manager.configurations.EpasParam.EpasParamValueType.IpList;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;


import models.Configuration;
import models.Office;
import models.Person;
import models.PersonConfiguration;
import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.query.QConfiguration;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

@Slf4j
public class ConfigurationManager {

  protected final JPQLQueryFactory queryFactory;
  private final PeriodManager periodManager;

  @Inject
  ConfigurationManager(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      PeriodManager periodManager) {
    this.queryFactory = new JPAQueryFactory(emp);
    this.periodManager = periodManager;
  }

  /**
   * Tutte le configurazioni di un certo tipo.
   * @param epasParam il parametro
   * @return elenco
   */
  public List<Configuration> configurationWithType(EpasParam epasParam) {
    final QConfiguration configuration = QConfiguration.configuration;

    final JPQLQuery query = queryFactory.from(configuration)
            .where(configuration.epasParam.eq(epasParam));
    return query.list(configuration);
  }
  
  /**
   * Aggiunge una nuova configurazione di tipo LocalTime. 
   * @param epasParam parametro
   * @param target il target
   * @param localTime valore
   * @param begin inizio 
   * @param end fine 
   * @return configurazione
   */
  public IPropertyInPeriod updateLocalTime(EpasParam epasParam, IPropertiesInPeriodOwner target, 
      LocalTime localTime, Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.LOCALTIME));
    return build(epasParam, target,
        EpasParamValueType.formatValue(localTime), begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo LocalTime Interval. 
   * @param epasParam parametro.
   * @param target il target.
   * @param from localTime inzio
   * @param to localTime fine
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public IPropertyInPeriod updateLocalTimeInterval(EpasParam epasParam, 
      IPropertiesInPeriodOwner target, LocalTime from, LocalTime to, 
      Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType
        .equals(EpasParamValueType.LOCALTIME_INTERVAL));
    return build(epasParam, target, EpasParamValueType.formatValue(new LocalTimeInterval(from, to)),
            begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo LocalDate. 
   * @param epasParam parametro
   * @param target il target
   * @param localDate data
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public IPropertyInPeriod updateLocalDate(EpasParam epasParam, IPropertiesInPeriodOwner target, 
      LocalDate localDate, Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.LOCALDATE));
    return build(epasParam, target, 
        EpasParamValueType.formatValue(localDate), begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo DayMonth. 
   * @param epasParam parametro
   * @param target il target 
   * @param day day
   * @param month month
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public IPropertyInPeriod updateDayMonth(EpasParam epasParam, IPropertiesInPeriodOwner target, 
      int day, int month, Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.DAY_MONTH));
    return build(epasParam, target, 
        EpasParamValueType.formatValue(new MonthDay(month, day)), begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo DayMonth con cadenza annuale.  
   * @param epasParam parametro
   * @param target il target 
   * @param day day
   * @param month month
   * @param year anno
   * @param applyToTheEnd se voglio applicare la configurazione anche per gli anni successivi.
   * @return configurazione
   */
  public IPropertyInPeriod updateYearlyDayMonth(EpasParam epasParam, 
      IPropertiesInPeriodOwner target, int day, int month, int year, boolean applyToTheEnd, 
      boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.DAY_MONTH));
    return build(epasParam, target, EpasParamValueType.formatValue(new MonthDay(month, day)), 
        Optional.fromNullable(targetYearBegin(target, year)), 
        Optional.fromNullable(targetYearEnd(target, year)), applyToTheEnd, persist);
  }


  /**
   * Aggiunge una nuova configurazione di tipo Month con cadenza annuale. 
   * @param epasParam parametro
   * @param target il target 
   * @param month month
   * @param begin begin
   * @param end end
   * @param persist persist
   * @return configurazione
   */
  public IPropertyInPeriod updateMonth(EpasParam epasParam, IPropertiesInPeriodOwner target, 
      int month, Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    // TODO: validare il valore 1-12 o fare un tipo specifico.
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.MONTH));
    return build(epasParam, target, EpasParamValueType.formatValue(month), begin, end, false, 
        persist);
  }
  
  /**
   * Aggiunge una nuova configurazione di tipo Month con cadenza annuale. 
   * @param epasParam parametro
   * @param target il target 
   * @param month month
   * @param year anno
   * @param applyToTheEnd se voglio applicare la configurazione anche per gli anni successivi.
   * @return configurazione
   */
  public IPropertyInPeriod updateYearlyMonth(EpasParam epasParam, IPropertiesInPeriodOwner target, 
      int month, int year, boolean applyToTheEnd, boolean persist) {
    // TODO: validare il valore 1-12 o fare un tipo specifico.
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.MONTH));
    return build(epasParam, target, EpasParamValueType.formatValue(month), 
        Optional.fromNullable(targetYearBegin(target, year)), 
        Optional.fromNullable(targetYearEnd(target, year)), applyToTheEnd, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo Boolean.
   * @param epasParam parametro
   * @param target il target 
   * @param value valore
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public IPropertyInPeriod updateBoolean(EpasParam epasParam, IPropertiesInPeriodOwner target, 
      boolean value, Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.BOOLEAN));
    return build(epasParam, target, EpasParamValueType.formatValue(value), 
        begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo Integer.
   * @param epasParam parametro
   * @param target il target 
   * @param value valore
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public IPropertyInPeriod updateInteger(EpasParam epasParam, IPropertiesInPeriodOwner target, 
      Integer value, Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.INTEGER));
    return build(epasParam, target, 
        EpasParamValueType.formatValue(value), begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo Integer con cadenza annuale.
   * @param epasParam parametro
   * @param target il target 
   * @param value valore
   * @param year anno
   * @param applyToTheEnd se voglio applicare la configurazione anche per gli anni successivi.
   * @return configurazione
   */
  public IPropertyInPeriod updateYearlyInteger(EpasParam epasParam, IPropertiesInPeriodOwner target,
      int value, int year, boolean applyToTheEnd, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.INTEGER));
    return build(epasParam, target, EpasParamValueType.formatValue(value), 
        Optional.fromNullable(targetYearBegin(target, year)), 
        Optional.fromNullable(targetYearEnd(target, year)), applyToTheEnd, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo IpList.
   * @param epasParam parametro
   * @param target il target
   * @param values ipList
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public IPropertyInPeriod updateIpList(EpasParam epasParam, IPropertiesInPeriodOwner target, 
      List<String> values, Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.IP_LIST));
    return build(epasParam, target, EpasParamValueType.formatValue(new IpList(values)), 
        begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo Email.
   * @param epasParam parametro
   * @param target il target 
   * @param email email
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public IPropertyInPeriod updateEmail(EpasParam epasParam, IPropertiesInPeriodOwner target, 
      String email, Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    // TODO: validare il valore o fare un tipo specifico.
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.EMAIL));
    return build(epasParam, target, 
        EpasParamValueType.formatValue(email), begin, end, false, persist);
  }

  /**
   * Costruttore generico di una configurazione periodica. Effettua tutti i passaggi di validazione.
   */
  private IPropertyInPeriod build(EpasParam epasParam, IPropertiesInPeriodOwner target, 
      String fieldValue, Optional<LocalDate> begin, Optional<LocalDate> end, boolean applyToTheEnd, 
      boolean persist) {
    if (applyToTheEnd) {
      end = Optional.fromNullable(target.calculatedEnd());
    }
    
    IPropertyInPeriod configurationInPeriod = null;
    
    if (epasParam.target.equals(Office.class)) {
      Configuration configuration = new Configuration();
      configuration.office = (Office)target;
      configuration.fieldValue = fieldValue;
      configuration.epasParam = epasParam;
      configuration.beginDate = configuration.office.beginDate;
      if (begin.isPresent()) {
        configuration.beginDate = begin.get();
      }
      if (end.isPresent()) {
        configuration.endDate = end.get();
      }
      configurationInPeriod = configuration;
    }
    
    if (epasParam.target.equals(Person.class)) {
      PersonConfiguration configuration = new PersonConfiguration();
      configuration.person = (Person)target;
      configuration.fieldValue = fieldValue;
      configuration.epasParam = epasParam;
      configuration.beginDate = configuration.person.beginDate;
      if (begin.isPresent()) {
        configuration.beginDate = begin.get();
      }
      if (end.isPresent()) {
        configuration.endDate = end.get();
      }
      configurationInPeriod = configuration;
    }

    //Controllo sul fatto di essere un parametro generale, annuale, o periodico.
    //Decidere se rimandare un errore al chiamante.
    Verify.verify(validateTimeType(epasParam, configurationInPeriod));

    periodManager.updatePeriods(configurationInPeriod, persist);
    return configurationInPeriod;
  }

  /**
   * Valida il parametro di configurazione sulla base del suo tipo tempo.
   * @param configuration parametro
   * @return esito.
   */
  public boolean validateTimeType(EpasParam epasParam, IPropertyInPeriod configuration) {

    if (epasParam.epasParamTimeType.equals(EpasParamTimeType.GENERAL)) {
      //il parametro deve coprire tutta la durata di un owner.
      return DateUtility.areIntervalsEquals(
          new DateInterval(configuration.getBeginDate(), configuration.calculatedEnd()),
          new DateInterval(configuration.getOwner().getBeginDate(), 
              configuration.getOwner().calculatedEnd()));
    }

    //il parametro PERIODIC non ha vincoli, il parametro YEARLY lo costruisco opportunamente 
    // passando dal builder.
    return true;
  }

  /**
   * Data inizio anno per la sede.  
   */
  public LocalDate targetYearBegin(IPropertiesInPeriodOwner target, int year) {
    LocalDate begin = new LocalDate(year, 1, 1);
    if (target.getBeginDate().getYear() == year && target.getBeginDate().isAfter(begin)) {
      return target.getBeginDate();
    }
    return begin;
  }

  /**
   * Data fine anno per la sede.
   */
  public LocalDate targetYearEnd(IPropertiesInPeriodOwner target, int year) {
    LocalDate end = new LocalDate(year, 12, 31);
    if (target.calculatedEnd() != null && target.calculatedEnd().getYear() == year 
        && target.calculatedEnd().isBefore(end)) {
      return target.calculatedEnd();
    }
    return end;
  }
  
  /**
   * La lista delle configurazioni esistenti della sede per la data.  
   * @param office sede
   * @param date data
   * @return lista di configurazioni
   */
  public List<Configuration> getOfficeConfigurationsByDate(Office office, LocalDate date) {

    List<Configuration> list = Lists.newArrayList();
    for (EpasParam epasParam : EpasParam.values()) {
      for (Configuration configuration : office.configurations) {
        if (!configuration.epasParam.equals(epasParam)) {
          continue;
        }
        if (!DateUtility.isDateIntoInterval(date, configuration.periodInterval())) {
          continue;
        }
        list.add(configuration);
      }
    }
    return list;
  }
  
  /**
   * La lista delle configurazioni esistenti della persona per la data.  
   * @param person person
   * @param date data
   * @return lista di configurazioni
   */
  public List<PersonConfiguration> getPersonConfigurationsByDate(Person person, LocalDate date) {

    List<PersonConfiguration> list = Lists.newArrayList();
    for (EpasParam epasParam : EpasParam.values()) {
      for (PersonConfiguration configuration : person.personConfigurations) {
        if (!configuration.epasParam.equals(epasParam)) {
          continue;
        }
        if (!DateUtility.isDateIntoInterval(date, configuration.periodInterval())) {
          continue;
        }
        list.add(configuration);
      }
    }
    return list;
  }

  /**
   * Preleva il valore della configurazione per l'owner, il tipo e la data.
   * Nota Bene: <br>
   * Nel caso il parametro di configurazione mancasse per la data specificata, 
   * si distinguono i casi:<br>
   * 1) Parametro necessario (appartiene all'intervallo di vita dell'owner): eccezione. 
   *    Questo stato non si verifica e non si deve verificare mai. 
   *    E' giusto interrompere bruscamente la richiesta per evitare effetti collaterali.<br>
   * 2) Parametro non necessario (data al di fuori della vita dell'owner). Di norma è il chiamante
   *    che dovrebbe occuparsi di fare questo controllo, siccome non è sempre così si ritorna un 
   *    valore di cortesia (il default o il più prossimo fra quelli definiti nell'owner).<br>    
   * @param owner sede o persona
   * @param epasParam parametro da ricercare
   * @param date data del valore
   * @return valore formato Object
   */
  private Object getConfigurationValue(IPropertiesInPeriodOwner owner, 
      EpasParam epasParam, LocalDate date) {
    
    // Casi illegali
    if (owner instanceof Office && !epasParam.target.equals(Office.class)) {
      throw new IllegalStateException();
    }
    if (owner instanceof Person && !epasParam.target.equals(Person.class)) {
      throw new IllegalStateException();
    }
    
    List<IPropertyInPeriod> configurations = Lists.newArrayList();
    if (owner instanceof Office) {
      configurations = Lists.newArrayList(((Office)owner).configurations);
    }
    if (owner instanceof Person) {
      configurations = Lists.newArrayList(((Person)owner).personConfigurations);
    }
    
    // Primo tentativo (caso generale)
    for (IPropertyInPeriod configuration : configurations) {
      
      EpasParam currentEpasParam = null;
      String fieldValue = null;
      
      if (configuration instanceof Configuration) {
        currentEpasParam = ((Configuration)configuration).epasParam;
        fieldValue = ((Configuration)configuration).fieldValue;
      }
      if (configuration instanceof PersonConfiguration) {
        currentEpasParam = ((PersonConfiguration)configuration).epasParam;
        fieldValue = ((PersonConfiguration)configuration).fieldValue;
      }
      
      if (!currentEpasParam.equals(epasParam)) {
        continue;
      }
      if (!DateUtility.isDateIntoInterval(date, configuration.periodInterval())) {
        continue;
      }
      return parseValue(currentEpasParam, fieldValue);
    }
    
    // Parametro necessario inesistente
    if (DateUtility.isDateIntoInterval(date, owner.periodInterval())) {
      throw new IllegalStateException();
    }
    
    // Parametro non necessario, risposta di cortesia.
    Object nearestValue = null;
    Integer days = null;

    for (IPropertyInPeriod configuration : configurations) {
      
      EpasParam currentEpasParam = null;
      String fieldValue = null;
      
      if (configuration instanceof Configuration) {
        currentEpasParam = ((Configuration)configuration).epasParam;
        fieldValue = ((Configuration)configuration).fieldValue;
      }
      if (configuration instanceof PersonConfiguration) {
        currentEpasParam = ((PersonConfiguration)configuration).epasParam;
        fieldValue = ((PersonConfiguration)configuration).fieldValue;
      }
      
      if (!currentEpasParam.equals(epasParam)) {
        continue;
      }

      Integer daysInterval;
      if (date.isBefore(configuration.getBeginDate())) {
        daysInterval = DateUtility
            .daysInInterval(new DateInterval(date, configuration.getBeginDate()));
      } else {
        daysInterval = DateUtility
            .daysInInterval(new DateInterval(configuration.calculatedEnd(), date));
      }
      if (days == null) {
        days = daysInterval;
        nearestValue = EpasParamValueType
            .parseValue(epasParam.epasParamValueType, fieldValue);
      } else { 
        if (days > daysInterval) {
          days = daysInterval;
          nearestValue = EpasParamValueType
              .parseValue(epasParam.epasParamValueType, fieldValue);
        }
      }
    }
    
    if (nearestValue != null) {
      log.debug("Ritorno il valore non necessario più prossimo per {} {} {}: {}",
          owner.toString(), epasParam, date, nearestValue);
      return nearestValue;
    } else {
      log.debug("Ritorno il valore non necessario default per {} {} {}: {}",
          owner.toString(), epasParam, date, nearestValue);
      return epasParam.defaultValue; 
    }

  }
  
  /**
   * Costruttore della configurazione di default se non esiste.<br>
   * Verificare che venga chiamata esclusivamente nel caso di nuovo enumerato !!!
   * Di norma la configurazione di default andrebbe creata tramite migrazione o al momento
   * della creazione della sede.
   * @param target sede o persona o ??
   * @param epasParam epasParam
   * @return il valore di default per tutto il periodo del target
   */
  private IPropertyInPeriod buildDefault(IPropertiesInPeriodOwner target, EpasParam epasParam) {
    
    return build(epasParam, target, (String)epasParam.defaultValue, 
        Optional.fromNullable(target.getBeginDate()), Optional.<LocalDate>absent(), true, true);

  }
  
  /**
   * Preleva il valore del parametro generale.
   * @param owner sede o persona
   * @param epasParam tipo parametro
   * @return value
   */
  public Object configValue(IPropertiesInPeriodOwner owner, EpasParam epasParam) {
    Preconditions.checkArgument(epasParam.isGeneral());
    return getConfigurationValue(owner, epasParam, LocalDate.now());
  }

  /**
   * Preleva il valore del parametro alla data.
   * @param owner sede o persona
   * @param epasParam tipo parametro
   * @param date data
   * @return value
   */
  public Object configValue(IPropertiesInPeriodOwner owner, EpasParam epasParam, LocalDate date) {
    return getConfigurationValue(owner, epasParam, date);
  }
  
  /**
   * Preleva il valore del parametro per l'anno.
   * @param owner sede o persona
   * @param epasParam tipo parametro
   * @param year anno
   * @return value
   */
  public Object configValue(IPropertiesInPeriodOwner owner, EpasParam epasParam, int year) {
    Preconditions.checkArgument(epasParam.isYearly());
    LocalDate date = new LocalDate(year, 1, 1);
    if (owner.getBeginDate().getYear() == year) {
      date = owner.getBeginDate();
    }
    return configValue(owner, epasParam, date);
  }
  
  /**
   * Aggiunge i nuovi epasParam quando vengono definiti (con il valore di default).
   * Da chiamare al momento della creazione dell'owner (person/office) ed al bootstrap di epas.
   * @param owner sede o persona
   */
  public void updateConfigurations(IPropertiesInPeriodOwner owner) {
    
    for (EpasParam epasParam : EpasParam.values()) {
      
      // Casi uscita
      if (owner instanceof Office && !epasParam.target.equals(Office.class)) {
        continue;
      }
      if (owner instanceof Person && !epasParam.target.equals(Person.class)) {
        continue;
      }
      
      // Casi da gestire
      if (owner instanceof Office) {
        boolean toCreate = true;
        for (Configuration configuration : ((Office)owner).configurations) {
          if (configuration.epasParam.equals(epasParam)) {
            toCreate = false;
          }
        }
        if (toCreate) {
          buildDefault(owner, epasParam);
        }
      }
      if (owner instanceof Person) {
        boolean toCreate = true;
        for (PersonConfiguration configuration : ((Person)owner).personConfigurations) {
          if (configuration.epasParam.equals(epasParam)) {
            toCreate = false;
          }
        }
        if (toCreate) {
          buildDefault(owner, epasParam);
        }
      }
    }
  }

  /**
   * Converte il formato stringa in formato oggetto per l'epasParam.
   * @param epasParam epasParam
   * @param fieldValue fieldValue
   * @return object
   */
  public Object parseValue(EpasParam epasParam, String fieldValue) {
    return epasParam.epasParamValueType
        .parseValue(epasParam.epasParamValueType, fieldValue);
  }

}
