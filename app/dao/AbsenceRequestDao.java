package dao;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import helpers.jpa.ModelQuery;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
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
   * Ritorna la lista delle richieste d'assenza non ancora validate che presentano date di 
   *     inizio/fine che in qualche modo intersecano le date di inizio/fine della richiesta 
   *     d'assenza da validare.
   * @param request la richiesta d'assenza da far validare
   * @return la lista delle richieste di assenza che hanno parametri che non permettono la corretta 
   *     creazione della richiesta d'assenza passata come parametro.
   */
  public List<AbsenceRequest> existingAbsenceRequests(AbsenceRequest request) {
    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    JPQLQuery query = getQueryFactory().from(absenceRequest)
        .where(absenceRequest.person.eq(request.person)
            .and(absenceRequest.flowEnded.eq(false))
            );
    return query.list(absenceRequest);
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
  public List<AbsenceRequest> findByPersonAndDate(Person person,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate, 
      AbsenceRequestType absenceRequestType, boolean active) {

    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;

    BooleanBuilder conditions = new BooleanBuilder(absenceRequest.person.eq(person)
        .and(absenceRequest.startAt.after(fromDate))
        .and(absenceRequest.type.eq(absenceRequestType)));
    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }
    if (active) {
      conditions.and(absenceRequest.flowEnded.eq(false));
    } else {
      conditions.and(absenceRequest.flowEnded.eq(true));
    }
    JPQLQuery query = getQueryFactory().from(absenceRequest)
        .where(conditions);
    return query.list(absenceRequest);
  }

  /**
   * Lista di richieste da approvare per ruolo, data e tipo.
   * 
   * @param uros la lista degli user_role_office associati alla persona pr cui si cercano le 
   *     richieste da approvare.
   * @param fromDate la data da cui cercare
   * @param toDate (opzionale) la data entro cui cercare
   * @param absenceRequestType il tipo di richiesta da cercare
   * @return La lista delle richieste di assenza da approvare per il ruolo passato. 
   */
  public List<AbsenceRequest> findRequestsToApprove(
      List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate, 
      AbsenceRequestType absenceRequestType) {

    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;

    BooleanBuilder conditions = new BooleanBuilder();

    if (uros.stream().noneMatch(uro -> uro.role.name.equals(Role.GROUP_MANAGER) 
        || uro.role.name.equals(Role.PERSONNEL_ADMIN)
        || uro.role.name.equals(Role.SEAT_SUPERVISOR))) {
      return Lists.newArrayList();
    }

    for (UsersRolesOffices uro : uros) {
      if (uro.role.name.equals(Role.PERSONNEL_ADMIN)) {
        personnelAdminQuery(conditions);
      } else if (uro.role.name.equals(Role.SEAT_SUPERVISOR)) {
        seatSupervisorQuery(conditions);
      } else if (uro.role.name.equals(Role.GROUP_MANAGER)) {
        managerQuery(conditions);
      } 
    }
    conditions.and(absenceRequest.startAt.after(fromDate))
      .and(absenceRequest.type.eq(absenceRequestType));
    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }
    JPQLQuery query = getQueryFactory()
        .from(absenceRequest).where(conditions);
    return query.list(absenceRequest);
  }

  /**
   * Metodo che ritorna tutti i flussi attivi.
   * @param uros la lista degli user_role_office associati alla persona pr cui si cercano le 
   *     richieste da approvare.
   * @param fromDate la data da cui cercare
   * @param toDate (opzionale) la data entro cui cercare
   * @param absenceRequestType il tipo di richiesta da cercare
   * @return la lista di tutti i flussi attivi indipendentemente dallo stato di approvazione.
   */
  public List<AbsenceRequest> allResults(List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate, 
      AbsenceRequestType absenceRequestType) {
    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;

    BooleanBuilder conditions = new BooleanBuilder();
    if (uros.stream().noneMatch(uro -> uro.role.name.equals(Role.GROUP_MANAGER) 
        || uro.role.name.equals(Role.PERSONNEL_ADMIN)
        || uro.role.name.equals(Role.SEAT_SUPERVISOR))) {
      return Lists.newArrayList();
    }
    
    conditions.and(absenceRequest.startAt.after(fromDate))
        .and(absenceRequest.type.eq(absenceRequestType)
          .and(absenceRequest.flowStarted.isTrue()).and(absenceRequest.flowEnded.isFalse()));
    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }
    JPQLQuery query = getQueryFactory()
        .from(absenceRequest).where(conditions);
    return query.list(absenceRequest);
  }

  /**
   * Metodo che ritorna la lista delle richieste di assenza già approvate per ruolo data e tipo.
   * @param uros la lista degli users_roles_offices
   * @param fromDate la data da cui cercare le richieste di assenza
   * @param toDate la data fino a cui cercare le richieste di assenza (opzionale)
   * @param absenceRequestType il tipo della richiesta di assenza.
   * @return la lista delle richieste di assenza approvate.
   */
  public List<AbsenceRequest> findRequestsApproved(
      List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate, 
      AbsenceRequestType absenceRequestType) {

    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;

    BooleanBuilder conditions = new BooleanBuilder();

    for (UsersRolesOffices uro : uros) {
      if (uro.role.name.equals(Role.PERSONNEL_ADMIN)) {
        personnelAdminQueryApproved(conditions);
      } else if (uro.role.name.equals(Role.SEAT_SUPERVISOR)) {
        seatSupervisorQueryApproved(conditions);
      } else if (uro.role.name.equals(Role.GROUP_MANAGER)) {
        managerQueryApproved(conditions);
      }
    }
    conditions.and(absenceRequest.startAt.after(fromDate))
      .and(absenceRequest.type.eq(absenceRequestType));
    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }
    JPQLQuery query = getQueryFactory()
        .from(absenceRequest).where(conditions);
    return query.list(absenceRequest);
  }

  /**
   * Metodo che ritorna la lista delle richieste di assenza già approvate per ruolo data e tipo.
   * @param uros la lista degli users_roles_offices
   * @param fromDate la data da cui cercare le richieste di assenza
   * @param toDate la data fino a cui cercare le richieste di assenza (opzionale)
   * @param absenceRequestType il tipo della richiesta di assenza.
   * @return la lista delle richieste totalmente approvate.
   */
  public List<AbsenceRequest> totallyApproved(List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate, 
      AbsenceRequestType absenceRequestType) {
    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    
    BooleanBuilder conditions = new BooleanBuilder();
    
    conditions.and(absenceRequest.startAt.after(fromDate))
      .and(absenceRequest.type.eq(absenceRequestType).and(absenceRequest.flowEnded.isTrue()));
    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }
    JPQLQuery query = getQueryFactory()
        .from(absenceRequest).where(conditions);
    return query.list(absenceRequest);
  }

  private void seatSupervisorQuery(BooleanBuilder condition) {

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    if (condition.hasValue()) {

      condition.or(condition.and(absenceRequest.officeHeadApprovalRequired.isTrue()
          .and(absenceRequest.officeHeadApproved.isNull()
              .and(absenceRequest.flowStarted.isTrue()
                  .and(absenceRequest.flowEnded.isFalse())))));
    } else {
      condition.and(absenceRequest.officeHeadApprovalRequired.isTrue()
          .and(absenceRequest.officeHeadApproved.isNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    }      


  }

  private void personnelAdminQuery(BooleanBuilder condition) {

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    if (condition.hasValue()) {
      condition.or(absenceRequest.administrativeApprovalRequired.isTrue()
          .and(absenceRequest.administrativeApproved.isNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    } else {
      condition.and(absenceRequest.administrativeApprovalRequired.isTrue()
          .and(absenceRequest.administrativeApproved.isNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    }

  }

  private void managerQuery(BooleanBuilder condition) {

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    if (condition.hasValue()) {
      condition.or(absenceRequest.managerApprovalRequired.isTrue()
          .and(absenceRequest.managerApproved.isNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    } else {
      condition.and(absenceRequest.managerApprovalRequired.isTrue()
          .and(absenceRequest.managerApproved.isNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    }

  }


  private void seatSupervisorQueryApproved(BooleanBuilder condition) {

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    if (condition.hasValue()) {
      condition.or(condition.and(absenceRequest.officeHeadApprovalRequired.isTrue()
          .and(absenceRequest.officeHeadApproved.isNotNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isTrue())))));
    } else {
      condition.and(absenceRequest.officeHeadApprovalRequired.isTrue()
          .and(absenceRequest.officeHeadApproved.isNotNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isTrue()))));
    }

  }

  private void personnelAdminQueryApproved(BooleanBuilder condition) {

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    if (condition.hasValue()) {
      condition.or(absenceRequest.administrativeApprovalRequired.isTrue()
          .and(absenceRequest.administrativeApproved.isNotNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    } else {
      condition.and(absenceRequest.administrativeApprovalRequired.isTrue()
          .and(absenceRequest.administrativeApproved.isNotNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    }

  }

  private void managerQueryApproved(BooleanBuilder condition) {

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    if (condition.hasValue()) {
      condition.or(absenceRequest.managerApprovalRequired.isTrue()
          .and(absenceRequest.managerApproved.isNotNull()
              .and(absenceRequest.flowStarted.isTrue())));
    } else {
      condition.and(absenceRequest.managerApprovalRequired.isTrue()
          .and(absenceRequest.managerApproved.isNotNull()
              .and(absenceRequest.flowStarted.isTrue())));
    }

  }
}
