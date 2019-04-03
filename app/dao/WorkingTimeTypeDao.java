package dao;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.query.QWorkingTimeType;
import org.joda.time.LocalDate;

/**
 * Dao per i WorkingTimeType.
 *
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

  /**
   * Se office è present il tipo orario di con quella descrizione se esiste. Se office non è present
   * il tipo orario di default con quella descrizione.
   */
  public WorkingTimeType workingTypeTypeByDescription(String description,
      Optional<Office> office) {

    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    final BooleanBuilder condition = new BooleanBuilder();

    if (office.isPresent()) {
      condition.and(wtt.description.eq(description).and(wtt.office.eq(office.get())));
    } else {
      condition.and(wtt.description.eq(description).and(wtt.office.isNull()));
    }

    return getQueryFactory().selectFrom(wtt).where(condition).fetchOne();

  }


  /**
   * Tutti gli orari.
   */
  public List<WorkingTimeType> getAllWorkingTimeType() {
    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    return getQueryFactory().selectFrom(wtt).fetch();
  }

  /**
   * Tutti gli orari di lavoro default e quelli speciali dell'office.
   */
  public List<WorkingTimeType> getEnabledWorkingTimeTypeForOffice(Office office) {

    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    return getQueryFactory()
        .selectFrom(wtt)
        .where(wtt.office.isNull().or(wtt.office.eq(office).and(wtt.disabled.eq(false)))).fetch();
  }

  /**
   * WorkingTimeType by id.
   *
   * @param id id
   * @return wtt
   */
  public WorkingTimeType getWorkingTimeTypeById(Long id) {
    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    return getQueryFactory().selectFrom(wtt)
        .where(wtt.id.eq(id)).fetchOne();
  }


  /**
   * @return la lista degli orari di lavoro presenti di default sul database.
   */
  public List<WorkingTimeType> getDefaultWorkingTimeType() {
    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    return getQueryFactory().selectFrom(wtt)
        .where(wtt.office.isNull()).orderBy(wtt.description.asc()).fetch();
  }

  /**
   * @return il tipo di orario di lavoro utilizzato in date.
   */

  /**
   * Il tipo orario per la persona attivo nel giorno.
   *
   * @param date data
   * @param person persona
   * @return il tipo orario se presente
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
   * Il tipo orario del giorno per la persona.
   *
   * @param date data
   * @param person persona
   * @return il tipo orario del giorno se presente
   */
  public Optional<WorkingTimeTypeDay> getWorkingTimeTypeDay(LocalDate date, Person person) {
    Optional<WorkingTimeType> wtt = getWorkingTimeType(date, person);
    if (!wtt.isPresent()) {
      return Optional.absent();
    }
    int index = date.getDayOfWeek() - 1;
    Verify.verify(index < wtt.get().workingTimeTypeDays.size(),
        String.format("Definiti %d giorni nel WorkingTimeType %s, "
                + "richiesto giorno non presente con indice %d",
            wtt.get().workingTimeTypeDays.size(), wtt.get(), index));

    Optional<WorkingTimeTypeDay> wttd =
        Optional.fromNullable(wtt.get().workingTimeTypeDays.get(index));

    Verify.verify(wttd.isPresent());
    Verify.verify(wttd.get().dayOfWeek == date.getDayOfWeek());

    return wttd;
  }

}
