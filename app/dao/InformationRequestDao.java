/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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
import com.querydsl.jpa.JPQLQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Office;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import models.base.InformationRequest;
import models.base.query.QInformationRequest;
import models.enumerate.InformationType;
import models.flows.AbsenceRequest;
import models.flows.Group;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.query.QAbsenceRequest;
import models.flows.query.QAffiliation;
import models.flows.query.QGroup;
import models.informationrequests.IllnessRequest;
import models.informationrequests.ServiceRequest;
import models.informationrequests.TeleworkRequest;
import models.informationrequests.query.QIllnessRequest;
import models.informationrequests.query.QServiceRequest;
import models.informationrequests.query.QTeleworkRequest;
import models.query.QPerson;

/**
 * Dao per i flussi informativi.
 * 
 * @author dario
 *
 */
public class InformationRequestDao extends DaoBase {

  @Inject
  InformationRequestDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  public List<InformationRequest> toApproveResults(List<UsersRolesOffices> uroList, 
      Optional<LocalDateTime> fromDate, Optional<LocalDateTime> toDate,
      InformationType informationType, Person signer) {
    final QInformationRequest informationRequest = QInformationRequest.informationRequest;

    BooleanBuilder conditions = new BooleanBuilder();

    if (uroList.stream().noneMatch(uro -> uro.role.name.equals(Role.SEAT_SUPERVISOR))) {
      return Lists.newArrayList();
    }
    if (fromDate.isPresent()) {
      conditions.and(informationRequest.startAt.after(fromDate.get()));
    }
    if (toDate.isPresent()) {
      conditions.and(informationRequest.endTo.before(toDate.get()));
    }   
    conditions.and(informationRequest.informationType.eq(informationType)
        .and(informationRequest.flowStarted.isTrue())
        .and(informationRequest.flowEnded.isFalse()));

    List<InformationRequest> results = new ArrayList<>();

    results.addAll(toApproveResultsAsSeatSuperVisor(uroList, 
        informationType, signer, conditions));

    return results;
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
  public List<TeleworkRequest> teleworksByPersonAndDate(Person person,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      InformationType informationType, boolean active) {

    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(fromDate);

    final QTeleworkRequest teleworkRequest = QTeleworkRequest.teleworkRequest;

    BooleanBuilder conditions = new BooleanBuilder(teleworkRequest.person.eq(person)
        .and(teleworkRequest.startAt.after(fromDate))
        .and(teleworkRequest.informationType.eq(informationType)));
    if (toDate.isPresent()) {
      conditions.and(teleworkRequest.endTo.before(toDate.get()));
    }
    if (active) {
      conditions.and(teleworkRequest.flowEnded.eq(false));
    } else {
      conditions.and(teleworkRequest.flowEnded.eq(true));
    }
    return getQueryFactory().selectFrom(teleworkRequest)
        .where(conditions).orderBy(teleworkRequest.startAt.desc()).fetch();
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
  public List<IllnessRequest> illnessByPersonAndDate(Person person,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      InformationType informationType, boolean active) {

    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(fromDate);

    final QIllnessRequest illnessRequest = QIllnessRequest.illnessRequest;

    BooleanBuilder conditions = new BooleanBuilder(illnessRequest.person.eq(person)
        .and(illnessRequest.startAt.after(fromDate))
        .and(illnessRequest.informationType.eq(informationType)));
    if (toDate.isPresent()) {
      conditions.and(illnessRequest.endTo.before(toDate.get()));
    }
    if (active) {
      conditions.and(illnessRequest.flowEnded.eq(false));
    } else {
      conditions.and(illnessRequest.flowEnded.eq(true));
    }
    return getQueryFactory().selectFrom(illnessRequest)
        .where(conditions).orderBy(illnessRequest.startAt.desc()).fetch();
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
  public List<ServiceRequest> servicesByPersonAndDate(Person person,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      InformationType informationType, boolean active) {

    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(fromDate);

    final QServiceRequest serviceRequest = QServiceRequest.serviceRequest;

    BooleanBuilder conditions = new BooleanBuilder(serviceRequest.person.eq(person)
        .and(serviceRequest.startAt.after(fromDate))
        .and(serviceRequest.informationType.eq(informationType)));
    if (toDate.isPresent()) {
      conditions.and(serviceRequest.endTo.before(toDate.get()));
    }
    if (active) {
      conditions.and(serviceRequest.flowEnded.eq(false));
    } else {
      conditions.and(serviceRequest.flowEnded.eq(true));
    }
    return getQueryFactory().selectFrom(serviceRequest)
        .where(conditions).orderBy(serviceRequest.startAt.desc()).fetch();
  }

  public InformationRequest getById(Long id) {
    final QInformationRequest informationRequest = QInformationRequest.informationRequest;

    return getQueryFactory().selectFrom(informationRequest)
        .where(informationRequest.id.eq(id)).fetchFirst();
  }

  public Optional<ServiceRequest> getServiceById(Long id) {
    final QServiceRequest serviceRequest = QServiceRequest.serviceRequest;

    final ServiceRequest result = getQueryFactory()
        .selectFrom(serviceRequest).where(serviceRequest.id.eq(id)).fetchFirst();
    return Optional.fromNullable(result);

  }

  public Optional<IllnessRequest> getIllnessById(Long id) {
    final QIllnessRequest illnessRequest = QIllnessRequest.illnessRequest;

    final IllnessRequest result = getQueryFactory()
        .selectFrom(illnessRequest).where(illnessRequest.id.eq(id)).fetchFirst();
    return Optional.fromNullable(result);

  }

  public Optional<TeleworkRequest> getTeleworkById(Long id) {
    final QTeleworkRequest teleworkRequest = QTeleworkRequest.teleworkRequest;

    final TeleworkRequest result = getQueryFactory()
        .selectFrom(teleworkRequest).where(teleworkRequest.id.eq(id)).fetchFirst();
    return Optional.fromNullable(result);

  }


  /**
   * Lista delle InformationRequest da Approvare come responsabile di sede.
   */
  private List<InformationRequest> toApproveResultsAsSeatSuperVisor(List<UsersRolesOffices> uros,
      InformationType informationType, Person signer, BooleanBuilder conditions) {
    final QInformationRequest informationRequest = QInformationRequest.informationRequest;

    if (uros.stream().anyMatch(uro -> uro.role.name.equals(Role.SEAT_SUPERVISOR))) {
      List<Office> officeList = uros.stream().map(u -> u.office).collect(Collectors.toList());
      conditions = seatSupervisorQuery(officeList, conditions, signer);
      return getQueryFactory().selectFrom(informationRequest).where(conditions).fetch();
    } else {
      return Lists.newArrayList();
    }
  }


  /**
   * Ritorna le condizioni con l'aggiunta di quelle relative al responsabile di sede.
   *
   * @param officeList la lista delle sedi
   * @param condition le condizioni pregresse
   * @param signer colui che deve firmare la richiesta
   * @return le condizioni per determinare se il responsabile di sede è coinvolto nell'approvazione.
   *    
   */
  private BooleanBuilder seatSupervisorQuery(List<Office> officeList, 
      BooleanBuilder condition, Person signer) {

    final QInformationRequest informationRequest = QInformationRequest.informationRequest;
    condition.and(informationRequest.person.office.in(officeList))
    .and(informationRequest.officeHeadApprovalRequired.isTrue()
        .and(informationRequest.officeHeadApproved.isNull()));

    return condition;
  }
}
