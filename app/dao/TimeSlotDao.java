package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import javax.persistence.EntityManager;
import models.Contract;
import models.ContractMandatoryTimeSlot;
import models.Office;
import models.Person;
import models.TimeSlot;
import models.query.QTimeSlot;
import org.joda.time.LocalDate;

public class TimeSlotDao extends DaoBase {

  private ContractDao contractDao;
  
  @Inject
  TimeSlotDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp, ContractDao contractDao) {
    super(queryFactory, emp);
    this.contractDao = contractDao;
  }

  /**
   * Il timeslot con id passato come parametro.
   * @param id l'id del timeslot
   * @return il timeSlot, se esiste, con id passato come parametro.
   */
  public Optional<TimeSlot> byId(long id) {
    final QTimeSlot ts = QTimeSlot.timeSlot;
    return Optional.fromNullable(getQueryFactory()
        .selectFrom(ts)
        .where(ts.id.eq(id)).fetchOne());
  }
  
  /**
   * Tutte le fasce di orario attive predefinite (non associate a nessun ufficio).
   */
  public List<TimeSlot> getPredefinedEnabledTimeSlots() {

    final QTimeSlot ts = QTimeSlot.timeSlot;
    return getQueryFactory()
        .selectFrom(ts)
        .where(ts.disabled.eq(false).and(ts.office.isNull())).fetch();
  }

  
  /**
   * Tutti le fasce di orario associate all'office.
   */
  public List<TimeSlot> getEnabledTimeSlotsForOffice(Office office) {

    final QTimeSlot ts = QTimeSlot.timeSlot;
    return getQueryFactory()
        .selectFrom(ts)
        .where(ts.office.isNull().or(ts.office.eq(office))
            .and(ts.disabled.eq(false))).fetch();
  }
  
  /**
   * L'eventuale fascia oraria di presenza obbligatoria per la persona attiva
   * nel giorno.
   *
   * @param date data
   * @param person persona
   * @return la fascia oraria obbligatoria se presente
   */
  public Optional<TimeSlot> getMandatoryTimeSlot(LocalDate date, Person person) {

    Contract contract = contractDao.getContract(date, person);

    if (contract != null) {
      for (ContractMandatoryTimeSlot mts : contract.contractMandatoryTimeSlots) {

        if (DateUtility.isDateIntoInterval(date, new DateInterval(mts.beginDate, mts.endDate))) {
          return Optional.of(mts.timeSlot);
        }
      }
    }
    return Optional.absent();
  }
}
