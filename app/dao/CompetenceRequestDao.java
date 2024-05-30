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
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import lombok.val;
import manager.flows.CompetenceRequestManager;
import manager.flows.CompetenceRequestManager.CompetenceRequestConfiguration;
import models.Office;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import models.flows.AbsenceRequest;
import models.flows.CompetenceRequest;
import models.flows.enumerate.CompetenceRequestType;
import models.flows.query.QAffiliation;
import models.flows.query.QCompetenceRequest;
import models.flows.query.QGroup;
import models.query.QPerson;
import models.query.QPersonReperibility;
import org.joda.time.LocalDateTime;

/**
 * DAO per le CompetenceRequest.
 */
public class CompetenceRequestDao extends DaoBase {

  @Inject
  CompetenceRequestDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);    
  }

  /**
   * Lista delle richiesta di assenza per persona e data.
   *
   * @param person La persona della quale recuperare le richieste di competenza
   * @param fromDate La data iniziale dell'intervallo temporale da considerare
   * @param toDate La data finale dell'intervallo temporale da considerare (opzionale)
   * @param competenceRequestType Il tipo di richiesta di competenza specifico
   * @return La lista delle richieste di competenza sull'intervallo e la persona specificati.
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
   * Metodo che ritorna la lista di tutte le richieste di competenza attive.
   *
   * @param uros la lista dei ruoli della persona che deve approvare
   * @param fromDate da quando cercare le richieste
   * @param toDate a quando cercare le richieste (opzionale)
   * @param competenceRequestType il tipo della richiesta
   * @param signer il dipendente deputato all'approvazione
   * @return la lista di tutte le richieste di competenza attive.
   */
  public List<CompetenceRequest> allResults(List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      CompetenceRequestType competenceRequestType, Person signer) {
    Preconditions.checkNotNull(fromDate);

    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;
    final QPerson person = QPerson.person;
    final QPersonReperibility pr = QPersonReperibility.personReperibility;

    if (uros.stream().noneMatch(uro -> uro.getRole().getName().equals(Role.EMPLOYEE)
        || !signer.getReperibilityTypes().isEmpty())) {
      return Lists.newArrayList();
    }

    BooleanBuilder conditions = new BooleanBuilder();

    val results = Lists.<CompetenceRequest>newArrayList();
    List<Office> officeList = uros.stream().map(u -> u.getOffice()).collect(Collectors.toList());
    conditions.and(competenceRequest.startAt.after(fromDate))
    .and(competenceRequest.type.eq(competenceRequestType)
        .and(competenceRequest.flowStarted.isTrue())
        .and(competenceRequest.flowEnded.isFalse())
        .and(competenceRequest.person.office.in(officeList)));


    JPQLQuery<CompetenceRequest> query;
    if (!signer.getReperibilityTypes().isEmpty()) {
      conditions.and(competenceRequest.employeeApprovalRequired.isTrue())
      .and(competenceRequest.employeeApproved.isNull())
      .and(competenceRequest.managerApprovalRequired.isTrue())
      .and(competenceRequest.managerApproved.isNull())
      .and(person.office.eq(signer.getOffice()));
      query = getQueryFactory().selectFrom(competenceRequest)
          .join(competenceRequest.person, person)
          .leftJoin(person.reperibility, pr)
          .where(pr.personReperibilityType.in(signer.getReperibilityTypes()).and(conditions));
    } else {
      conditions.and(competenceRequest.employeeApprovalRequired.isTrue())
      .and(competenceRequest.employeeApproved.isNotNull()
          .and(competenceRequest.managerApprovalRequired.isTrue()
              .and(competenceRequest.managerApproved.isNull())));
      query = getQueryFactory().selectFrom(competenceRequest).where(conditions);
    }
    results.addAll(query.fetch());
    return results;
  }


  /**
   * Metodo che ritorna le richieste da approvare.
   *
   * @param roleList la lista dei ruoli sulla sede
   * @param fromDate da quando ricercare le richieste
   * @param toDate fino a quando ricercare le richieste
   * @param type il tipo di richiesta
   * @param signer chi deve firmare la richiesta
   * @return la lista di richieste di competenza da approvare.
   */
  public List<CompetenceRequest> toApproveResults(List<UsersRolesOffices> roleList,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate, CompetenceRequestType type,
      Person signer) {
    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;
    final QPerson person = QPerson.person;
    final QPersonReperibility pr = QPersonReperibility.personReperibility;

    BooleanBuilder conditions = new BooleanBuilder();

    if (roleList.stream().noneMatch(uro -> uro.getRole().getName().equals(Role.EMPLOYEE)
        || !signer.getReperibilityTypes().isEmpty())) {
      return Lists.newArrayList();
    }
    conditions.and(competenceRequest.type.eq(type)
        .and(competenceRequest.flowStarted.isTrue())
        .and(competenceRequest.flowEnded.isFalse()));
    if (toDate.isPresent()) {
      conditions.and(competenceRequest.endTo.before(toDate.get()));
    }

    List<CompetenceRequest> results = new ArrayList<>();

    switch (type) {
      case CHANGE_REPERIBILITY_REQUEST:
        if (!signer.getReperibilityTypes().isEmpty()) {
          List<Office> officeList = 
              roleList.stream().map(u -> u.getOffice()).collect(Collectors.toList());
          conditions = managerQuery(officeList, conditions, signer);
          List<CompetenceRequest> queryResults = getQueryFactory().selectFrom(competenceRequest)
              .join(competenceRequest.person, person)
              .join(person.reperibility, pr)
              .where(pr.personReperibilityType.supervisor.eq(signer).and(conditions))
              .fetch();
          results.addAll(queryResults);
        } else {
          conditions = employeeQuery(conditions, signer);
          List<CompetenceRequest> queryResults = getQueryFactory().selectFrom(competenceRequest)
              .where(competenceRequest.teamMate.eq(signer).and(conditions)).fetch();
          results.addAll(queryResults);
        }
        break;
      case OVERTIME_REQUEST:
        final QGroup group = QGroup.group;
        if (roleList.stream().noneMatch(r -> r.getRole().getName().equals(Role.SEAT_SUPERVISOR) 
            || r.getRole().getName().equals(Role.GROUP_MANAGER))) {
          return results;
        }
        List<Office> officeList = 
            roleList.stream().map(u -> u.getOffice()).collect(Collectors.toList());
        if (roleList.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))) {
          conditions = officeHeadQuery(officeList,conditions, signer);
        } else {     
          final QAffiliation affiliation = QAffiliation.affiliation;
          conditions = managerQuery(officeList, conditions, signer);
          List<CompetenceRequest> queryResults = getQueryFactory().selectFrom(competenceRequest)
              .join(competenceRequest.person, person).fetchJoin()
              .join(person.affiliations, affiliation)
              .on(affiliation.beginDate.before(LocalDate.now())
                  .and(affiliation.endDate.isNull().or(affiliation.endDate.after(LocalDate.now()))))
              .join(affiliation.group, group)
              .where(group.manager.eq(signer).and(conditions))
              .distinct()
              .fetch();
          results.addAll(queryResults);
          return results;
        }
        List<CompetenceRequest> queryResult = getQueryFactory()
            .selectFrom(competenceRequest).where(conditions).fetch();
        results.addAll(queryResult);
        break;
      default:
        break;          
    }

    return results;
  }

  /**
   * Metodo che ritorna la lista delle richieste totalmente approvate.
   *
   * @param roleList la lista dei ruoli sulla sede
   * @param fromDate da quando ricercare le richieste
   * @param toDate fino a quando ricercare le richieste
   * @param type il tipo di richiesta
   * @param signer la persona che deve approvare
   * @return la lista delle richieste totalmente approvate.
   */
  public List<CompetenceRequest> totallyApproved(List<UsersRolesOffices> roleList,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate, CompetenceRequestType type,
      Person signer) {
    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;
    final QPerson person = QPerson.person;
    final QPersonReperibility pr = QPersonReperibility.personReperibility;

    BooleanBuilder conditions = new BooleanBuilder();
    List<CompetenceRequest> results = new ArrayList<>();
    List<Office> officeList = 
        roleList.stream().map(u -> u.getOffice()).collect(Collectors.toList());

    conditions.and(competenceRequest.startAt.after(fromDate))
    .and(competenceRequest.type.eq(type).and(competenceRequest.flowEnded.isTrue())
        .and(competenceRequest.person.office.in(officeList)));

    if (toDate.isPresent()) {
      conditions.and(competenceRequest.endTo.before(toDate.get()));
    }

    switch (type) {
      case CHANGE_REPERIBILITY_REQUEST:
        if (!signer.getReperibilityTypes().isEmpty()) {
          conditions = managerApprovedQuery(officeList, conditions, signer);
          List<CompetenceRequest> queryResults = getQueryFactory().selectFrom(competenceRequest)
              .join(competenceRequest.person, person)
              .join(person.reperibility, pr)
              .where(pr.personReperibilityType.supervisor.eq(signer).and(conditions))
              .fetch();
          results.addAll(queryResults);
        } else {
          conditions = employeeApprovedQuery(conditions, signer);
          List<CompetenceRequest> queryResults = getQueryFactory().selectFrom(competenceRequest)
              .where(competenceRequest.teamMate.eq(signer).and(conditions)).fetch();
          results.addAll(queryResults);
        }
        break;
      case OVERTIME_REQUEST:
        final QGroup group = QGroup.group;
        if (roleList.stream().noneMatch(r -> r.getRole().getName().equals(Role.SEAT_SUPERVISOR) 
            || r.getRole().getName().equals(Role.GROUP_MANAGER))) {
          return results;
        }       
        if (roleList.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))) {
          conditions = officeHeadApprovedQuery(officeList,conditions, signer);
        } else {     
          final QAffiliation affiliation = QAffiliation.affiliation;
          conditions = managerApprovedQuery(officeList, conditions, signer);
          List<CompetenceRequest> queryResults = getQueryFactory().selectFrom(competenceRequest)
              .join(competenceRequest.person, person).fetchJoin()
              .join(person.affiliations, affiliation)
              .on(affiliation.beginDate.before(LocalDate.now())
                  .and(affiliation.endDate.isNull().or(affiliation.endDate.after(LocalDate.now()))))
              .join(affiliation.group, group)
              .where(group.manager.eq(signer).and(conditions))
              .distinct()
              .fetch();
          results.addAll(queryResults);
          return results;
        }
        List<CompetenceRequest> queryResult = getQueryFactory()
            .selectFrom(competenceRequest).where(conditions).fetch();
        results.addAll(queryResult);
        break;
      default:
        break;          
    }

    return results;
  }

  /**
   * Ritorna la lista delle richieste di competenza non ancora validate che presentano date di
   * inizio/fine che in qualche modo intersecano le date di inizio/fine della richiesta d'assenza da
   * validare.
   *
   * @param request la richiesta di competenza da far validare
   * @return la lista delle richieste di competenza che hanno parametri che non permettono 
   *     la corretta creazione della richiesta di competenza passata come parametro.
   */
  public List<CompetenceRequest> existingCompetenceRequests(CompetenceRequest request) {
    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;
    return getQueryFactory().selectFrom(competenceRequest)
        .where(competenceRequest.person.eq(request.getPerson())
            .and(
                competenceRequest.beginDateToAsk.between(request.getBeginDateToAsk(), request.getEndDateToAsk())
                  .or(competenceRequest.endDateToAsk.between(request.getBeginDateToAsk(), request.getEndDateToAsk()))
                  .or(competenceRequest.beginDateToGive.between(request.getBeginDateToGive(), request.getEndDateToGive()))
                  .or(competenceRequest.endDateToGive.between(request.getBeginDateToGive(), request.getEndDateToGive()))
                )
            .and(competenceRequest.flowEnded.eq(false)))
        .fetch();
  }


  /**
   * Metodo che aggiorna le condizioni di ricerca per il responsabile del servizio.
   *
   * @param officeList la lista delle sedi
   * @param condition le condizioni passate dal chiamante
   * @param signer la persona che deve autorizzare la richiesta
   * @return le condizioni aggiornate per il responsabile del servizio.
   */
  private BooleanBuilder managerQuery(List<Office> officeList, 
      BooleanBuilder condition, Person signer) {
    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;
    condition.and(competenceRequest.managerApprovalRequired.isTrue())
    .and(competenceRequest.managerApproved.isNull())
    .andAnyOf(competenceRequest.employeeApproved.isNotNull(), 
        competenceRequest.employeeApprovalRequired.isFalse())
    .and(competenceRequest.person.office.in(officeList));
    return condition;

  }
  
  private BooleanBuilder managerApprovedQuery(List<Office> officeList, 
      BooleanBuilder condition, Person signer) {
    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;
    condition.and(competenceRequest.managerApprovalRequired.isTrue())
    .and(competenceRequest.managerApproved.isNotNull())
    .andAnyOf(competenceRequest.employeeApproved.isNotNull(), 
        competenceRequest.employeeApprovalRequired.isFalse())
    .and(competenceRequest.person.office.in(officeList));
    return condition;

  }

  private BooleanBuilder employeeQuery(BooleanBuilder condition, Person signer) {
    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;
    condition.and(competenceRequest.employeeApprovalRequired.isTrue())
    .and(competenceRequest.employeeApproved.isNull());
    return condition;
  }
  
  private BooleanBuilder employeeApprovedQuery(BooleanBuilder condition, Person signer) {
    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;
    condition.and(competenceRequest.employeeApprovalRequired.isTrue())
    .and(competenceRequest.employeeApproved.isNotNull());
    return condition;
  }

  private BooleanBuilder officeHeadQuery(List<Office> officeList,
      BooleanBuilder condition, Person signer) {
    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;
    condition.and(competenceRequest.officeHeadApprovalRequired.isTrue())
    .and(competenceRequest.officeHeadApproved.isNull())
    .andAnyOf(competenceRequest.managerApproved.isNotNull(), 
        competenceRequest.managerApprovalRequired.isFalse())
    .and(competenceRequest.person.office.in(officeList));
    return condition;
  }
  
  private BooleanBuilder officeHeadApprovedQuery(List<Office> officeList,
      BooleanBuilder condition, Person signer) {
    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;
    condition.and(competenceRequest.officeHeadApprovalRequired.isTrue())
    .and(competenceRequest.officeHeadApproved.isNotNull())
    .andAnyOf(competenceRequest.managerApproved.isNotNull(), 
        competenceRequest.managerApprovalRequired.isFalse())
    .and(competenceRequest.person.office.in(officeList));
    return condition;
  }


}

