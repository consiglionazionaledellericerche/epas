package dao;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import org.joda.time.LocalDateTime;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import lombok.val;
import models.Office;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import models.flows.AbsenceRequest;
import models.flows.CompetenceRequest;
import models.flows.Group;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.enumerate.CompetenceRequestType;
import models.flows.query.QAbsenceRequest;
import models.flows.query.QCompetenceRequest;
import models.flows.query.QGroup;
import models.query.QPerson;

public class CompetenceRequestDao extends DaoBase {

  @Inject
  CompetenceRequestDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
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
  public List<CompetenceRequest> findByPersonAndDate(Person person,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      CompetenceRequestType competenceRequestType, boolean active) {

    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(fromDate);

    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;

    BooleanBuilder conditions = new BooleanBuilder(competenceRequest.person.eq(person)
        .and(competenceRequest.startAt.after(fromDate))
        .and(competenceRequest.type.eq(competenceRequestType)));
    if (toDate.isPresent()) {
      conditions.and(competenceRequest.endTo.before(toDate.get()));
    }
    if (active) {
      conditions.and(competenceRequest.flowEnded.eq(false));
    } else {
      conditions.and(competenceRequest.flowEnded.eq(true));
    }
    return getQueryFactory().selectFrom(competenceRequest)
        .where(conditions).orderBy(competenceRequest.startAt.desc()).fetch();
  }
  
  /**
   * Metodo che ritorna la lista di tutte le richieste di assenza attive.
   *
   * @param uros la lista dei ruoli della persona che deve approvare
   * @param fromDate da quando cercare le richieste
   * @param toDate a quando cercare le richieste (opzionale)
   * @param absenceRequestType il tipo della richiesta
   * @param groups la lista dei gruppi di cui fa parte il dipendente che fa la richiesta
   * @param signer il dipendente deputato all'approvazione
   * @return la lista di tutte le richieste di assenza attive.
   */
  public List<CompetenceRequest> allResults(List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      CompetenceRequestType competenceRequestType, List<Group> groups, Person signer) {
    Preconditions.checkNotNull(fromDate);

    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;
    final QPerson person = QPerson.person;
    final QGroup group = QGroup.group;

    BooleanBuilder conditions = new BooleanBuilder();

    if (uros.stream().noneMatch(uro -> uro.role.name.equals(Role.GROUP_MANAGER)
        || uro.role.name.equals(Role.PERSONNEL_ADMIN)
        || uro.role.name.equals(Role.SEAT_SUPERVISOR))) {
      return Lists.newArrayList();
    }
    val results = Lists.<CompetenceRequest>newArrayList();
    List<Office> officeList = uros.stream().map(u -> u.office).collect(Collectors.toList());
    conditions.and(competenceRequest.startAt.after(fromDate))
        .and(competenceRequest.type.eq(competenceRequestType)
            .and(competenceRequest.flowStarted.isTrue())
            .and(competenceRequest.flowEnded.isFalse())
            .and(competenceRequest.person.office.in(officeList)));
    if (toDate.isPresent()) {
      conditions.and(competenceRequest.endTo.before(toDate.get()));
    }
    if (uros.stream().anyMatch(uro -> uro.role.name.equals(Role.SEAT_SUPERVISOR))) {
      results.addAll(allResultsAsSuperVisor(
          uros, fromDate, toDate, competenceRequestType, groups, signer));
    }
    JPQLQuery<CompetenceRequest> query;
    if (uros.stream().anyMatch(uro -> uro.role.name.equals(Role.GROUP_MANAGER))) {
      conditions.and(competenceRequest.managerApprovalRequired.isTrue())
          .and(competenceRequest.managerApproved.isNotNull())
          .and(person.office.eq(signer.office));
      query = getQueryFactory().selectFrom(competenceRequest)
          .join(competenceRequest.person, person)
          .join(person.groups, group)
          .where(group.manager.eq(signer).and(conditions));
    } else {
      query = getQueryFactory().selectFrom(competenceRequest).where(conditions);
    }
    results.addAll(query.fetch());
    return results;
  }
  
  private List<CompetenceRequest> allResultsAsSuperVisor(List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      CompetenceRequestType competenceRequestType, List<Group> groups, Person signer) {
    Preconditions.checkNotNull(fromDate);

    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;
    final QPerson person = QPerson.person;

    BooleanBuilder conditions = new BooleanBuilder();
    List<Office> officeList = uros.stream().map(u -> u.office).collect(Collectors.toList());
    conditions.and(competenceRequest.startAt.after(fromDate))
        .and(competenceRequest.type.eq(competenceRequestType)
            .and(competenceRequest.flowStarted.isTrue())
            .and(competenceRequest.flowEnded.isFalse())
            .and(competenceRequest.person.office.in(officeList)));
    if (toDate.isPresent()) {
      conditions.and(competenceRequest.endTo.before(toDate.get()));
    }
    if (uros.stream().anyMatch(uro -> uro.role.name.equals(Role.SEAT_SUPERVISOR))) {
      conditions.and(competenceRequest.managerApprovalRequired.isTrue())
          .and(competenceRequest.officeHeadApprovalRequired.isNotNull())
          .and(person.office.in(officeList));
      return getQueryFactory().selectFrom(competenceRequest)
          .join(competenceRequest.person, person)
          .where(person.office.in(
              uros.stream().map(
                  userRoleOffice -> userRoleOffice.office)
                  .collect(Collectors.toSet())).and(conditions))
          .fetch();
    } else {
      return Lists.newArrayList();
    }
  }

}
