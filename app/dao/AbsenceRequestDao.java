package dao;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import helpers.jpa.ModelQuery;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Person;
import models.Role;
import models.flows.AbsenceRequest;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.query.QAbsenceRequest;
import org.joda.time.LocalDateTime;

/**
 * Dao per l'accesso alle richiesta di assenza.
 * 
 * @author cristian
 *
 */
public class AbsenceRequestDao extends DaoBase {

  @Inject
  AbsenceRequestDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Lista delle richiesta di assenza per persona e data.
   * 
   * @param person La persona della quale recuperare le richieste di assenza
   * @param fromDate La data iniziale dell'intervallo temporale da considerare
   * @param toDate La data finale dell'intervallo temporale da considerare (opzionale)
   * @param absenceRequestType Il tipo di richiesta di assenza specifico
   * @return La lista delle richieste di assenze sull'intervallo e la persona specificati.
   */
  public ModelQuery.SimpleResults<AbsenceRequest> findByPersonAndDate(Person person,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate, 
      AbsenceRequestType absenceRequestType) {

    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;

    BooleanBuilder conditions = new BooleanBuilder(absenceRequest.person.eq(person)
        .and(absenceRequest.startAt.after(fromDate))
        .and(absenceRequest.type.eq(absenceRequestType)));
    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }
    return ModelQuery.wrap(getQueryFactory().from(absenceRequest)
          .where(conditions), absenceRequest);
  }
 
  /**
   * Lista di richieste da approvare per ruolo, data e tipo.
   * 
   * @param role il ruolo per cui si cercano le richieste
   * @param fromDate la data da cui cercare
   * @param toDate (opzionale) la data entro cui cercare
   * @param absenceRequestType il tipo di richiesta da cercare
   * @return La lista delle richieste di assenza da approvare per il ruolo passato. 
   */
  public ModelQuery.SimpleResults<AbsenceRequest> findRequestsToApprove(Role role,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate, 
      AbsenceRequestType absenceRequestType) {

    Preconditions.checkNotNull(role);
    Preconditions.checkNotNull(fromDate);
    
    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    BooleanBuilder conditions = new BooleanBuilder(absenceRequest.startAt.after(fromDate))
        .and(absenceRequest.type.eq(absenceRequestType));
    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }
    JPQLQuery query = null;
    if (role.name.equals(Role.PERSONNEL_ADMIN)) {
      query = personnelAdminQuery();
    } else if (role.name.equals(Role.SEAT_SUPERVISOR)) {
      query = seatSupervisorQuery();
    } else {
      query = managerQuery();
    }
    
    return ModelQuery.wrap(query.where(conditions), absenceRequest);
  }
  
  private JPQLQuery seatSupervisorQuery() {
    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    final JPQLQuery query = getQueryFactory().from(absenceRequest)
        .where(absenceRequest.officeHeadApprovalRequired.isTrue()
            .and(absenceRequest.officeHeadApproved.isNull()
                .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    return query;
  }
  
  private JPQLQuery personnelAdminQuery() {
    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    final JPQLQuery query = getQueryFactory().from(absenceRequest)
        .where(absenceRequest.administrativeApprovalRequired.isTrue()
            .and(absenceRequest.administrativeApproved.isNull()
                .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    return query;
  }
  
  private JPQLQuery managerQuery() {
    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    final JPQLQuery query = getQueryFactory().from(absenceRequest)
        .where(absenceRequest.managerApprovalRequired.isTrue()
            .and(absenceRequest.managerApproved.isNull()
                .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    return query;
  }
}
