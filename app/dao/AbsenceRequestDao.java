/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dao;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import lombok.val;
import models.Office;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import models.flows.AbsenceRequest;
import models.flows.Group;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.query.QAbsenceRequest;
import models.flows.query.QAffiliation;
import models.flows.query.QGroup;
import models.query.QOffice;
import models.query.QPerson;
import org.joda.time.LocalDateTime;


/**
 * Dao per l'accesso alle richiesta di assenza.
 *
 * @author Cristian Lucchesi
 */
public class AbsenceRequestDao extends DaoBase {

  @Inject
  AbsenceRequestDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Ritorna la lista delle richieste d'assenza non ancora validate che presentano date di
   * inizio/fine che in qualche modo intersecano le date di inizio/fine della richiesta d'assenza da
   * validare.
   *
   * @param request la richiesta d'assenza da far validare
   * @return la lista delle richieste di assenza che hanno parametri che non permettono la corretta
   *     creazione della richiesta d'assenza passata come parametro.
   */
  public List<AbsenceRequest> existingAbsenceRequests(AbsenceRequest request) {
    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    return getQueryFactory().selectFrom(absenceRequest)
        .where(absenceRequest.person.eq(request.getPerson())
            .and(absenceRequest.flowEnded.eq(false)))
        .fetch();
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
    return getQueryFactory().selectFrom(absenceRequest)
        .join(absenceRequest.person).fetchJoin()
        .where(conditions).orderBy(absenceRequest.startAt.desc()).fetch();
  }


