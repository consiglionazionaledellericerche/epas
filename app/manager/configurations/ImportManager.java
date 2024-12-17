package manager.configurations;

import cnr.sync.dto.v3.ConfigurationOfficeDto;
import com.google.common.base.Verify;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import dao.ConfigurationDao;
import dao.OfficeDao;
import dao.PersonDao;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.PeriodManager;
import manager.configurations.EpasParam.EpasParamValueType;
import manager.configurations.EpasParam.EpasParamValueType.IpList;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;
import manager.recaps.recomputation.RecomputeRecap;
import models.Configuration;
import models.Office;
import models.base.IPropertyInPeriod;
import models.enumerate.BlockType;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import play.jobs.Job;
import play.libs.F.Promise;

@Slf4j
public class ImportManager {

  protected final JPQLQueryFactory queryFactory;
  private final PeriodManager periodManager;
  private final ConsistencyManager consistencyManager;
  private final ConfigurationManager configurationManager;

  /**
   * Default constructor per l'injection.
   */
  @Inject
  ImportManager(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      PeriodManager periodManager, OfficeDao officeDao, PersonDao personDao,
      ConfigurationManager configurationManager, ConsistencyManager consistencyManager) {
    this.queryFactory = new JPAQueryFactory(emp);
    this.periodManager = periodManager;
    this.configurationManager = configurationManager;
    this.consistencyManager = consistencyManager;
  }

