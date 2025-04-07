/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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

import static com.querydsl.core.group.GroupBy.groupBy;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.filter.QFilters;
import helpers.JodaConverters;
import helpers.jpa.ModelQuery;
import helpers.jpa.ModelQuery.SimpleResults;
import it.cnr.iit.epas.DateInterval;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import manager.configurations.EpasParam;
import models.BadgeReader;
import models.CompetenceCode;
import models.CompetenceCodeGroup;
import models.Contract;
import models.Institute;
import models.Office;
import models.Person;
import models.PersonDay;
import models.enumerate.ContractType;
import models.flows.query.QAffiliation;
import models.flows.query.QGroup;
import models.query.QBadge;
import models.query.QConfiguration;
import models.query.QContract;
import models.query.QContractStampProfile;
import models.query.QContractWorkingTimeType;
import models.query.QOffice;
import models.query.QPerson;
import models.query.QPersonCompetenceCodes;
import models.query.QPersonConfiguration;
import models.query.QPersonDay;
import models.query.QPersonHourForOvertime;
import models.query.QPersonReperibility;
import models.query.QPersonShift;
import models.query.QPersonShiftShiftType;
import models.query.QPersonsOffices;
import models.query.QUser;
import models.query.QWorkingTimeType;
import models.query.QWorkingTimeTypeDay;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.testng.util.Strings;

/**
 * DAO per le person.
 *
 * @author Marco Andreini
 */
public final class PersonDao extends DaoBase {

  @Inject
  public PersonDayDao personDayDao;

  @Inject
  PersonDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Persona (se esiste) a partire dal id.
   *
   * @param id l'id della persona.
   * @return la persona che ha associato l'id.
   */
  public Optional<Person> byId(Long id) {
    final QPerson person = QPerson.person;
    final Person result = getQueryFactory().selectFrom(person).where(person.id.eq(id))
        .fetchOne();
    return Optional.fromNullable(result);
  }

  /**
   * Lista di persone attive per anno/mese su set di sedi.
   *
   * @param offices la lista degli uffici
   * @param yearMonth l'oggetto anno/mese
   * @return la lista delle persone di un certo ufficio attive in quell'anno/mese.
   */
  public List<Person> getActivePersonInMonth(Set<Office> offices, YearMonth yearMonth) {
    int year = yearMonth.getYear();
    int month = yearMonth.getMonthOfYear();

    Optional<LocalDate> beginMonth = Optional.fromNullable(new LocalDate(year, month, 1));
    Optional<LocalDate> endMonth =
        Optional.fromNullable(beginMonth.get().dayOfMonth().withMaximumValue());

    return personQuery(Optional.absent(), offices, false, beginMonth, endMonth,
        true, Optional.absent(), Optional.absent(), false).fetch();
  }

  /**
   * Lista dei tecnici attivi per anno/mese su set di sedi.
   *
   * @param offices la lista degli uffici
   * @param yearMonth l'oggetto anno/mese
   * @return la lista delle persone di un certo ufficio attive in quell'anno/mese.
   */
  public List<Person> getActiveTechnicianInMonth(Set<Office> offices, YearMonth yearMonth) {    
    return personQuery(Optional.absent(), offices, true, 
        Optional.of(yearMonth.toLocalDate(1)), 
        Optional.of(yearMonth.toLocalDate(1).dayOfMonth().withMaximumValue()),
        true, Optional.absent(), Optional.absent(), false).fetch();
  }


