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
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
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
import models.informationrequests.IllnessRequest;
import models.informationrequests.ServiceRequest;
import models.informationrequests.TeleworkRequest;
import models.informationrequests.query.QIllnessRequest;
import models.informationrequests.query.QServiceRequest;
import models.informationrequests.query.QTeleworkRequest;
import models.query.QOffice;
import models.query.QPerson;

/**
 * Dao per i flussi informativi.
 *
 * @author dario
 */
public class InformationRequestDao extends DaoBase {

  @Inject
  InformationRequestDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Metodo che ritorna la lista delle richieste di flusso corrispondenti ai parametri passati.
   *
   * @param uroList         la lista dei ruoli sulle sedi
   * @param fromDate        da quale data cercare le richieste (opzionale)
   * @param toDate          a quale data cercare le richieste (opzionale)
   * @param informationType il tipo di richiesta
   * @param signer          chi deve firmare la richiesta
   * @return la lista delle richieste di flusso corrispondenti ai parametri passati.
   */
  public List<InformationRequest> toApproveResults(List<UsersRolesOffices> uroList,
      Optional<LocalDateTime> fromDate, Optional<LocalDateTime> toDate,
      InformationType informationType, Person signer) {
    final QInformationRequest informationRequest = QInformationRequest.informationRequest;

    BooleanBuilder conditions = new BooleanBuilder();

    if (uroList.stream().noneMatch(uro -> uro.getRole().getName().equals(Role.SEAT_SUPERVISOR)
        || uro.getRole().getName().equals(Role.PERSONNEL_ADMIN)
        || uro.getRole().getName().equals(Role.GROUP_MANAGER))) {
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
    if ((informationType.equals(InformationType.ILLNESS_INFORMATION) 
        || informationType.equals(InformationType.PARENTAL_LEAVE_INFORMATION))
        && uroList.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.PERSONNEL_ADMIN))) {
      results.addAll(toApproveResultsAsPersonnelAdmin(uroList,
          informationType, signer, conditions));
    } 
    if (informationType.equals(InformationType.SERVICE_INFORMATION) 
        && uroList.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.GROUP_MANAGER))) {
      results.addAll(toApproveResultsAsGroupManager(uroList, informationType, signer, conditions));
    }
    else {
      results.addAll(toApproveResultsAsSeatSuperVisor(uroList,
          informationType, signer, conditions));
    }

    return results;
  }

  /**
   * Lista delle richiesta di assenza per persona e data.
   *
   * @param person          La persona della quale recuperare le richieste di assenza
   * @param fromDate        La data iniziale dell'intervallo temporale da considerare
   * @param toDate          La data finale dell'intervallo temporale da considerare (opzionale)
   * @param informationType Il tipo di richiesta di assenza specifico
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
   * @param person          La persona della quale recuperare le richieste di assenza
   * @param fromDate        La data iniziale dell'intervallo temporale da considerare
   * @param toDate          La data finale dell'intervallo temporale da considerare (opzionale)
   * @param informationType Il tipo di richiesta di assenza specifico
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
   * @param person          La persona della quale recuperare le richieste di assenza
   * @param fromDate        La data iniziale dell'intervallo temporale da considerare
   * @param toDate          La data finale dell'intervallo temporale da considerare (opzionale)
   * @param informationType Il tipo di richiesta di assenza specifico
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

  /**
   * Ritorna la richiesta informativa con id passato come parametro.
   *
   * @param id l'identificativo della richiesta
   * @return La richiesta con l'id passato come parametro.
   */
  public InformationRequest getById(Long id) {
    final QInformationRequest informationRequest = QInformationRequest.informationRequest;

    return getQueryFactory().selectFrom(informationRequest)
        .where(informationRequest.id.eq(id)).fetchFirst();
  }

  /**
   * Ritorna la richiesta di uscita di servizio con l'id passato come parametro.
   *
   * @param id l'idendificativo della richiesta di uscita di servizio
   * @return l'uscita di servizio, se esiste, corrispondente all'id passato.
   */
  public Optional<ServiceRequest> getServiceById(Long id) {
    final QServiceRequest serviceRequest = QServiceRequest.serviceRequest;

    final ServiceRequest result = getQueryFactory()
        .selectFrom(serviceRequest).where(serviceRequest.id.eq(id)).fetchFirst();
    return Optional.fromNullable(result);

  }

  /**
   * Ritorna la richiesta di info per malattia con id passato come parametro.
   *
   * @param id l'identificativo della richiesta di informazione di malattia
   * @return la richiesta di info per malattia con id passato come parametro.
   */
  public Optional<IllnessRequest> getIllnessById(Long id) {
    final QIllnessRequest illnessRequest = QIllnessRequest.illnessRequest;

    final IllnessRequest result = getQueryFactory()
        .selectFrom(illnessRequest).where(illnessRequest.id.eq(id)).fetchFirst();
    return Optional.fromNullable(result);

  }

  /**
   * Cerca e ritorna la richiesta di telelavoro (se esiste) con id uguale a quello passato.
   *
   * @param id l'identificativo della richiesta di telelavoro da cercare
   * @return la richiesta di telelavoro (se esiste) con id uguale a quello passato.
   */
  public Optional<TeleworkRequest> getTeleworkById(Long id) {
    final QTeleworkRequest teleworkRequest = QTeleworkRequest.teleworkRequest;

    final TeleworkRequest result = getQueryFactory()
        .selectFrom(teleworkRequest).where(teleworkRequest.id.eq(id)).fetchFirst();
    return Optional.fromNullable(result);

  }

  /**
   * La lista delle richieste di malattia appartenenti alla lista di id passati come parametro.
   *
   * @param ids la lista di id di richieste di malattia
   * @return la lista delle richieste di malattia appartenenti alla lista di id passati come
   * parametro.
   */
  public List<IllnessRequest> illnessByIds(List<Long> ids) {
    final QIllnessRequest illnessRequest = QIllnessRequest.illnessRequest;
    return getQueryFactory().selectFrom(illnessRequest)
        .where(illnessRequest.id.in(ids)).fetch();
  }

  /**
   * La lista delle richieste di uscita di servizio appartenenti alla lista di id passati come
   * parametro.
   *
   * @param ids la lista di id di richieste di uscita di servizio
   * @return la lista delle richieste di uscita di servizio appartenenti alla lista di id passati
   * come parametro.
   */
  public List<ServiceRequest> servicesByIds(List<Long> ids) {
    final QServiceRequest serviceRequest = QServiceRequest.serviceRequest;
    return getQueryFactory().selectFrom(serviceRequest)
        .where(serviceRequest.id.in(ids)).fetch();
  }

  /**
   * La lista delle richieste di telelavoro appartenenti alla lista di id passati come parametro.
   *
   * @param ids la lista di id di richieste di telelavoro
   * @return la lista delle richieste di telelavoro appartenenti alla lista di id passati come
   * parametro.
   */
  public List<TeleworkRequest> teleworksByIds(List<Long> ids) {
    final QTeleworkRequest teleworkRequest = QTeleworkRequest.teleworkRequest;
    return getQueryFactory().selectFrom(teleworkRequest)
        .where(teleworkRequest.id.in(ids)).fetch();
  }

  /**
   * La lista di tutte le richieste di telelavoro effettuate dalla persona passata come parametro.
   *
   * @param person la persona di cui ricercare le richieste di telelavoro
   * @return la lista di tutte le richieste di telelavoro effettuate dalla persona passata come
   * parametro.
   */
  public List<TeleworkRequest> personTeleworkList(Person person) {
    final QTeleworkRequest teleworkRequest = QTeleworkRequest.teleworkRequest;
    return getQueryFactory().selectFrom(teleworkRequest).where(teleworkRequest.person.eq(person))
        .orderBy(teleworkRequest.year.desc(), teleworkRequest.month.desc()).fetch();
  }

  /**
   * La richiesta di telelavoro effettuate dalla persona del mese ed anno passati come parametro.
   *
   * @param person la persona di cui ricercare le richieste di telelavoro
   * @param month  il mese della richiesta di telelavoro
   * @param year   l'anno della richieste di telelavoro
   * @return la richiesta di telelavoro effettuate dalla persona del mese ed anno passati come
   * parametro.
   */
  public Optional<TeleworkRequest> personTeleworkInPeriod(Person person, Integer month,
      Integer year) {
    final QTeleworkRequest teleworkRequest = QTeleworkRequest.teleworkRequest;

    final BooleanBuilder conditions = new BooleanBuilder();
    conditions.and(teleworkRequest.person.eq(person))
        .and(teleworkRequest.month.eq(month)
            .and(teleworkRequest.year.eq(year)));

    final TeleworkRequest result = getQueryFactory().selectFrom(teleworkRequest)
        .where(conditions).fetchFirst();
    return Optional.fromNullable(result);
  }

  /**
   * Ritorna la lista delle informationRequest da approvare come responsabile di sede.
   *
   * @param uros            la lista degli userRoleOffice
   * @param informationType il tipo di richiesta
   * @param signer          il firmatario della richiesta
   * @param conditions      le condizioni da applicare alla ricerca
   * @return Lista delle InformationRequest da Approvare come responsabile di sede.
   */
  private List<InformationRequest> toApproveResultsAsSeatSuperVisor(List<UsersRolesOffices> uros,
      InformationType informationType, Person signer, BooleanBuilder conditions) {
    final QInformationRequest informationRequest = QInformationRequest.informationRequest;

    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.SEAT_SUPERVISOR)
        || uro.getRole().getName().equals(Role.PERSONNEL_ADMIN))) {
      List<Office> officeList = uros.stream().map(u -> u.getOffice()).collect(Collectors.toList());
      conditions = seatSupervisorQuery(officeList, conditions, signer);
      return getQueryFactory().selectFrom(informationRequest).where(conditions).fetch();
    } else {
      return Lists.newArrayList();
    }
  }

  /**
   * Lista delle InformationRequest da Approvare come responsabile di sede.
   */
  private List<InformationRequest> toApproveResultsAsPersonnelAdmin(List<UsersRolesOffices> uros,
      InformationType informationType, Person signer, BooleanBuilder conditions) {
    final QInformationRequest informationRequest = QInformationRequest.informationRequest;

    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.PERSONNEL_ADMIN))) {
      List<Office> officeList = uros.stream().map(u -> u.getOffice()).collect(Collectors.toList());
      conditions = personnelAdminQuery(officeList, conditions, signer);
      return getQueryFactory().selectFrom(informationRequest).where(conditions).fetch();
    } else {
      return Lists.newArrayList();
    }
  }
  
  /**
   * Lista delle InformationRequest da approvare come responsabile di gruppo.
   */
  private List<InformationRequest> toApproveResultsAsGroupManager(List<UsersRolesOffices> uros,
      InformationType informationType, Person signer, BooleanBuilder conditions) {
    final QInformationRequest informationRequest = QInformationRequest.informationRequest;
    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.GROUP_MANAGER))) {
      List<Office> officeList = uros.stream().map(u -> u.getOffice()).collect(Collectors.toList());
      conditions = groupManagerQuery(officeList, conditions, signer);
      return getQueryFactory().selectFrom(informationRequest).where(conditions).fetch();
    } else {
      return Lists.newArrayList();
    }
  }


  /**
   * Ritorna le condizioni con l'aggiunta di quelle relative al responsabile di sede.
   *
   * @param officeList la lista delle sedi
   * @param condition  le condizioni pregresse
   * @param signer     colui che deve firmare la richiesta
   * @return le condizioni per determinare se il responsabile di sede è coinvolto nell'approvazione.
   */
  private BooleanBuilder seatSupervisorQuery(List<Office> officeList,
      BooleanBuilder condition, Person signer) {

    final QInformationRequest informationRequest = QInformationRequest.informationRequest;
    condition.and(informationRequest.person.office.in(officeList))
        .and(informationRequest.officeHeadApprovalRequired.isTrue()
            .and(informationRequest.officeHeadApproved.isNull()));

    return condition;
  }

  /**
   * Ritorna le condizioni con l'aggiunta di quelle relative al responsabile di sede.
   *
   * @param officeList la lista delle sedi
   * @param condition  le condizioni pregresse
   * @param signer     colui che deve firmare la richiesta
   * @return le condizioni per determinare se il responsabile di sede è coinvolto nell'approvazione.
   */
  private BooleanBuilder personnelAdminQuery(List<Office> officeList,
      BooleanBuilder condition, Person signer) {

    final QInformationRequest informationRequest = QInformationRequest.informationRequest;
    condition.and(informationRequest.person.office.in(officeList))
        .and(informationRequest.administrativeApprovalRequired.isTrue()
            .and(informationRequest.administrativeApproved.isNull()));

    return condition;
  }
  
  /**
   * Ritorna le condizioni con l'aggiunta di quelle relative al responsabile di gruppo.
   *
   * @param officeList la lista delle sedi
   * @param condition  le condizioni pregresse
   * @param signer     colui che deve firmare la richiesta
   * @return le condizioni per determinare se il responsabile di gruppo è coinvolto nell'approvazione.
   */
  private BooleanBuilder groupManagerQuery(List<Office> officeList, 
      BooleanBuilder condition, Person signer) {
    final QInformationRequest informationRequest = QInformationRequest.informationRequest;
    condition.and(informationRequest.person.office.in(officeList))
    .and(informationRequest.managerApprovalRequired.isTrue()
        .and(informationRequest.managerApproved.isNull()));
    return condition;
  }

  /**
   * Metodo che ritorna la lista delle richieste già approvate per ruolo data e tipo.
   *
   * @param uros            la lista degli users_roles_offices
   * @param fromDate        la data da cui cercare le richieste
   * @param toDate          la data fino a cui cercare le richieste (opzionale)
   * @param informationType il tipo della richiesta.
   * @return la lista delle richieste totalmente approvate.
   */
  public List<InformationRequest> totallyApproved(List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      InformationType informationType, Person signer) {
    Preconditions.checkNotNull(fromDate);

    final QInformationRequest informationRequest = QInformationRequest.informationRequest;

    BooleanBuilder conditions = new BooleanBuilder();
    List<InformationRequest> results = new ArrayList<>();

    List<Office> officeList = uros.stream().map(u -> u.getOffice()).collect(Collectors.toList());
    conditions.and(informationRequest.startAt.after(fromDate))
        .and(informationRequest.informationType.eq(informationType)
            .and(informationRequest.flowEnded.isTrue())
            .and(informationRequest.person.office.in(officeList)));

    if (toDate.isPresent()) {
      conditions.and(informationRequest.endTo.before(toDate.get()));
    }

    results.addAll(totallyApprovedOnRole(
        uros, fromDate, toDate, informationType, signer));

    JPQLQuery<InformationRequest> query;
    query = getQueryFactory().selectFrom(informationRequest).where(conditions)
        .orderBy(informationRequest.startAt.desc());

    results.addAll(query.fetch());
    return results;
  }

  private List<InformationRequest> totallyApprovedOnRole(List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      InformationType informationType, Person signer) {
    Preconditions.checkNotNull(fromDate);

    final QInformationRequest informationRequest = QInformationRequest.informationRequest;
    final QPerson person = QPerson.person;
    final QOffice office = QOffice.office;
    List<Office> officeList = uros.stream().map(u -> u.getOffice()).collect(Collectors.toList());
    BooleanBuilder conditions = new BooleanBuilder();
    conditions.and(informationRequest.startAt.after(fromDate))
        .and(informationRequest.informationType.eq(informationType)
            .and(informationRequest.flowEnded.isTrue())
            .and(informationRequest.person.office.eq(signer.getOffice())));

    if (toDate.isPresent()) {
      conditions.and(informationRequest.endTo.before(toDate.get()));
    }
    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.SEAT_SUPERVISOR))) {
      conditions.and(
          informationRequest.officeHeadApprovalRequired.isTrue()
              .and(informationRequest.officeHeadApproved.isNotNull())
              .and(person.office.in(officeList)));
    } 
    if (uros.stream().anyMatch(uro -> uro.getRole().getName().equals(Role.PERSONNEL_ADMIN))){
      conditions.and(
          informationRequest.administrativeApprovalRequired.isTrue()
              .and(informationRequest.administrativeApproved.isNotNull())
              .and(person.office.in(officeList)));
    } else {
      conditions.and(
          informationRequest.managerApprovalRequired.isTrue()
              .and(informationRequest.managerApproved.isNotNull())
              .and(person.office.in(officeList)));
    }
    return getQueryFactory().selectFrom(informationRequest)
        .join(informationRequest.person, person)
        .join(person.office, office)
        .where(office.in(uros.stream().map(
                userRoleOffices -> userRoleOffices.getOffice())
            .collect(Collectors.toSet())).and(conditions))
        .orderBy(informationRequest.startAt.desc())
        .fetch();
  }
}