  /**
   * Metodo che ritorna tutti i flussi attivi da approvare.
   *
   * @param uros la lista degli user_role_office associati alla persona pr cui si cercano le
   *     richieste da approvare.
   * @param fromDate la data da cui cercare
   * @param toDate (opzionale) la data entro cui cercare
   * @param absenceRequestType il tipo di richiesta da cercare
   * @return la lista di tutti i flussi attivi da approvare.
   */
  public Set<AbsenceRequest> toApproveResults(List<UsersRolesOffices> uros,
      Optional<LocalDateTime> fromDate, Optional<LocalDateTime> toDate,
      AbsenceRequestType absenceRequestType, List<Group> groups, Person signer) {
    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    final QPerson person = QPerson.person;
    final QGroup group = QGroup.group;

    BooleanBuilder conditions = new BooleanBuilder();

    if (uros.stream().noneMatch(uro -> uro.getRole().getName().equals(Role.GROUP_MANAGER)
        || uro.getRole().getName().equals(Role.PERSONNEL_ADMIN)
        || uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))) {
      return Sets.newHashSet();
    }
    if (fromDate.isPresent()) {
      conditions.and(absenceRequest.startAt.after(fromDate.get()));
    }
    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }   
    conditions.and(absenceRequest.type.eq(absenceRequestType)
            .and(absenceRequest.flowStarted.isTrue())
            .and(absenceRequest.flowEnded.isFalse()));

    Set<AbsenceRequest> results = Sets.newHashSet();
    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))) {
      results.addAll(
          toApproveResultsAsSeatSuperVisor(
              uros, fromDate, toDate, absenceRequestType, groups, signer));
    }
    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.GROUP_MANAGER))) {
      List<Office> officeList = uros.stream().map(u -> u.getOffice()).collect(Collectors.toList());
      conditions = managerQuery(officeList, conditions, signer);
      final QAffiliation affiliation = QAffiliation.affiliation;
      List<AbsenceRequest> queryResults = getQueryFactory().selectFrom(absenceRequest)
          .join(absenceRequest.person, person).fetchJoin()
          .join(person.affiliations, affiliation)
            .on(affiliation.beginDate.before(LocalDate.now())
                .and(affiliation.endDate.isNull().or(affiliation.endDate.after(LocalDate.now()))))
          .join(affiliation.group, group)
          .where(group.manager.eq(signer).and(conditions))
          .distinct()
          .fetch();
      results.addAll(queryResults);
    }
    return results;
  }

  /**
   * Lista delle AbsenceRequest da Approvare come responsabile di sede.
   */
  public List<AbsenceRequest> toApproveResultsAsSeatSuperVisor(List<UsersRolesOffices> uros,
      Optional<LocalDateTime> fromDate, Optional<LocalDateTime> toDate,
      AbsenceRequestType absenceRequestType, List<Group> groups, Person signer) {
    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;

    BooleanBuilder baseConditions = new BooleanBuilder();

    if (uros.stream().noneMatch(uro -> uro.getRole().getName().equals(Role.PERSONNEL_ADMIN)
        || uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))) {
      return Lists.newArrayList();
    }
    if (fromDate.isPresent()) {
      baseConditions.and(absenceRequest.startAt.after(fromDate.get()));
    }
    if (toDate.isPresent()) {
      baseConditions.and(absenceRequest.endTo.before(toDate.get()));
    }    
    baseConditions.and(absenceRequest.type.eq(absenceRequestType)
            .and(absenceRequest.flowStarted.isTrue())
            .and(absenceRequest.flowEnded.isFalse()));

    val urosSupervisor = 
        uros.stream().filter(uro -> uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))
        .collect(Collectors.toList());
    val uroOffices = urosSupervisor.stream().map(uro -> uro.getOffice())
        .collect(Collectors.toList());
    return getQueryFactory().selectFrom(absenceRequest)
        .join(absenceRequest.person).fetchJoin()
        .join(absenceRequest.events).fetchJoin()
        .where(
            baseConditions, absenceRequest.person.office.in(uroOffices), 
            seatSupervisorCondition(absenceRequest))
        .fetch();
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
  public List<AbsenceRequest> allResults(List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      AbsenceRequestType absenceRequestType, List<Group> groups, Person signer) {
    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    final QPerson person = QPerson.person;
    final QGroup group = QGroup.group;

    BooleanBuilder conditions = new BooleanBuilder();

    if (uros.stream().noneMatch(uro -> uro.getRole().getName().equals(Role.GROUP_MANAGER)
        || uro.getRole().getName().equals(Role.PERSONNEL_ADMIN)
        || uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))) {
      return Lists.newArrayList();
    }
    val results = Lists.<AbsenceRequest>newArrayList();
    List<Office> officeList = uros.stream().map(u -> u.getOffice()).collect(Collectors.toList());
    conditions.and(absenceRequest.startAt.after(fromDate))
        .and(absenceRequest.type.eq(absenceRequestType)
            .and(absenceRequest.flowStarted.isTrue())
            .and(absenceRequest.flowEnded.isFalse())
            .and(absenceRequest.person.office.in(officeList)));
    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }
    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))) {
      results.addAll(allResultsAsSuperVisor(
          uros, fromDate, toDate, absenceRequestType, groups, signer));
    }
    JPQLQuery<AbsenceRequest> query;
    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.GROUP_MANAGER))) {
      conditions.and(absenceRequest.managerApprovalRequired.isTrue())
          .and(absenceRequest.managerApproved.isNotNull())
          .and(person.office.eq(signer.getOffice()));
      final QAffiliation affiliation = QAffiliation.affiliation;
      query = getQueryFactory().selectFrom(absenceRequest)
          .join(absenceRequest.person, person).fetchJoin()
          .join(person.affiliations, affiliation)
            .on(affiliation.beginDate.before(LocalDate.now())
              .and(affiliation.endDate.isNull().or(affiliation.endDate.after(LocalDate.now()))))
          .join(affiliation.group, group)
          .where(group.manager.eq(signer).and(conditions));
    } else {
      query = getQueryFactory().selectFrom(absenceRequest).where(conditions);
    }
    results.addAll(query.fetch());
    return results;
  }

  private List<AbsenceRequest> allResultsAsSuperVisor(List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      AbsenceRequestType absenceRequestType, List<Group> groups, Person signer) {
    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    final QPerson person = QPerson.person;

    BooleanBuilder conditions = new BooleanBuilder();
    List<Office> officeList = uros.stream().map(u -> u.getOffice()).collect(Collectors.toList());
    conditions.and(absenceRequest.startAt.after(fromDate))
        .and(absenceRequest.type.eq(absenceRequestType)
            .and(absenceRequest.flowStarted.isTrue())
            .and(absenceRequest.flowEnded.isFalse())
            .and(absenceRequest.person.office.in(officeList)));
    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }
    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))) {
      conditions.and(absenceRequest.managerApprovalRequired.isTrue())
          .and(absenceRequest.officeHeadApprovalRequired.isNotNull()
              .or(absenceRequest.officeHeadApprovalForManagerRequired.isNotNull()))
          .and(person.office.in(officeList));
      return getQueryFactory().selectFrom(absenceRequest)
          .join(absenceRequest.person, person).fetchJoin()
          .where(person.office.in(
              uros.stream().map(
                  userRoleOffice -> userRoleOffice.getOffice())
                  .collect(Collectors.toSet())).and(conditions))
          .orderBy(absenceRequest.startAt.desc())
          .fetch();
    } else {
      return Lists.newArrayList();
    }
  }

  /**
   * Metodo che ritorna la lista delle richieste di assenza già approvate per ruolo data e tipo.
   *
   * @param uros la lista degli users_roles_offices
   * @param fromDate la data da cui cercare le richieste di assenza
   * @param toDate la data fino a cui cercare le richieste di assenza (opzionale)
   * @param absenceRequestType il tipo della richiesta di assenza.
   * @return la lista delle richieste totalmente approvate.
   */
  public List<AbsenceRequest> totallyApproved(List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      AbsenceRequestType absenceRequestType, List<Group> groups, Person signer) {
    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    final QPerson person = QPerson.person;
    final QGroup group = QGroup.group;

    BooleanBuilder conditions = new BooleanBuilder();
    List<AbsenceRequest> results = new ArrayList<>();
    JPQLQuery<AbsenceRequest> query;
    List<Office> officeList = 
        uros.stream().filter(uro -> !uro.getRole().getName().equals(Role.EMPLOYEE))
        .map(u -> u.getOffice()).collect(Collectors.toList());
    conditions.and(absenceRequest.startAt.after(fromDate))
        .and(absenceRequest.type.eq(absenceRequestType).and(absenceRequest.flowEnded.isTrue())
            .and(absenceRequest.person.office.in(officeList)));

    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }

    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))) {
      results
          .addAll(totallyApprovedAsSuperVisor(
              uros, fromDate, toDate, absenceRequestType, groups, signer));
    }

    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.GROUP_MANAGER))) {
      conditions.and(absenceRequest.managerApprovalRequired.isTrue())
        .and(absenceRequest.managerApproved.isNotNull())
          .and(person.office.in(officeList));
      final QAffiliation affiliation = QAffiliation.affiliation;
      query = getQueryFactory().selectFrom(absenceRequest)
          .join(absenceRequest.person, person).fetchJoin()
          .join(person.affiliations, affiliation)
          .join(affiliation.group, group)
          .where(group.manager.eq(signer).and(conditions))
          .orderBy(absenceRequest.startAt.desc());
    } else {
      query = getQueryFactory()
          .selectFrom(absenceRequest)
          .join(absenceRequest.person).fetchJoin()
          .where(conditions)
          .orderBy(absenceRequest.startAt.desc());
    }

    results.addAll(query.fetch());
    return results;
  }

  private List<AbsenceRequest> totallyApprovedAsSuperVisor(List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      AbsenceRequestType absenceRequestType, List<Group> groups, Person signer) {
    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    final QPerson person = QPerson.person;
    final QOffice office = QOffice.office;
    List<Office> officeList = uros.stream()
        .filter(uro -> uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))
        .map(u -> u.getOffice()).distinct()
        .collect(Collectors.toList());
    BooleanBuilder conditions = new BooleanBuilder();
    conditions.and(absenceRequest.startAt.after(fromDate))
        .and(absenceRequest.type.eq(absenceRequestType)
        .and(absenceRequest.flowEnded.isTrue()));

    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }
    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))) {
      conditions.and(
          absenceRequest.officeHeadApprovalRequired.isTrue()
              .or(absenceRequest.officeHeadApprovalForManagerRequired.isTrue()))
          .and(absenceRequest.officeHeadApproved.isNotNull())
          .and(person.office.in(officeList));

      return getQueryFactory().selectFrom(absenceRequest)
          .join(absenceRequest.person, person).fetchJoin()
          .join(person.office, office)
          .where(office.in(uros.stream().map(
              userRoleOffices -> userRoleOffices.getOffice())
              .collect(Collectors.toSet())).and(conditions))
          .orderBy(absenceRequest.startAt.desc())
          .fetch();
    } else {
      return Lists.newArrayList();
    }

  }

  /**
   * Ritorna le condizioni con l'aggiunta di quelle relative al responsabile di sede.
   *
   * @param officeList la lista delle sedi
   * @param condition le condizioni pregresse
   * @return le condizioni per determinare se il responsabile di sede è coinvolto nell'approvazione
   *     delle ferie.
   */
  private BooleanBuilder seatSupervisorCondition(QAbsenceRequest absenceRequest) {
    BooleanBuilder condition = new BooleanBuilder();
    condition.andAnyOf(
            //per i manager è richiesta solo l'approvazione da parte del responsabile di sede
            absenceRequest.officeHeadApprovalForManagerRequired.isTrue()
            .and(absenceRequest.officeHeadApproved.isNull()),
            
            //questo è il caso della doppia approvazione in cui il manager ha già approvato 
            absenceRequest.managerApprovalRequired.isTrue()
              .and(absenceRequest.managerApproved.isNotNull())
              .and(absenceRequest.officeHeadApprovalRequired.isTrue())
              .and(absenceRequest.officeHeadApproved.isNull()),

            //questo è il caso in cui è necessaria solo l'approvazione del responsabile di sede
            absenceRequest.officeHeadApprovalRequired.isTrue()
                .and(absenceRequest.officeHeadApproved.isNull())
                .and(absenceRequest.managerApprovalRequired.isFalse()));
    return condition;
  }


  /**
   * Ritorna le condizioni di firma delle richieste da parte del responsabile di gruppo.
   *
   * @param officeList la lista delle sedi
   * @param condition le condizioni precedenti
   * @param signer il firmatario della richiesta
   * @return le condizioni di firma delle richieste da parte del responsabile di gruppo.
   */
  private BooleanBuilder managerQuery(List<Office> officeList, 
      BooleanBuilder condition, Person signer) {
    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    condition.and(absenceRequest.managerApprovalRequired.isTrue())
        .and(absenceRequest.managerApproved.isNull())
        .and(absenceRequest.person.office.in(officeList));
    return condition;

  }

}