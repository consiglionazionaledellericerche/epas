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
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Person;
import models.UsersRolesOffices;
import models.enumerate.InformationType;
import models.flows.AbsenceRequest;
import models.flows.Group;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.query.QAbsenceRequest;
import models.informationrequests.IllnessRequest;
import models.informationrequests.ServiceRequest;
import models.informationrequests.TeleworkRequest;
import models.informationrequests.query.QIllnessRequest;
import models.informationrequests.query.QServiceRequest;
import models.informationrequests.query.QTeleworkRequest;

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
  
  public List<IllnessRequest> toApproveIllnessResults() {
    return null;
  }
  
  public List<TeleworkRequest> toApproveTeleworkResults(List<UsersRolesOffices> uroList, 
      Optional<LocalDateTime> fromDate, Optional<LocalDateTime> toDate,
      InformationType informationType, List<Group> groups, Person signer) {
    return null;
  }
  
  public List<ServiceRequest> toApproveServiceResults() {
    return null;
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
        .and(serviceRequest.startAt.after(fromDate.toLocalTime()))
        .and(serviceRequest.informationType.eq(informationType)));
    if (toDate.isPresent()) {
      conditions.and(serviceRequest.endTo.before(toDate.get().toLocalTime()));
    }
    if (active) {
      conditions.and(serviceRequest.flowEnded.eq(false));
    } else {
      conditions.and(serviceRequest.flowEnded.eq(true));
    }
    return getQueryFactory().selectFrom(serviceRequest)
        .where(conditions).orderBy(serviceRequest.startAt.desc()).fetch();
  }
}