  public void importConfig(ConfigurationOfficeDto dto, Long officeId) {

    Office office = Office.findById(officeId);
    Configuration newConfiguration = null;
    EpasParam epasParam = dto.getEpasParam();
    log.debug("Parametro da cambiare: {}", epasParam.name);
    java.util.Optional<Configuration> configuration = configurationManager.getConfigurationByOfficeAndType(office, epasParam);

    LocalDate beginNew = LocalDate.parse(dto.getBeginDate());
    LocalDate toUse = null;
    if (beginNew.isBefore(configuration.get().getBeginDate())) {
      toUse = configuration.get().getBeginDate();
    } else {
      toUse = beginNew;
    }
    //configuration.get().merge();
    log.debug("Data di partenza: {}", toUse.toString());
    log.debug("Valore: '{}'", dto.getFieldValue());
    switch(epasParam.epasParamValueType) {
      case BOOLEAN:
        newConfiguration = (Configuration) configurationManager.updateBoolean(epasParam,
            office, Boolean.getBoolean(dto.getFieldValue()),
            com.google.common.base.Optional.fromNullable(toUse),
            dto.getEndDate() != null ? com.google.common.base.Optional.fromNullable(LocalDate.parse(dto.getEndDate())) 
                : com.google.common.base.Optional.absent(), false);
        break;
      case DAY_MONTH:
        MonthDay dayMonth = (MonthDay) EpasParamValueType
        .parseValue(EpasParamValueType.DAY_MONTH, dto.getFieldValue());
        newConfiguration = (Configuration) configurationManager.updateDayMonth(epasParam,
            office, dayMonth.getDayOfMonth(), dayMonth.getMonthOfYear(),
            com.google.common.base.Optional.fromNullable(toUse),
            dto.getEndDate() != null ? com.google.common.base.Optional.fromNullable(LocalDate.parse(dto.getEndDate())) 
                : com.google.common.base.Optional.absent(), false);
        break;
      case EMAIL:
        newConfiguration = (Configuration) configurationManager.updateEmail(epasParam,
            office, dto.getFieldValue(),
            com.google.common.base.Optional.fromNullable(toUse),
            dto.getEndDate() != null ? com.google.common.base.Optional.fromNullable(LocalDate.parse(dto.getEndDate())) 
                : com.google.common.base.Optional.absent(), false);
        break;
      case ENUM:
        newConfiguration = (Configuration) configurationManager.updateEnum(epasParam,
            office, BlockType.valueOf(dto.getFieldValue()),
            com.google.common.base.Optional.fromNullable(toUse),
            dto.getEndDate() != null ? com.google.common.base.Optional.fromNullable(LocalDate.parse(dto.getEndDate())) 
                : com.google.common.base.Optional.absent(), false);
        break;
      case INTEGER:
        newConfiguration = (Configuration) configurationManager.updateInteger(epasParam,
            office, Integer.parseInt(dto.getFieldValue()),
            com.google.common.base.Optional.fromNullable(toUse),
            dto.getEndDate() != null ? com.google.common.base.Optional.fromNullable(LocalDate.parse(dto.getEndDate())) 
                : com.google.common.base.Optional.absent(), false);
        break;
      case IP_LIST:
        IpList ipList = (IpList) EpasParamValueType.parseValue(
            epasParam.epasParamValueType, dto.getFieldValue());
        newConfiguration = (Configuration) configurationManager.updateIpList(epasParam,
            office, ipList.ipList,
            com.google.common.base.Optional.fromNullable(toUse),
            dto.getEndDate() != null ? com.google.common.base.Optional.fromNullable(LocalDate.parse(dto.getEndDate())) 
                : com.google.common.base.Optional.absent(), false);
        break;
      case LOCALDATE:
        newConfiguration = (Configuration) configurationManager.updateLocalDate(epasParam,
            office, LocalDate.parse(dto.getFieldValue()),
            com.google.common.base.Optional.fromNullable(toUse),
            dto.getEndDate() != null ? com.google.common.base.Optional.fromNullable(LocalDate.parse(dto.getEndDate())) 
                : com.google.common.base.Optional.absent(), false);
        break;
      case LOCALTIME:
        LocalTime localtime = (LocalTime) EpasParamValueType
        .parseValue(EpasParamValueType.LOCALTIME, dto.getFieldValue());
        newConfiguration = (Configuration) configurationManager.updateLocalTime(epasParam,
            office, localtime,
            com.google.common.base.Optional.fromNullable(toUse),
            dto.getEndDate() != null ? com.google.common.base.Optional.fromNullable(LocalDate.parse(dto.getEndDate())) 
                : com.google.common.base.Optional.absent(), false);
        break;
      case LOCALTIME_INTERVAL:
        LocalTimeInterval localtimeInterval = (LocalTimeInterval) EpasParamValueType
        .parseValue(EpasParamValueType.LOCALTIME_INTERVAL, dto.getFieldValue());
        newConfiguration = (Configuration) configurationManager.updateLocalTimeInterval(epasParam,
            office, localtimeInterval.from, localtimeInterval.to,
            com.google.common.base.Optional.fromNullable(toUse),
            dto.getEndDate() != null ? com.google.common.base.Optional.fromNullable(LocalDate.parse(dto.getEndDate())) 
                : com.google.common.base.Optional.absent(), false);
        break;
      case MONTH:
        newConfiguration = (Configuration) configurationManager.updateMonth(epasParam,
            office, Integer.parseInt(dto.getFieldValue()),
            com.google.common.base.Optional.fromNullable(toUse),
            dto.getEndDate() != null ? com.google.common.base.Optional.fromNullable(LocalDate.parse(dto.getEndDate())) 
                : com.google.common.base.Optional.absent(), false);
        break;
      default:
        break;
    }

    if(configuration.get().getEpasParam().epasParamValueType.equals(EpasParamValueType.BOOLEAN)) {
      log.debug("Proviamo a non fare ricalcoli per i parametri booleani");
      return;
    }
    Verify.verifyNotNull(newConfiguration);

    List<IPropertyInPeriod> periodRecaps = periodManager
        .updatePeriods(newConfiguration, false);
    RecomputeRecap recomputeRecap =
        periodManager.buildRecap(office.getBeginDate(),
            com.google.common.base.Optional.fromNullable(LocalDate.now()),
            periodRecaps, com.google.common.base.Optional.<LocalDate>absent());
    recomputeRecap.epasParam = newConfiguration.getEpasParam();

    periodRecaps =  periodManager.updatePeriods(newConfiguration, true);

    consistencyManager.performRecomputation(office,
        newConfiguration.getEpasParam().recomputationTypes, recomputeRecap.recomputeFrom);

  }
}
