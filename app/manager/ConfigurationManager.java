package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import models.Configuration;
import models.Office;
import models.enumerate.EpasParam;
import models.enumerate.EpasParam.EpasParamTimeType;
import models.enumerate.EpasParam.EpasParamValueType;

import models.enumerate.EpasParam.EpasParamValueType.DayMonth;
import models.enumerate.EpasParam.EpasParamValueType.IpList;
import models.enumerate.EpasParam.EpasParamValueType.LocalTimeInterval;


import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

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
   * Aggiunge una nuova configurazione di tipo LocalTime. 
   * @param epasParam parametro
   * @param office sede
   * @param localTime valore
   * @param begin inizio 
   * @param end fine 
   * @return configurazione
   */
  public Configuration updateLocalTime(EpasParam epasParam, Office office, LocalTime localTime, 
      Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.LOCALTIME));
    return build(epasParam, office,
        EpasParamValueType.formatValue(localTime), begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo LocalTime Interval. 
   * @param epasParam parametro.
   * @param office sede.
   * @param from localTime inzio
   * @param to localTime fine
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public Configuration updateLocalTimeInterval(EpasParam epasParam, Office office, 
      LocalTime from, LocalTime to, 
      Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType
        .equals(EpasParamValueType.LOCALTIME_INTERVAL));
    return build(epasParam, office, EpasParamValueType.formatValue(new LocalTimeInterval(from, to)),
            begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo LocalDate. 
   * @param epasParam parametro
   * @param office sede
   * @param localDate data
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public Configuration updateLocalDate(EpasParam epasParam, Office office, LocalDate localDate, 
      Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.LOCALDATE));
    return build(epasParam, office, 
        EpasParamValueType.formatValue(localDate), begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo DayMonth. 
   * @param epasParam parametro
   * @param office sede 
   * @param day day
   * @param month month
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public Configuration updateDayMonth(EpasParam epasParam, Office office, int day, int month, 
      Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.DAY_MONTH));
    return build(epasParam, office, 
        EpasParamValueType.formatValue(new DayMonth(day, month)), begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo DayMonth con cadenza annuale.  
   * @param epasParam parametro
   * @param office sede 
   * @param day day
   * @param month month
   * @param year anno
   * @param applyToTheEnd se voglio applicare la configurazione anche per gli anni successivi.
   * @return configurazione
   */
  public Configuration updateYearlyDayMonth(EpasParam epasParam, Office office, int day, int month, 
      int year, boolean applyToTheEnd, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.DAY_MONTH));
    return build(epasParam, office, EpasParamValueType.formatValue(new DayMonth(day, month)), 
        Optional.fromNullable(officeYearBegin(office, year)), 
        Optional.fromNullable(officeYearEnd(office, year)), applyToTheEnd, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo Month con cadenza annuale. 
   * @param epasParam parametro
   * @param office sede 
   * @param month month
   * @param year anno
   * @param applyToTheEnd se voglio applicare la configurazione anche per gli anni successivi.
   * @return configurazione
   */
  public Configuration updateYearlyMonth(EpasParam epasParam, Office office, int month, 
      int year, boolean applyToTheEnd, boolean persist) {
    // TODO: validare il valore 1-12 o fare un tipo specifico.
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.MONTH));
    return build(epasParam, office, EpasParamValueType.formatValue(month), 
        Optional.fromNullable(officeYearBegin(office, year)), 
        Optional.fromNullable(officeYearEnd(office, year)), applyToTheEnd, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo Boolean.
   * @param epasParam parametro
   * @param office sede 
   * @param value valore
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public Configuration updateBoolean(EpasParam epasParam, Office office, boolean value, 
      Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.BOOLEAN));
    return build(epasParam, office, EpasParamValueType.formatValue(value), 
        begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo Integer.
   * @param epasParam parametro
   * @param office sede 
   * @param value valore
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public Configuration updateInteger(EpasParam epasParam, Office office, Integer value, 
      Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.INTEGER));
    return build(epasParam, office, 
        EpasParamValueType.formatValue(value), begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo Integer con cadenza annuale.
   * @param epasParam parametro
   * @param office sede 
   * @param value valore
   * @param year anno
   * @param applyToTheEnd se voglio applicare la configurazione anche per gli anni successivi.
   * @return configurazione
   */
  public Configuration updateYearlyInteger(EpasParam epasParam, Office office, 
      int value, int year, boolean applyToTheEnd, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.INTEGER));
    return build(epasParam, office, EpasParamValueType.formatValue(value), 
        Optional.fromNullable(officeYearBegin(office, year)), 
        Optional.fromNullable(officeYearEnd(office, year)), applyToTheEnd, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo IpList.
   * @param epasParam parametro
   * @param office sede
   * @param values ipList
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public Configuration updateIpList(EpasParam epasParam, Office office, List<String> values, 
      Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.IP_LIST));
    return build(epasParam, office, EpasParamValueType.formatValue(new IpList(values)), 
        begin, end, false, persist);
  }

  /**
   * Aggiunge una nuova configurazione di tipo Email.
   * @param epasParam parametro
   * @param office sede 
   * @param email email
   * @param begin inizio
   * @param end fine
   * @return configurazione
   */
  public Configuration updateEmail(EpasParam epasParam, Office office, String email, 
      Optional<LocalDate> begin, Optional<LocalDate> end, boolean persist) {
    // TODO: validare il valore o fare un tipo specifico.
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.EMAIL));
    return build(epasParam, office, 
        EpasParamValueType.formatValue(email), begin, end, false, persist);
  }

  /**
   * Costruttore generico di una configurazione periodica. Effettua tutti i passaggi di validazione.
   */
  private Configuration build(EpasParam epasParam, Office office, String fieldValue, 
      Optional<LocalDate> begin, Optional<LocalDate> end, boolean applyToTheEnd, boolean persist) {
    if (applyToTheEnd) {
      end = Optional.fromNullable(office.calculatedEnd());
    }
    Configuration configuration = new Configuration();
    configuration.office = office;
    configuration.fieldValue = fieldValue;
    configuration.epasParam = epasParam;
    configuration.beginDate = office.beginDate;
    if (begin.isPresent()) {
      configuration.beginDate = begin.get();
    }
    if (end.isPresent()) {
      configuration.endDate = end.get();
    }

    //Controllo sul fatto di essere un parametro generale, annuale, o periodico.
    //Decidere se rimandare un errore al chiamante.
    Verify.verify(validateTimeType(configuration));

    periodManager.updatePeriods(configuration, persist);
    return configuration;
  }

  /**
   * Valida il parametro di configurazione sulla base del suo tipo tempo.
   * @param configuration parametro
   * @return esito.
   */
  public boolean validateTimeType(Configuration configuration) {

    if (configuration.epasParam.epasParamTimeType.equals(EpasParamTimeType.GENERAL)) {
      //il parametro deve coprire tutta la durata di un owner.
      return DateUtility.areIntervalsEquals(
          new DateInterval(configuration.getBeginDate(), configuration.calculatedEnd()),
          new DateInterval(configuration.office.getBeginDate(), 
              configuration.office.calculatedEnd()));
    }

    //il parametro PERIODIC non ha vincoli, il parametro YEARLY lo costruisco opportunamente 
    // passando dal builder.
    return true;
  }

  /**
   * Data inizio anno per la sede.  
   */
  public LocalDate officeYearBegin(Office office, int year) {
    LocalDate begin = new LocalDate(year, 1, 1);
    if (office.beginDate.getYear() == year && office.beginDate.isAfter(begin)) {
      return office.beginDate;
    }
    return begin;
  }

  /**
   * Data fine anno per la sede.
   */
  public LocalDate officeYearEnd(Office office, int year) {
    LocalDate end = new LocalDate(year, 12, 31);
    if (office.calculatedEnd() != null && office.calculatedEnd().getYear() == year 
        && office.calculatedEnd().isBefore(end)) {
      return office.calculatedEnd();
    }
    return end;
  }
  
  /**
   * La lista delle configurazioni della sede per la data. <br> 
   * Se non esiste (inizializzazione parametro) applica il valore di default. 
   * @param office sede
   * @param date data
   * @return lista di configurazioni
   */
  public List<Configuration> getOfficeConfigurationsByDate(Office office, LocalDate date) {

    List<Configuration> list = Lists.newArrayList();
    for (EpasParam epasParam : EpasParam.values()) {
      list.add(getOfficeConfiguration(office, epasParam, date));
    }
    return list;
  }

  /**
   * Preleva la configurazione per la sede, il tipo e la data passata.
   * @param office
   * @param epasParam
   * @param date
   * @return
   */
  public Configuration getOfficeConfiguration(Office office, EpasParam epasParam, LocalDate date) {
    
    //TODO verificare se serve la cache .... forse no.
    for (Configuration configuration : office.configurations) {
      if (!configuration.epasParam.equals(epasParam)) {
        continue;
      }
      DateInterval interval = new DateInterval(configuration.beginDate, configuration.calculatedEnd()); 
      if (!DateUtility.isDateIntoInterval(date, interval)) {
        continue;
      }
      return configuration;
    }
    return buildDefault(office, epasParam);
  }
  
  /**
   * Costruttore della configurazione di default se non esiste.<br>
   * Verificare che venga chiamata esclusivamente nel caso di nuovo enumerato !!!
   * Di norma la configurazione di default andrebbe creata tramite migrazione o al momento
   * della creazione della sede.
   * @param office
   * @param epasParam
   * @return
   */
  private Configuration buildDefault(Office office, EpasParam epasParam) {

    return build(epasParam, office, (String)epasParam.defaultValue, 
        Optional.fromNullable(office.beginDate), Optional.<LocalDate>absent(), true, true);

  }

  /**
   * Preleva il valore del parametro alla data.
   * @param office sede
   * @param epasParam tipo parametro
   * @param date data
   * @return value
   */
  public Object configValue(Office office, EpasParam epasParam, LocalDate date) {
    Configuration configuration = getOfficeConfiguration(office, epasParam, date);
    return EpasParamValueType.parseValue(configuration.epasParam.epasParamValueType, 
        configuration.fieldValue);
  }
  
  /**
   * Preleva il valore del parametro per l'anno.
   * @param office sede
   * @param epasParam tipo parametro
   * @param year anno
   * @return value
   */
  public Object configValue(Office office, EpasParam epasParam, int year) {
    Preconditions.checkArgument(epasParam.isYearly());
    LocalDate date = new LocalDate(year, 1, 1);
    if (office.beginDate.getYear() == year) {
      date = office.beginDate;
    }
    return configValue(office, epasParam, date);
  }
  

}