  /**
   * La lista di persone una volta applicati i filtri dei parametri. (Dovrà sostituire list
   * deprecata). TODO: Perseo significa che utilizza i metodi puliti di paginazione implementati da
   * Marco (PerseoSimpleResult e ModelQuery) che dovranno sostituire i deprecati SimpleResult e
   * ModelQuery di epas.
   */
  public SimpleResults<Person> listPerseo(Optional<String> name, Set<Office> offices,
      boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {

    final QPerson person = QPerson.person;

    return ModelQuery.wrap(
        // JPQLQuery
        personQuery(name, offices, onlyTechnician, Optional.fromNullable(start),
            Optional.fromNullable(end), onlyOnCertificate, Optional.absent(),
            Optional.absent(), false),
        // Expression
        person);
  }

  /**
   * Tutte le persone di epas (possibile filtrare sulla sede).
   */
  public SimpleResults<Person> list(Optional<Office> office) {
    final QPerson person = QPerson.person;

    Set<Office> offices = Sets.newHashSet();
    if (office.isPresent()) {
      offices.add(office.get());
    }
    return ModelQuery.wrap(
        // JPQLQuery
        personQuery(Optional.absent(), offices, false, Optional.absent(),
            Optional.absent(), false, Optional.absent(),
            Optional.absent(), false),
        // Expression
        person);

  }


  /**
   * La lista di persone una volta applicati i filtri dei parametri.
   */
  public SimpleResults<Person> list(Optional<String> name, Set<Office> offices,
      boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {

    final QPerson person = QPerson.person;

    return ModelQuery.wrap(
        // JPQLQuery
        personQuery(name, offices, onlyTechnician, Optional.fromNullable(start),
            Optional.fromNullable(end), onlyOnCertificate, Optional.absent(),
            Optional.absent(), false),
        // Expression
        person);
  }

  /**
   * Lista di persone in base alla sede.
   *
   * @param office Ufficio
   * @return Restituisce la lista delle persone appartenenti all'ufficio specificato.
   */
  public List<Person> byOffice(Office office) {
    return personQuery(Optional.absent(), ImmutableSet.of(office), false, Optional.absent(),
        Optional.absent(), false, Optional.absent(), Optional.absent(), false).fetch();
  }

  /**
   * Lista delle persone appartenenti all'istituto.
   *
   * @param institute l'istituto passato come parametro
   * @return la lista delle persone afferenti all'istituto passato come parametro.
   */
  public List<Person> byInstitute(Institute institute) {
    final QPerson person = QPerson.person;
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;

    return getQueryFactory()
        .selectFrom(person).leftJoin(person.personsOffices, personsOffices)
        .where(personsOffices.office.institute.eq(institute)).orderBy(person.surname.asc()).fetch();
  }


  /**
   * Permette la fetch automatica di tutte le informazioni delle persone filtrate. TODO: e' usata
   * solo in Persons.list ma se serve in altri metodi rendere parametrica la funzione
   * PersonDao.list.
   *
   * @param name l'eventuale nome da filtrare
   * @param offices la lista degli uffici su cui cercare
   * @param onlyTechnician true se cerco solo i tecnici, false altrimenti
   * @param start da quando iniziare la ricerca
   * @param end quando terminare la ricerca
   * @param onlyOnCertificate true se voglio solo gli strutturati, false altrimenti
   * @return la lista delle persone trovate con queste retrizioni
   */
  public SimpleResults<Person> listFetched(Optional<String> name, Set<Office> offices,
      boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {

    final QPerson person = QPerson.person;

    JPQLQuery<Person> query = personQuery(name, offices, onlyTechnician,
        Optional.fromNullable(start),
        Optional.fromNullable(end), onlyOnCertificate, Optional.absent(),
        Optional.absent(), false);

    SimpleResults<Person> result = ModelQuery.wrap(
        // JPQLQuery
        query,
        // Expression
        person);

    fetchContracts(Sets.newHashSet(result.list()), Optional.fromNullable(start),
        Optional.fromNullable(end));

    return result;

  }

  /**
   * La lista delle persone abilitate alla competenza competenceCode. E che superano i filtri dei
   * parametri.
   *
   * @param competenceCode codice competenza
   * @param name name
   * @param offices offices
   * @param onlyTechnician solo tecnologhi
   * @param start attivi da
   * @param end attivi a
   * @return model query delle persone selezionte.
   */
  public SimpleResults<Person> listForCompetence(CompetenceCode competenceCode,
      Optional<String> name, Set<Office> offices, boolean onlyTechnician,
      LocalDate start, LocalDate end, Optional<Person> personInCharge) {

    Preconditions.checkState(!offices.isEmpty());
    Preconditions.checkNotNull(competenceCode);

    final QPerson person = QPerson.person;

    return ModelQuery.wrap(personQuery(name, offices, onlyTechnician,
        Optional.fromNullable(start), Optional.fromNullable(end), true,
        Optional.fromNullable(competenceCode), personInCharge, false), person);

  }

  /**
   * Lista per codice di competenza.
   *
   * @param offices Uffici dei quali verificare le persone
   * @param yearMonth Il mese interessato
   * @param code Il codice di competenza da considerare
   * @return La lista delle persone con il codice di competenza abilitato nel mese specificato.
   */
  public List<Person> listForCompetence(
      Set<Office> offices, YearMonth yearMonth, CompetenceCode code) {
    int year = yearMonth.getYear();
    int month = yearMonth.getMonthOfYear();

    Optional<LocalDate> beginMonth = Optional.fromNullable(new LocalDate(year, month, 1));
    Optional<LocalDate> endMonth =
        Optional.fromNullable(beginMonth.get().dayOfMonth().withMaximumValue());
    return personQuery(Optional.absent(), offices, false, beginMonth, endMonth,
        true, Optional.fromNullable(code), Optional.absent(), false)
        .fetch();
  }

  /**
   * La lista di persone che rispondono ai criteri di ricerca.
   *
   * @param group il gruppo di codici di competenza
   * @param offices l'insieme delle sedi
   * @param onlyTechnician se si vogliono solo i tecnici
   * @param start da quando fare la ricerca
   * @param end fino a quando fare la ricerca
   * @return la lista di persone che rispondono ai criteri di ricerca.
   */
  public List<Person> listForCompetenceGroup(CompetenceCodeGroup group, 
      Set<Office> offices, boolean onlyTechnician,
      LocalDate start, LocalDate end, boolean temporary) {

    Preconditions.checkState(!offices.isEmpty());
    Preconditions.checkNotNull(group);

    final QPerson person = QPerson.person;
    final QContract contract = QContract.contract;

    final BooleanBuilder condition = new BooleanBuilder();

    final JPQLQuery<Person> query = getQueryFactory()
        .selectFrom(person).distinct()
        .leftJoin(person.contracts, contract)
        .leftJoin(person.qualification).fetchJoin()
        .leftJoin(person.personCompetenceCodes, QPersonCompetenceCodes.personCompetenceCodes);

    filterOffices(condition, offices, Optional.fromNullable(start), Optional.fromNullable(end), false);
    filterOnlyTechnician(condition, onlyTechnician);
    filterContract(condition, Optional.fromNullable(start), Optional.fromNullable(end));
    if (temporary) {
      filterTemporary(condition);
    }


    filterCompetenceCodeGroupEnabled(condition, Optional.fromNullable(group), start);

    return query.where(condition).fetch();

  }

  /**
   * Una mappa contenente le persone con perseoId valorizzato. La chiave è il perseoId.
   *
   * @param office sede opzionale, se absente tutte le persone.
   * @return mappa
   */
  public Map<Long, Person> mapSynchronized(Optional<Office> office) {

    final QPerson person = QPerson.person;

    //Il metodo personQuery considera la lista solo se non è ne null ne vuota.
    Set<Office> offices = Sets.newHashSet();
    if (office.isPresent()) {
      offices.add(office.get());
    }

    return personQuery(Optional.absent(), offices, false, Optional.absent(),
        Optional.absent(), false, Optional.absent(),
        Optional.absent(), true)
        .transform(groupBy(person.perseoId).as(person));

  }

  /**
   * L'ultimo contratto inserito in ordine di data inizio. (Tendenzialmente quello attuale)
   *
   * @param person la persona di cui si richiede il contratto
   * @return l'ultimo contratto in ordine temporale.
   */
  public Optional<Contract> getLastContract(Person person) {

    final QContract contract = QContract.contract;

    final List<Contract> results = getQueryFactory().selectFrom(contract)
        .where(contract.person.eq(person))
        .orderBy(contract.beginDate.desc()).fetch();

    if (results.isEmpty()) {
      return Optional.absent();
    }
    return Optional.fromNullable(results.get(0));

  }

  /**
   * Il contratto precedente in ordine temporale rispetto a quello passato come argomento.
   */
  public Contract getPreviousPersonContract(Contract contract) {

    final QContract qcontract = QContract.contract;

    final List<Contract> results =

        getQueryFactory().selectFrom(qcontract).where(qcontract.person.eq(contract.getPerson()))
        .orderBy(qcontract.beginDate.desc()).fetch();

    final int indexOf = results.indexOf(contract);
    if (indexOf + 1 < results.size()) {
      return results.get(indexOf + 1);
    } else {
      return null;
    }
  }

  /**
   * Lista di contratti per persona nel periodo.
   *
   * @param person la persona di cui si vogliono i contratti
   * @param fromDate la data di inizio da cui cercare
   * @param toDate la data di fine in cui cercare
   * @return la lista di contratti che soddisfa le seguenti condizioni.
   */
  public List<Contract> getContractList(Person person, LocalDate fromDate, LocalDate toDate) {

    final QContract contract = QContract.contract;

    BooleanBuilder conditions =
        new BooleanBuilder(contract.person.eq(person).and(contract.beginDate.loe(toDate)));

    conditions.andAnyOf(contract.endContract.isNull().and(contract.endDate.isNull()),
        contract.endContract.isNull().and(contract.endDate.goe(fromDate)),
        contract.endContract.isNotNull().and(contract.endContract.goe(fromDate)));

    return getQueryFactory().selectFrom(contract).where(conditions)
        .orderBy(contract.beginDate.asc()).fetch();
  }

  /**
   * Ritorna la lista dei person day della persona nella finestra temporale specificata ordinati per
   * data con ordinimento crescente.
   *
   * @param person la persona di cui si chiedono i personday
   * @param interval l'intervallo dei personday
   * @param onlyWithMealTicket se con i mealticket associati
   * @return la lista dei personday che soddisfano i parametri
   */
  public List<PersonDay> getPersonDayIntoInterval(Person person, DateInterval interval,
      boolean onlyWithMealTicket) {

    final QPersonDay qpd = QPersonDay.personDay;

    final BooleanBuilder condition = new BooleanBuilder();
    condition.and(qpd.person.eq(person).and(qpd.date.goe(interval.getBegin()))
        .and(qpd.date.loe(interval.getEnd())));

    if (onlyWithMealTicket) {
      condition.and(qpd.isTicketAvailable.eq(true));
    }
    return getQueryFactory().selectFrom(qpd).where(condition).orderBy(qpd.date.asc()).fetch();
  }

  /**
   * Preleva la persona passata per id.
   *
   * @param personId l'id della persona.
   * @return la persona corrispondente all'id passato come parametro.
   */
  public Person getPersonById(Long personId) {

    final QPerson person = QPerson.person;
    final QContract contract = QContract.contract;
    final QContractStampProfile csp = QContractStampProfile.contractStampProfile;

    return getQueryFactory().selectFrom(person)
        .leftJoin(person.contracts, contract).fetchAll()
        .leftJoin(contract.contractStampProfile, csp).fetchAll()
        .where(person.id.eq(personId)).distinct()
        .fetchOne();
  }

  /**
   * Persona tramite la matricola.
   *
   * @param number la matricola passata come parametro.
   * @return la persona corrispondente alla matricola passata come parametro.
   */
  public Person getPersonByNumber(String number) {
    final QPerson person = QPerson.person;
    return getQueryFactory().selectFrom(person).where(person.number.eq(number)).fetchOne();
  }


  /**
   * La lista di persone con matricola valida associata. Se office present le sole persone di quella
   * sede.
   *
   * @return persone con matricola valida
   */
  public List<Person> getPersonsWithNumber(Optional<Office> office) {

    final QPerson person = QPerson.person;
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;

    BooleanBuilder condition =
        new BooleanBuilder(person.number.isNotNull().and(person.number.isNotEmpty()));

    if (office.isPresent()) {
      condition.and(personsOffices.office.eq(office.get()));
    }

    return getQueryFactory().selectFrom(person)
        .leftJoin(person.personsOffices, personsOffices)
        .where(condition).orderBy(person.number.asc()).fetch();
  }

  /**
   * Persona (se esiste) a partire dalla mail.
   *
   * @param email la mail della persona.
   * @return la persona che ha associata la mail email.
   */
  public Optional<Person> byEmail(String email) {

    final QPerson person = QPerson.person;

    final Person result = getQueryFactory().selectFrom(person).where(person.email.eq(email))
        .fetchOne();

    return Optional.fromNullable(result);
  }


  /**
   * Persona (se esiste) a partire dall'eppn.
   *
   * @param eppn il parametro eppn per autenticazione via shibboleth.
   * @return la persona se esiste associata al parametro eppn.
   */
  public Optional<Person> byEppn(String eppn) {

    final QPerson person = QPerson.person;

    final Person result = 
        getQueryFactory().selectFrom(person)
        .where(person.eppn.equalsIgnoreCase(eppn))
        .fetchOne();

    return Optional.fromNullable(result);
  }

  /**
   * Persona (se esiste) a partire dal codice fiscale.
   *
   * @param fiscalCode il codice fiscale della persona.
   * @return la persona se esiste, Optional.absent() altrimenti.
   */
  public Optional<Person> byFiscalCode(String fiscalCode) {
    final QPerson person = QPerson.person;
    final Person result = getQueryFactory().selectFrom(person)
        .where(person.fiscalCode.equalsIgnoreCase(fiscalCode))
        .fetchOne();

    return Optional.fromNullable(result);
  }

  /**
   * Persona (se esiste) a partire dal campo eppn o dal campo email.
   * Il campo eppn viene usato come prioritario se passato, poi viene
   * utilizzato il campo email se passato, infine il campo perseoId. 
   *
   * @param eppn il campo eppn associato alla persona.
   * @param email il campo eppn associato alla persona.
   * @param perseoId il campo perseoId associato alla persona.
   * @return la persona se esiste associata al parametro eppn.
   */
  public Optional<Person> byIdOrEppnOrEmailOrPerseoIdOrFiscalCodeOrNumber(
      Long id, String eppn, String email, Long perseoId, String fiscalCode, String number) {
    if (id == null && eppn == null && email == null && perseoId == null 
        && fiscalCode == null && number == null) {
      return Optional.absent();
    }
    if (id != null) {
      return Optional.fromNullable(getPersonById(id));
    }
    if (!Strings.isNullOrEmpty(eppn)) {
      return byEppn(eppn);
    } 
    if (!Strings.isNullOrEmpty(email)) {
      return byEmail(email);  
    }
    if (perseoId != null) {
      return Optional.fromNullable(getPersonByPerseoId(perseoId));  
    }
    if (!Strings.isNullOrEmpty(fiscalCode)) {
      return byFiscalCode(fiscalCode);
    }
    if (!Strings.isNullOrEmpty(number)) {
      return Optional.fromNullable(getPersonByNumber(number));
    }
    return Optional.absent();
  }

  /**
   * Persona (se esiste) a partire dal perseoId.
   *
   * @param perseoId l'id della persona sull'applicazione perseo.
   * @return la persona identificata dall'id con cui è salvata sul db di perseo.
   */
  public Person getPersonByPerseoId(Long perseoId) {

    final QPerson person = QPerson.person;

    return getQueryFactory().selectFrom(person).where(person.perseoId.eq(perseoId)).fetchOne();
  }

  /**
   * Il proprietario del badge.
   *
   * @param badgeNumber codice del badge
   * @param badgeReader badge reader
   * @return il proprietario del badge
   */
  public Person getPersonByBadgeNumber(String badgeNumber, BadgeReader badgeReader) {

    final QPerson person = QPerson.person;
    final QBadge badge = QBadge.badge;

    //Rimuove tutti gli eventuali 0 iniziali alla stringa
    // http://stackoverflow.com/questions/2800739/how-to-remove-leading-zeros-from-alphanumeric-text
    final String cleanedBadgeNumber = badgeNumber.replaceFirst("^0+(?!$)", "");

    return getQueryFactory().selectFrom(person)
        .leftJoin(person.badges, badge)
        .where(badge.badgeReader.eq(badgeReader)
            .andAnyOf(badge.code.eq(badgeNumber), badge.code.eq(cleanedBadgeNumber)))
        .fetchOne();
  }

  /**
   * Lista di persone per tipo di reperibilità associata.
   *
   * @param type il tipo della reperibilità.
   * @return la lista di persone in reperibilità con tipo type.
   */
  public List<Person> getPersonForReperibility(Long type) {

    final QPerson person = QPerson.person;
    final QPersonReperibility rep = QPersonReperibility.personReperibility;

    return getQueryFactory().selectFrom(person)
        .leftJoin(person.reperibility, rep)
        .where(rep.personReperibilityType.id.eq(type)
            .and(rep.startDate.isNull()
                .or(rep.startDate.loe(LocalDate.now())
                    .and(rep.endDate.isNull()
                        .or(rep.endDate.goe(LocalDate.now()))))))
        .fetch();
  }

  /**
   * Lista di persone per tipo di turno associato.
   *
   * @param type il tipo di turno
   * @return la lista di persone che hanno come tipo turno quello passato come parametro.
   */
  public List<Person> getPersonForShift(String type, LocalDate date) {

    final QPerson person = QPerson.person;
    final QPersonShiftShiftType psst = QPersonShiftShiftType.personShiftShiftType;
    final QPersonShift ps = QPersonShift.personShift;

    return getQueryFactory().selectFrom(person).leftJoin(person.personShifts, ps)
        .leftJoin(ps.personShiftShiftTypes, psst)
        .where(psst.shiftType.type.eq(type)
            .and(ps.beginDate.loe(date).andAnyOf(ps.endDate.isNull(), ps.endDate.goe(date)))
            .and(psst.beginDate.isNull().or(psst.beginDate.loe(LocalDate.now()))
                .and(psst.endDate.isNull().or(psst.endDate.goe(LocalDate.now())))))
        .fetch();
  }


  /**
   * Le persone attive della sede con il campo matricola popolato.
   *
   * @return persone
   */
  public List<Person> activeWithNumber(Office office) {

    final QPerson person = QPerson.person;

    return personQuery(Optional.absent(), Sets.newHashSet(office), false,
        Optional.fromNullable(LocalDate.now()), Optional.fromNullable(LocalDate.now()),
        true, Optional.absent(), Optional.absent(), false)
        .where(person.number.isNotNull()).fetch();
  }

  /**
   * La query per la ricerca delle persone. Versione con JPQLQuery injettata per selezionare le
   * fetch da utilizzare con la proiezione desiderata.
   */
  private JPQLQuery<?> personQuery(JPQLQuery<?> injectedQuery, Optional<String> name,
      Set<Office> offices,
      boolean onlyTechnician, Optional<LocalDate> start, Optional<LocalDate> end,
      boolean onlyOnCertificate, Optional<CompetenceCode> compCode,
      boolean onlySynchronized, boolean onlyPeopleInTelework) {

    final BooleanBuilder condition = new BooleanBuilder();

    filterOffices(condition, offices, start, end, true);
    filterOnlyTechnician(condition, onlyTechnician);
    condition.and(new QFilters().filterNameFromPerson(QPerson.person, name));
    filterOnlyOnCertificate(condition, onlyOnCertificate);
    filterContract(condition, start, end);
    if (start.isPresent()) {
      filterCompetenceCodeEnabled(condition, compCode, start.get());
    }

    filterOnlySynchronized(condition, onlySynchronized);
    filterOnlyTelework(condition, onlyPeopleInTelework);

    return injectedQuery.where(condition);

  }

  /**
   * La query per la ricerca delle persone. Versione da utilizzare per proiezione esatta Person.
   *
   * @param name l'eventuale nome
   * @param offices la lista degli uffici
   * @param onlyTechnician true se si chiedono solo i tecnici, false altrimenti
   * @param start da quando iniziare la ricerca
   * @param end quando terminare la ricerca
   * @param onlyOnCertificate true se si chiedono solo gli strutturati, false altrimenti
   * @param compCode il codice di competenza
   * @param personInCharge il responsabile della persona
   * @param onlySynchronized le persone con perseoId valorizzato
   * @return la lista delle persone corrispondente ai criteri di ricerca
   */
  private JPQLQuery<Person> personQuery(Optional<String> name, Set<Office> offices,
      boolean onlyTechnician,
      Optional<LocalDate> start, Optional<LocalDate> end, boolean onlyOnCertificate,
      Optional<CompetenceCode> compCode, Optional<Person> personInCharge,
      boolean onlySynchronized) {

    final QPerson person = QPerson.person;
    final QContract contract = QContract.contract;
    final QAffiliation affiliation = QAffiliation.affiliation;
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;

    final JPQLQuery<Person> query = getQueryFactory().selectFrom(person)

        // join one to many or many to many (only one bag fetchable!!!)
        .leftJoin(person.contracts, contract)
        .leftJoin(person.personCompetenceCodes, QPersonCompetenceCodes.personCompetenceCodes)
        .leftJoin(person.user, QUser.user)
        .leftJoin(person.affiliations, affiliation)
        .leftJoin(affiliation.group, QGroup.group)
        // join one to one
        .leftJoin(person.reperibility, QPersonReperibility.personReperibility).fetchJoin()
        .leftJoin(
            person.personHourForOvertime, QPersonHourForOvertime.personHourForOvertime).fetchJoin()
        .leftJoin(person.qualification).fetchJoin()
        .leftJoin(person.personsOffices, personsOffices)
        // order by
        .orderBy(person.surname.asc(), person.name.asc())
        .distinct();

    final BooleanBuilder condition = new BooleanBuilder();

    filterOffices(condition, offices, start, end, false);
    filterOnlyTechnician(condition, onlyTechnician);
    condition.and(new QFilters().filterNameFromPerson(QPerson.person, name));
    filterOnlyOnCertificate(condition, onlyOnCertificate);
    filterContract(condition, start, end);
    if (start.isPresent()) {
      filterCompetenceCodeEnabled(condition, compCode, start.get());
    }
    filterPersonInCharge(condition, personInCharge);
    filterOnlySynchronized(condition, onlySynchronized);
    return query.where(condition);
  }


  /**
   * Filtro sugli uffici.
   */
  private void filterOffices(BooleanBuilder condition, Set<Office> offices, 
      Optional<LocalDate> start, Optional<LocalDate> end, boolean forLiteList) {

    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    if (offices != null && !offices.isEmpty()) {
      condition.and(personsOffices.office.in(offices));
    }
    if (!forLiteList) {
      if (start.isPresent()) {
        condition.and(personsOffices.beginDate.loe(end.get()));
      }
      if (end.isPresent()) {
        condition.andAnyOf(personsOffices.endDate.goe(start.get()), 
            personsOffices.endDate.isNull());
      }      
    } else {
      if (start.isPresent() && end.isPresent()) {
        condition.and(personsOffices.beginDate.loe(end.get()));
        condition.andAnyOf(personsOffices.endDate.isNull(), 
            personsOffices.endDate.goe(start.get()));
      }

    }

  }

  /**
   * Filtro sulle date contrattuali.
   *
   * @param condition il booleanbuilder contenente eventuali altre condizioni
   * @param start absent() no limit
   * @param end absent() no limit
   */
  private void filterContract(BooleanBuilder condition, Optional<LocalDate> start,
      Optional<LocalDate> end) {

    final QContract contract = QContract.contract;

    if (end.isPresent()) {

      condition.and(contract.beginDate.loe(end.get()));
    }

    if (start.isPresent()) {
      // entrambe le date nulle
      condition.andAnyOf(contract.endContract.isNull().and(contract.endDate.isNull()),
          // una nulla e l'altra successiva
          contract.endContract.isNull().and(contract.endDate.goe(start.get())),
          // viceversa rispetto alla precedente
          contract.endDate.isNull().and(contract.endContract.goe(start.get())),
          //entrambe valorizzate ed entrambe successive
          contract.endDate.goe(start.get()).and(contract.endContract.goe(start.get()))
          );
    }
  }


  /**
   * Filtra solo i livelli IV-VIII.
   *
   * @param condition la condizione
   * @param value true se vogliamo solo i tecnici/amministrativi, false altrimenti
   */
  private void filterOnlyTechnician(BooleanBuilder condition, boolean value) {
    if (value) {
      final QPerson person = QPerson.person;
      condition.and(person.qualification.qualification.gt(3));
    }
  }

  /**
   * Filtra solo le persone che devono andare su attestati.
   *
   * @param condition la condizione
   * @param value true se vogliamo quelli che vanno su attestati, false altrimenti
   */
  private void filterOnlyOnCertificate(BooleanBuilder condition, boolean value) {
    if (value) {
      final QContract contract = QContract.contract;
      condition.and(contract.contractType.eq(ContractType.structured_public_administration));
    }
  }

  private void filterOnlyTelework(BooleanBuilder condition, boolean value) {
    if (value) {
      final QPersonConfiguration conf = QPersonConfiguration.personConfiguration;
      condition.and(conf.epasParam.eq(EpasParam.TELEWORK_STAMPINGS)
          .and(conf.fieldValue.eq("true")));
    }
  }

  /**
   * Filtra le persone che appartengono al gruppo di lavoro del personInCharge.
   *
   * @param condition la condizione
   * @param personInCharge il responsabile se presente
   */
  private void filterPersonInCharge(BooleanBuilder condition, Optional<Person> personInCharge) {
    if (personInCharge.isPresent()) {
      final QGroup group = QGroup.group;
      condition.and(group.manager.eq(personInCharge.get()));
    }
  }

  /**
   * Filtra le persone sincronizzate con perseo.
   *
   * @param condition la condizione
   * @param value true se vogliamo solo i sincronizzati con perseo, false altrimenti
   */
  private void filterOnlySynchronized(BooleanBuilder condition, boolean value) {
    if (value) {
      final QPerson person = QPerson.person;
      condition.and(person.perseoId.isNotNull());
    }
  }


  /**
   * Filtro su competenza abilitata.
   */
  private void filterCompetenceCodeEnabled(BooleanBuilder condition,
      Optional<CompetenceCode> compCode, LocalDate date) {

    if (compCode.isPresent()) {
      final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
      condition.and(pcc.competenceCode.eq(compCode.get())).and(pcc.beginDate.loe(date)
          .andAnyOf(pcc.endDate.goe(date), pcc.endDate.isNull()));
    }
  }

  /**
   * Filtro su codice competenza abilitato appartenente a gruppo.
   *
   * @param condition la condition che mi porto dietro da altre restrizioni
   * @param group il gruppo da controllare
   * @param date la data da cui cercare
   */
  private void filterCompetenceCodeGroupEnabled(BooleanBuilder condition,
      Optional<CompetenceCodeGroup> group, LocalDate date) {
    if (group.isPresent()) {
      final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
      condition.and(pcc.competenceCode.in(group.get().competenceCodes)
          .and(pcc.beginDate.loe(date)
              .andAnyOf(pcc.endDate.goe(date), pcc.endDate.isNull())));
    }
  }

  /**
   * Filtro sui tempi determinati.
   *
   * @param condition la condition che mi porto dietro da altre restrizioni
   */
  private void filterTemporary(BooleanBuilder condition) {
    final QContract contract = QContract.contract;
    condition.and(contract.endDate.isNotNull());
  }


  /**
   * Importa tutte le informazioni della persona necessarie alla business logic ottimizzando il
   * numero di accessi al db.
   */
  public Person fetchPersonForComputation(Long id, Optional<LocalDate> begin,
      Optional<LocalDate> end) {

    QPerson qperson = QPerson.person;

    // Fetch della persona e dei suoi contratti

    final Person person = getQueryFactory().selectFrom(qperson).leftJoin(qperson.contracts)
        .fetchJoin()
        .where(qperson.id.eq(id)).distinct()
        .fetchOne();

    fetchContracts(Sets.newHashSet(person), begin, end);

    // Fetch dei buoni pasto (non necessaria, una query)
    // Fetch dei personday

    personDayDao.getPersonDayInPeriod(person, begin.get(), end);

    return person;

  }

  /**
   * Fetch di tutti dati dei contratti attivi nella finestra temporale specificata. Si può filtrare
   * su una specifica persona.
   */
  private void fetchContracts(Set<Person> person, Optional<LocalDate> start,
      Optional<LocalDate> end) {

    // Fetch dei contratti appartenenti all'intervallo
    QContract contract = QContract.contract;
    QContractWorkingTimeType cwtt = QContractWorkingTimeType.contractWorkingTimeType;
    QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;

    final BooleanBuilder condition = new BooleanBuilder(contract.person.in(person));
    filterContract(condition, start, end);

    List<Contract> contracts = getQueryFactory()
        .selectFrom(contract)
        .leftJoin(contract.contractMonthRecaps).fetchJoin()
        .leftJoin(contract.contractStampProfile).fetchJoin()
        .leftJoin(contract.contractWorkingTimeType, cwtt).fetchJoin()
        .where(condition)
        .orderBy(contract.beginDate.asc()).distinct()
        .fetch();

    if (!person.isEmpty()) {
      // Fetch dei tipi orario associati ai contratti (verificare l'utilità)
      getQueryFactory().selectFrom(cwtt)
      .leftJoin(cwtt.workingTimeType, wtt).fetchJoin()
      .where(cwtt.contract.in(contracts).and(cwtt.contract.person.in(person))).distinct()
      .fetch();
    }
  }

  /**
   * Genera la lista di PersonLite contenente le persone attive nel mese specificato appartenenti ad
   * un office in offices. Importante: utile perchè non sporca l'entity manager con oggetti
   * parziali.
   */
  public List<PersonLite> liteList(
      Set<Office> offices, int year, int month, boolean onlyPeopleInTelework) {

    final QPerson person = QPerson.person;
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;

    Optional<LocalDate> beginMonth = Optional.fromNullable(new LocalDate(year, month, 1));
    Optional<LocalDate> endMonth =
        Optional.fromNullable(beginMonth.get().dayOfMonth().withMaximumValue());

    JPQLQuery<?> lightQuery =
        getQueryFactory().from(person)
        .leftJoin(person.contracts, QContract.contract)
        .leftJoin(person.personConfigurations, QPersonConfiguration.personConfiguration)
        .leftJoin(person.personsOffices, personsOffices)
        .orderBy(person.surname.asc(), person.name.asc()).distinct();

    lightQuery = personQuery(lightQuery, Optional.absent(), offices, false, beginMonth,
        endMonth, true, Optional.absent(), false, onlyPeopleInTelework);

    return lightQuery
        .select(Projections.bean(PersonLite.class, person.id, person.name, person.surname)).fetch();
  }

  /**
   * Questo metodo ci è utile per popolare le select delle persone.
   *
   * @param offices gli uffici di appartenenza delle persone richieste
   * @return la Lista delle persone appartenenti agli uffici specificati
   */
  public List<PersonLite> peopleInOffices(Set<Office> offices) {

    final QPerson person = QPerson.person;

    JPQLQuery<?> lightQuery =
        getQueryFactory().from(person).leftJoin(person.contracts, QContract.contract)
        .orderBy(person.surname.asc(), person.name.asc()).distinct();

    lightQuery = personQuery(lightQuery, Optional.absent(), offices, false, Optional.absent(),
        Optional.absent(), true, Optional.absent(), false, false);

    return lightQuery
        .select(Projections.bean(PersonLite.class, person.id, person.name, person.surname))
        .fetch();
  }

  /**
   * Query ad hoc fatta per i Jobs che inviano le email di alert per segnalare problemi sui giorni.
   *
   * @return la Lista di tutte le persone con i requisiti adatti per poter effettuare le
   *        segnalazioni dei problemi.
   */
  public List<Person> eligiblesForSendingAlerts() {

    final QPerson person = QPerson.person;
    final QContract contract = QContract.contract;
    final QOffice office = QOffice.office;
    final QConfiguration config = QConfiguration.configuration;
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;

    final BooleanBuilder baseCondition = new BooleanBuilder();

    // Requisiti della Persona
    baseCondition.and(person.wantEmail.isTrue()); // la persona non ha l'invio mail disabilitato

    // Requisiti sul contratto
    // il contratto è attivo per l'invio attestati
    baseCondition.and(contract.contractType.eq(ContractType.structured_public_administration));
    // il contratto deve essere attivo oggi
    final LocalDate today = LocalDate.now();
    filterContract(baseCondition, Optional.of(today), Optional.of(today));

    final BooleanBuilder sendEmailCondition = new BooleanBuilder();
    // Requisiti sulla configurazione dell'office
    // L'ufficio ha l'invio mail attivo
    sendEmailCondition
    .and(config.epasParam.eq(EpasParam.SEND_EMAIL).and(config.fieldValue.eq("true")));
    // Se l'ufficio ha il parametro per l'autocertificazione disabilitato coinvolgo
    // tutti i dipendenti

    final BooleanBuilder trAutoCertificationDisabledCondition = new BooleanBuilder();
    trAutoCertificationDisabledCondition.and(config.epasParam.eq(EpasParam.TR_AUTOCERTIFICATION)
        .and(config.fieldValue.eq("false")));

    // Se il parametro è attivo escludo i tecnologi e i ricercatori
    final BooleanBuilder trAutoCertificationEnabledCondition = new BooleanBuilder();
    trAutoCertificationEnabledCondition.and(config.epasParam.eq(EpasParam.TR_AUTOCERTIFICATION)
        .and(config.fieldValue.eq("true")).and(person.qualification.qualification.gt(3)));

    final JPQLQuery<Long> personSendEmailTrue = JPAExpressions.selectFrom(person)
        .leftJoin(person.contracts, contract)
        .leftJoin(person.personsOffices, personsOffices)
        .leftJoin(office.configurations, config)
        .where(personsOffices.office.eq(office).and(baseCondition), sendEmailCondition)
        .select(person.id);

    final JPQLQuery<Long> personAutocertDisabled = JPAExpressions.selectFrom(person)
        .leftJoin(person.contracts, contract)
        .leftJoin(person.personsOffices, personsOffices)
        //.leftJoin(person.office, office)
        .leftJoin(office.configurations, config)
        .where(personsOffices.office.eq(office).and(baseCondition), 
            trAutoCertificationDisabledCondition)
        .select(person.id);

    final JPQLQuery<Long> autocertEnabledOnlyTecnicians = JPAExpressions.selectFrom(person)
        .leftJoin(person.contracts, contract)
        .leftJoin(person.personsOffices, personsOffices)
        //.leftJoin(person.office, office)
        .leftJoin(office.configurations, config)
        .where(personsOffices.office.eq(office).and(baseCondition), 
            trAutoCertificationEnabledCondition)
        .select(person.id);

    return queryFactory.selectFrom(person)
        .where(
            person.id.in(personSendEmailTrue),
            person.id.in(personAutocertDisabled)
            .or(person.id.in(autocertEnabledOnlyTecnicians)))
        .distinct().fetch();
  }

  /**
   * Restituisce tutti i tecnologi e ricercatori delle sedi sulle quali è abilitata
   * l'autocertificazione, verificando i requisiti per l'invio della mail (contratto attivo, in
   * attestati, parametro wantEmail true etc...)
   *
   * @return La lista contenente tutti i tecnologi e ricercatori delle sedi nelle quali è attiva
   *        l'autocertificazione.
   */
  public List<Person> trWithAutocertificationOn() {

    final QPerson person = QPerson.person;
    final QContract contract = QContract.contract;
    final QOffice office = QOffice.office;
    final QConfiguration config = QConfiguration.configuration;
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    final BooleanBuilder baseCondition = new BooleanBuilder();

    // Requisiti della Persona
    baseCondition.and(person.wantEmail.isTrue()); // la persona non ha l'invio mail disabilitato

    // Requisiti sul contratto
    // il contratto è attivo per l'invio attestati
    baseCondition.and(contract.contractType.eq(ContractType.structured_public_administration));
    // il contratto deve essere attivo oggi
    final LocalDate today = LocalDate.now();
    filterContract(baseCondition, Optional.of(today), Optional.of(today));

    final BooleanBuilder sendEmailCondition = new BooleanBuilder();
    // Requisiti sulla configurazione dell'office
    // L'ufficio ha l'invio mail attivo
    sendEmailCondition
    .and(config.epasParam.eq(EpasParam.SEND_EMAIL).and(config.fieldValue.eq("true")));

    // Prendo solo i tecnologi e i ricercatori delle sedi dove è stato attivato il parametro
    // per l'autocertificazione
    final BooleanBuilder trAutoCertificationEnabledCondition = new BooleanBuilder();
    trAutoCertificationEnabledCondition.and(config.epasParam.eq(EpasParam.TR_AUTOCERTIFICATION)
        .and(config.fieldValue.eq("true")).and(person.qualification.qualification.loe(3)));

    final JPQLQuery<Long> personSendEmailTrue = JPAExpressions.selectFrom(person)
        .leftJoin(person.contracts, contract)
        .leftJoin(person.personsOffices, personsOffices)
        //.leftJoin(person.office, office)
        .leftJoin(office.configurations, config)
        .where(personsOffices.office.eq(office).and(baseCondition), sendEmailCondition)
        .select(person.id);

    final JPQLQuery<Long> trAutocertEnabled = JPAExpressions.selectFrom(person)
        .leftJoin(person.contracts, contract)
        .leftJoin(person.personsOffices, personsOffices)
        //.leftJoin(person.office, office)
        .leftJoin(office.configurations, config)
        .where(personsOffices.office.eq(office).and(baseCondition), 
            trAutoCertificationEnabledCondition)
        .select(person.id);

    return queryFactory.selectFrom(person)
        .where(
            person.id.in(personSendEmailTrue),
            person.id.in(trAutocertEnabled))
        .distinct().fetch();
  }

  /**
   * La lista di persone senza configurazione.
   *
   * @return la lista di persone senza configurazione.
   */
  public List<Person> peopleWithoutConfiguration() {
    final QPerson person = QPerson.person;
    return getQueryFactory()
        .selectFrom(person).where(person.personConfigurations.isEmpty()).fetch();   

  }

  /**
   * Dto contenente le sole informazioni della persona richieste dalla select nel template menu.
   */
  public static class PersonLite {

    public Long id;
    public String name;
    public String surname;

    public Person person;

    /**
     * Costruttore.
     *
     * @param id id
     * @param name nome
     * @param surname cognome
     */
    public PersonLite(Long id, String name, String surname) {
      this.id = id;
      this.name = name;
      this.surname = surname;
    }

    @Override
    public String toString() {
      return surname + ' ' + name;
    }

  }



}