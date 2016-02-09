package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

import models.ConfGeneral;
import models.Configuration;
import models.Office;
import models.enumerate.EpasParam;
import models.enumerate.EpasParam.EpasParamTimeType;
import models.enumerate.EpasParam.EpasParamValueType;
import models.enumerate.Parameter;
import models.query.QConfGeneral;

import org.joda.time.LocalDate;
import org.joda.time.MonthDay;

import play.cache.Cache;

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
   * Costruisce la configurazione con tipo DayMonth.
   * @param epasParam
   * @param office
   * @param day
   * @param month
   * @param begin
   * @param end
   * @return
   */
  public Configuration buildDayMonth(EpasParam epasParam, Office office, int day, int month, 
      Optional<LocalDate> begin, Optional<LocalDate> end) {
    
    Preconditions.checkState(epasParam.epasParamValueType.equals(EpasParamValueType.DAY_MONTH));
    Configuration configuration = new Configuration();
    configuration.office = office;
    configuration.fieldValue = day + "-" + month;
    
    periodManager.updatePeriods(configuration, true);
    
    return configuration;
  }

  
  
 }
