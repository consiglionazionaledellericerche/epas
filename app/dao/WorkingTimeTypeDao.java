package dao;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.types.expr.BooleanExpression;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.query.QWorkingTimeType;

import org.joda.time.LocalDate;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

/**
 * Dao per i WorkingTimeType.
 * @author dario
 */
public class WorkingTimeTypeDao extends DaoBase {

  private final ContractDao contractDao;

  @Inject
  WorkingTimeTypeDao(JPQLQueryFactory queryFactory,
                     Provider<EntityManager> emp, ContractDao contractDao) {
    super(queryFactory, emp);
    this.contractDao = contractDao;
  }

  @Deprecated
  public WorkingTimeType getWorkingTimeTypeByDescription(String description) {
    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    final JPQLQuery query = getQueryFactory().from(wtt)
            .where(wtt.description.eq(description));
    return query.singleResult(wtt);
  }

  /**
   * Se office è present il tipo orario di con quella descrizione se esiste. Se office non è present
   * il tipo orario di default con quella descrizione.
   */
  public WorkingTimeType workingTypeTypeByDescription(String description,
                                                      Optional<Office> office) {

    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;

    if (office.isPresent()) {
      return getQueryFactory().from(wtt)
              .where(wtt.description.eq(description).and(wtt.office.eq(office.get())))
              .singleResult(wtt);
    } else {
      return getQueryFactory().from(wtt)
              .where(wtt.description.eq(description).and(wtt.office.isNull()))
              .singleResult(wtt);
    }

  }


  /**
   * Tutti gli orari.
   */
  public List<WorkingTimeType> getAllWorkingTimeType() {
    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    final JPQLQuery query = getQueryFactory().from(wtt);
    return query.list(wtt);
  }

  /**
   * Tutti gli orari di lavoro default e quelli speciali dell'office.
   */
  public List<WorkingTimeType> getEnabledWorkingTimeTypeForOffice(Office office) {

    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    final JPQLQuery query = getQueryFactory()
            .from(wtt)
            .where(wtt.office.isNull().or(
                BooleanExpression.allOf(wtt.office.eq(office).and(wtt.disabled.eq(false)))));
    return query.list(wtt);
  }

  public WorkingTimeType getWorkingTimeTypeById(Long id) {
    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    final JPQLQuery query = getQueryFactory().from(wtt)
            .where(wtt.id.eq(id));
    return query.singleResult(wtt);
  }


  /**
   * @return la lista degli orari di lavoro presenti di default sul database.
   */
  public List<WorkingTimeType> getDefaultWorkingTimeType() {
    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    final JPQLQuery query = getQueryFactory().from(wtt)
            .where(wtt.office.isNull()).orderBy(wtt.description.asc());
    return query.list(wtt);
  }

  /**
   * @return il tipo di orario di lavoro utilizzato in date.
   */
  public Optional<WorkingTimeType> getWorkingTimeType(LocalDate date, Person person) {

    Contract contract = contractDao.getContract(date, person);

    if (contract != null) {
      for (ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {

        if (DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate))) {
          return Optional.of(cwtt.workingTimeType);
        }
      }
    }
    return Optional.absent();
  }
  
  /**
   * Se per il tipo orario la data è un giorno festivo.
   * @param date
   * @param workingTimeType
   * @return
   */
  public boolean isWorkingTypeTypeHoliday(LocalDate date, WorkingTimeType workingTimeType) {
    int dayOfWeekIndex = date.getDayOfWeek() - 1;
    WorkingTimeTypeDay wttd = workingTimeType.workingTimeTypeDays.get(dayOfWeekIndex);
    Preconditions.checkState(wttd.dayOfWeek == date.getDayOfWeek());
    return wttd.holiday;
  }

}
