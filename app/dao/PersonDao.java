package dao;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.types.Projections;
import com.mysema.query.types.QBean;

import dao.filter.QFilters;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;
import helpers.jpa.PerseoModelQuery;
import helpers.jpa.PerseoModelQuery.PerseoSimpleResults;

import it.cnr.iit.epas.DateInterval;

import models.BadgeReader;
import models.CompetenceCode;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.query.QBadge;
import models.query.QContract;
import models.query.QContractStampProfile;
import models.query.QContractWorkingTimeType;
import models.query.QPerson;
import models.query.QPersonDay;
import models.query.QPersonHourForOvertime;
import models.query.QPersonReperibility;
import models.query.QPersonShift;
import models.query.QPersonShiftShiftType;
import models.query.QUser;
import models.query.QVacationPeriod;
import models.query.QWorkingTimeType;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

/**
 * DAO per le person.
 *
 * @author marco
 */
public final class PersonDao extends DaoBase {


  @Inject
  public OfficeDao officeDao;
  @Inject
  public PersonDayDao personDayDao;

  @Inject
  PersonDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }



  public List<Person> getActivePersonInMonth(Set<Office> offices, YearMonth yearMonth) {
    final QPerson person = QPerson.person;
    int year = yearMonth.getYear();
    int month = yearMonth.getMonthOfYear();

    Optional<LocalDate> beginMonth = Optional.fromNullable(new LocalDate(year, month, 1));
    Optional<LocalDate> endMonth =
        Optional.fromNullable(beginMonth.get().dayOfMonth().withMaximumValue());

    JPQLQuery query = personQuery(Optional.<String>absent(), offices, false, beginMonth, endMonth,
        true, Optional.<CompetenceCode>absent(), Optional.<Person>absent());

    return ModelQuery.simpleResults(query, person).list();
  }

 

  /**
   * La lista di persone una volta applicati i filtri dei parametri.
   * 
   * @param name
   * @param offices
   * @param onlyTechnician
   * @param start
   * @param end
   * @param onlyOnCertificate
   * @return
   */
  public PerseoSimpleResults<Person> listPerseo(Optional<String> name, Set<Office> offices,
      boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {

    final QPerson person = QPerson.person;

    return PerseoModelQuery.wrap(
        // JPQLQuery
        personQuery(name, offices, onlyTechnician, Optional.fromNullable(start),
            Optional.fromNullable(end), onlyOnCertificate, Optional.<CompetenceCode>absent(),
            Optional.<Person>absent()),
        // Expression
        person);
  }


  /**
   * La lista di persone una volta applicati i filtri dei parametri.
   * 
   * @param name
   * @param offices
   * @param onlyTechnician
   * @param start
   * @param end
   * @param onlyOnCertificate
   * @return
   */
  public SimpleResults<Person> list(Optional<String> name, Set<Office> offices,
      boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {

    final QPerson person = QPerson.person;

    return ModelQuery.simpleResults(
        // JPQLQuery
        personQuery(name, offices, onlyTechnician, Optional.fromNullable(start),
            Optional.fromNullable(end), onlyOnCertificate, Optional.<CompetenceCode>absent(),
            Optional.<Person>absent()),
        // Expression
        person);
  }



  /**
   * Permette la fetch automatica di tutte le informazioni delle persone filtrate.
   * 
   * TODO: e' usata solo in Persons.list ma se serve in altri metodi rendere parametrica la funzione
   * PersonDao.list.
   * 
   * @param name l'eventuale nome da filtrare
   * @param offices la lista degli uffici su cui cercare
   * @param onlyTechnician true se cerco solo i tecnici, false altrimenti
   * @param start da quando iniziare la ricerca
   * @param end quando terminare la ricerca
   * @param onlyOnCertificate true se voglio solo gli strutturati, false altrimenti
   * @param badgeReader opzionale 
   * @return la lista delle persone trovate con queste retrizioni
   */
  public SimpleResults<Person> listFetched(Optional<String> name, Set<Office> offices,
      boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {

    final QPerson person = QPerson.person;

    JPQLQuery query = personQuery(name, offices, onlyTechnician, Optional.fromNullable(start),
        Optional.fromNullable(end), onlyOnCertificate, Optional.<CompetenceCode>absent(),
        Optional.<Person>absent());

    SimpleResults<Person> result = ModelQuery.simpleResults(
        // JPQLQuery
        query,
        // Expression
        person);

    fetchContracts(Optional.<Person>absent(), Optional.fromNullable(start),
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
  public PerseoSimpleResults<Person> listForCompetence(CompetenceCode competenceCode, 
      Optional<String> name, Set<Office> offices, boolean onlyTechnician, 
      LocalDate start, LocalDate end, Optional<Person> personInCharge) {

    Preconditions.checkState(!offices.isEmpty());
    Preconditions.checkNotNull(competenceCode);

    final QPerson person = QPerson.person;

    return PerseoModelQuery.wrap(personQuery(name, offices, onlyTechnician,
        Optional.fromNullable(start), Optional.fromNullable(end), true,
        Optional.fromNullable(competenceCode), personInCharge), person);

  }

  /**
   * L'ultimo contratto inserito in ordine di data inizio. (Tendenzialmente quello attuale)
   * 
   * @param person
   * @return
   */
  public Optional<Contract> getLastContract(Person person) {

    final QContract contract = QContract.contract;

    final JPQLQuery query = getQueryFactory().from(contract).where(contract.person.eq(person))
        .orderBy(contract.beginDate.desc());

    List<Contract> contracts = query.list(contract);
    if (contracts.size() == 0) {
      return Optional.<Contract>absent();
    }
    return Optional.fromNullable(contracts.get(0));

  }

  /**
   * Il contratto precedente in ordine temporale rispetto a quello passato come argomento.
   * 
   * @param c
   * @return
   */
  public Contract getPreviousPersonContract(Contract c) {

    final QContract contract = QContract.contract;

    final JPQLQuery query = getQueryFactory().from(contract).where(contract.person.eq(c.person))
        .orderBy(contract.beginDate.desc());

    List<Contract> contracts = query.list(contract);

    final int indexOf = contracts.indexOf(c);
    if (indexOf + 1 < contracts.size()) {
      return contracts.get(indexOf + 1);
    } else {
      return null;
    }
  }

  /**
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

    return getQueryFactory().from(contract).where(conditions).orderBy(contract.beginDate.asc())
        .list(contract);
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

    final JPQLQuery query = getQueryFactory().from(qpd).orderBy(qpd.date.asc());

    final BooleanBuilder condition = new BooleanBuilder();
    condition.and(qpd.person.eq(person).and(qpd.date.goe(interval.getBegin()))
        .and(qpd.date.loe(interval.getEnd())));

    if (onlyWithMealTicket) {
      condition.and(qpd.isTicketAvailable.eq(true));
    }
    query.where(condition);

    return query.list(qpd);
  }

  /**
   * @param personId l'id della persona.
   * @return la persona corrispondente all'id passato come parametro.
   */
  public Person getPersonById(Long personId) {

    final QPerson person = QPerson.person;
    final QContract contract = QContract.contract;
    final QContractStampProfile csp = QContractStampProfile.contractStampProfile;

    final JPQLQuery query = getQueryFactory().from(person).leftJoin(person.contracts, contract)
        .fetchAll().leftJoin(contract.contractStampProfile, csp).fetchAll()
        .where(person.id.eq(personId)).distinct();

    return query.singleResult(person);
  }

  /**
   * @param email  della persona
   * @return la persona corrispondente alla email.
   */
  @Deprecated // email non è un campo univoco... decidere
  public Person getPersonByEmail(String email) {

    final QPerson person = QPerson.person;

    final JPQLQuery query = getQueryFactory().from(person).where(person.email.eq(email));

    return query.singleResult(person);
  }

  /**
   * @param number la matricola passata come parametro.
   * @return la persona corrispondente alla matricola passata come parametro.
   */
  @Deprecated
  public Person getPersonByNumber(Integer number, Optional<Set<Office>> officeList) {

    final BooleanBuilder condition = new BooleanBuilder();
    final QPerson person = QPerson.person;
    if (officeList.isPresent()) {
      condition.and(person.office.in(officeList.get()));
    }

    condition.and(person.number.eq(number));

    final JPQLQuery query = getQueryFactory().from(person).where(condition);

    return query.singleResult(person);
  }

  public Person getPersonByNumber(Integer number) {
    return getPersonByNumber(number, Optional.<Set<Office>>absent());
  }

  /**
   * @return la lista di persone che hanno una matricola associata.
   */
  public List<Person> getPersonsByNumber() {

    final QPerson person = QPerson.person;

    final JPQLQuery query =
        getQueryFactory().from(person).where(person.number.isNotNull().and(person.number.ne(0)));
    query.orderBy(person.number.asc());
    return query.list(person);
  }

  /**
   * @param email la mail della persona.
   * @return la persona che ha associata la mail email.
   */
  public Optional<Person> byEmail(String email) {

    final QPerson person = QPerson.person;

    final JPQLQuery query = getQueryFactory().from(person).where(person.email.eq(email));

    return Optional.fromNullable(query.singleResult(person));
  }


  /**
   * @param eppn il parametro eppn per autenticazione via shibboleth.
   * @return la persona se esiste associata al parametro eppn.
   */
  public Optional<Person> byEppn(String eppn) {

    final QPerson person = QPerson.person;

    final JPQLQuery query = getQueryFactory().from(person).where(person.eppn.eq(eppn));

    return Optional.fromNullable(query.singleResult(person));
  }

  /**
   * @param perseoId l'id della persona sull'applicazione perseo.
   * @return la persona identificata dall'id con cui è salvata sul db di perseo.
   */
  public Person getPersonByPerseoId(Integer perseoId) {

    final QPerson person = QPerson.person;

    final JPQLQuery query = getQueryFactory().from(person).where(person.iId.eq(perseoId));

    return query.singleResult(person);
  }

  /**
   * @param oldId il vecchio id.
   * @return la persona associata al vecchio id (se presente in anagrafica) passato come parametro
   */
  public Person getPersonByOldID(Long oldId, Optional<Set<Office>> offices) {

    final BooleanBuilder condition = new BooleanBuilder();
    final QPerson person = QPerson.person;
    if (offices.isPresent()) {
      condition.and(person.office.in(offices.get()));
    }

    condition.and(person.oldId.eq(oldId));


    final JPQLQuery query = getQueryFactory().from(person).where(condition);

    return query.singleResult(person);
  }

  /**
   * 
   * @param oldId  il vecchio id.
   * @return la persona associata al vecchio id presente nella vecchia anagrafica.
   */
  public Person getPersonByOldID(Long oldId) {
    return getPersonByOldID(oldId, Optional.<Set<Office>>absent());
  }

  /**
   * 
   * @param badgeNumber il numero badge.
   * @return la persona associata al badgeNumber passato come parametro.
   */
  public Person getPersonByBadgeNumber(String badgeNumber, Optional<BadgeReader> badgeReader) {

    final BooleanBuilder condition = new BooleanBuilder();
    final QPerson person = QPerson.person;
    final QBadge badge = QBadge.badge;
    final JPQLQuery query = getQueryFactory().from(person)
        .leftJoin(person.badges, badge);
    if (badgeReader.isPresent()) {
      condition.and(badge.badgeReader.eq(badgeReader.get()));
    }
    
    condition.and(badge.code.eq(badgeNumber));
    query.where(condition);
    

    return query.singleResult(person);
  }


  public Person getPersonByBadgeNumber(String badgeNumber) {
    return getPersonByBadgeNumber(badgeNumber, Optional.<BadgeReader>absent());
  }

  /**
   * 
   * @param type il tipo della reperibilità.
   * @return la lista di persone in reperibilità con tipo type.
   */
  public List<Person> getPersonForReperibility(Long type) {

    final QPerson person = QPerson.person;

    final JPQLQuery query = getQueryFactory().from(person)
        .where(person.reperibility.personReperibilityType.id.eq(type)
            .and(person.reperibility.startDate.isNull()
                .or(person.reperibility.startDate.loe(LocalDate.now())
                    .and(person.reperibility.endDate.isNull()
                        .or(person.reperibility.endDate.goe(LocalDate.now()))))));
    return query.list(person);

  }

  /**
   * 
   * @param type
   * @return la lista di persone che hanno come tipo turno quello passato come parametro
   */
  public List<Person> getPersonForShift(String type) {

    final QPerson person = QPerson.person;
    final QPersonShiftShiftType psst = QPersonShiftShiftType.personShiftShiftType;
    final QPersonShift ps = QPersonShift.personShift;

    final JPQLQuery query = getQueryFactory().from(person).leftJoin(person.personShift, ps)
        .leftJoin(ps.personShiftShiftTypes, psst)
        .where(psst.shiftType.type.eq(type)
            .and(psst.beginDate.isNull().or(psst.beginDate.loe(LocalDate.now()))
                .and(psst.endDate.isNull().or(psst.endDate.goe(LocalDate.now())))));
    return query.list(person);
  }

  /**
   * 
   * @param badgeReader il lettore badge su cui verificare chi timbra
   * @return la lista di persone che hanno un badge che timbra nel badgereader passato come parametro
   */
  public SimpleResults<Person> getPeopleFromBadgeReader(BadgeReader badgeReader){
    final QPerson person = QPerson.person;
    final QBadge badge = QBadge.badge;
    final JPQLQuery query = getQueryFactory().from(person).leftJoin(person.badges, badge)
        .where(badge.badgeReader.eq(badgeReader).and(badge.person.eq(person)))            
        .orderBy(person.surname.asc());
    SimpleResults<Person> result = ModelQuery.simpleResults(
        // JPQLQuery
        query,
        // Expression
        person);
    return result;
  }

  /**
   * 
   * @return il responsabile per la persona passata come parametro
   */
  public Person getPersonInCharge(Person p) {
    final QPerson person = QPerson.person;
    final JPQLQuery query = getQueryFactory().from(person).where(person.people.contains(p));
    return query.singleResult(person);
  }


  /**
   * La query per la ricerca delle persone. Versione con JPQLQuery injettata per selezionare le
   * fetch da utilizzare con la proiezione desiderata.
   * 
   * @param injectedQuery
   * @param name
   * @param offices
   * @param onlyTechnician
   * @param start
   * @param end
   * @param onlyOnCertificate
   * @param compCode
   * @return
   */
  private JPQLQuery personQuery(JPQLQuery injectedQuery, Optional<String> name, Set<Office> offices,
      boolean onlyTechnician, Optional<LocalDate> start, Optional<LocalDate> end,
      boolean onlyOnCertificate, Optional<CompetenceCode> compCode,
      Optional<BadgeReader> badgeReader) {

    final BooleanBuilder condition = new BooleanBuilder();

    filterOffices(condition, offices);
    filterOnlyTechnician(condition, onlyTechnician);
    condition.and(new QFilters().filterNameFromPerson(QPerson.person, name));
    filterOnlyOnCertificate(condition, onlyOnCertificate);
    filterContract(condition, start, end);
    filterCompetenceCodeEnabled(condition, compCode);
    //filterWithBadge(condition, badgeReader);

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
   * @param badgeReader il lettore badge su cui timbra
   * @return la lista delle persone corrispondente ai criteri di ricerca
   */
  private JPQLQuery personQuery(Optional<String> name, Set<Office> offices, boolean onlyTechnician,
      Optional<LocalDate> start, Optional<LocalDate> end, boolean onlyOnCertificate,
      Optional<CompetenceCode> compCode, Optional<Person> personInCharge) {

    final QPerson person = QPerson.person;
    final QContract contract = QContract.contract;

    final JPQLQuery query = getQueryFactory().from(person).leftJoin(person.contracts, contract)
        .fetch().leftJoin(person.user, QUser.user)
        .leftJoin(person.reperibility, QPersonReperibility.personReperibility).fetch()
        .leftJoin(person.personHourForOvertime, QPersonHourForOvertime.personHourForOvertime)
        .fetch().leftJoin(person.personShift, QPersonShift.personShift).fetch()
        .leftJoin(person.qualification).fetch().orderBy(person.surname.asc(), person.name.asc())
        .distinct();

    final BooleanBuilder condition = new BooleanBuilder();

    if (personInCharge.isPresent()) {
      condition.and(person.personInCharge.eq(personInCharge.get()));
    }
    filterOffices(condition, offices);
    //filterWithBadge(condition, badgeReader);
    filterOnlyTechnician(condition, onlyTechnician);
    condition.and(new QFilters().filterNameFromPerson(QPerson.person, name));
    filterOnlyOnCertificate(condition, onlyOnCertificate);
    filterContract(condition, start, end);
    filterCompetenceCodeEnabled(condition, compCode);

    return query.where(condition);
  }


  /**
   * La query per la ricerca delle persone. Versione con JPQLQuery injettata per selezionare le
   * fetch da utilizzare con la proiezione desiderata.
   */
  private JPQLQuery personQuery(
          JPQLQuery injectedQuery,
          Optional<String> name,
          Set<Office> offices,
          boolean onlyTechnician,
          Optional<LocalDate> start, Optional<LocalDate> end,
          boolean onlyOnCertificate,
          Optional<CompetenceCode> compCode,
          boolean withBadge) {

    final BooleanBuilder condition = new BooleanBuilder();

    filterOffices(condition, offices);
    filterOnlyTechnician(condition, onlyTechnician);
    condition.and(new QFilters().filterNameFromPerson(QPerson.person, name));
    filterOnlyOnCertificate(condition, onlyOnCertificate);
    filterContract(condition, start, end);
    filterCompetenceCodeEnabled(condition, compCode);
    
    return injectedQuery.where(condition);

  }

  /**
   * La query per la ricerca delle persone. Versione da utilizzare per proiezione esatta Person.
   */
  private JPQLQuery personQuery(
          Optional<String> name,
          Set<Office> offices,
          boolean onlyTechnician,
          Optional<LocalDate> start, Optional<LocalDate> end,
          boolean onlyOnCertificate,
          Optional<CompetenceCode> compCode,
          Optional<Person> personInCharge,
          boolean withBadge) {

    final QPerson person = QPerson.person;
    final QContract contract = QContract.contract;

    final JPQLQuery query = getQueryFactory().from(person)
            .leftJoin(person.contracts, contract).fetch()
            .leftJoin(person.user, QUser.user)
            .leftJoin(person.reperibility, QPersonReperibility.personReperibility).fetch()
            .leftJoin(person.personHourForOvertime, QPersonHourForOvertime.personHourForOvertime).fetch()
            .leftJoin(person.personShift, QPersonShift.personShift).fetch()
            .leftJoin(person.qualification).fetch()
            .orderBy(person.surname.asc(), person.name.asc())
            .distinct();

    final BooleanBuilder condition = new BooleanBuilder();

    if (personInCharge.isPresent()) {
      condition.and(person.personInCharge.eq(personInCharge.get()));
    }
    filterOffices(condition, offices);
    filterOnlyTechnician(condition, onlyTechnician);
    condition.and(new QFilters().filterNameFromPerson(QPerson.person, name));
    filterOnlyOnCertificate(condition, onlyOnCertificate);
    filterContract(condition, start, end);
    filterCompetenceCodeEnabled(condition, compCode);
    
    return query.where(condition);
  }


  /**
   * Filtro sugli uffici.
   */
  private void filterOffices(BooleanBuilder condition, Set<Office> offices) {

    final QPerson person = QPerson.person;

    if (offices != null && !offices.isEmpty()) {
      condition.and(person.office.in(offices));
    }
  }

  /**
   * Filtro sulle date contrattuali.
   * 
   * @param condition
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

      condition.andAnyOf(contract.endContract.isNull().and(contract.endDate.isNull()),
          contract.endDate.isNotNull().and(contract.endDate.goe(start.get())),
          contract.endContract.isNotNull().and(contract.endContract.goe(start.get())));
    }
  }


  private void filterOnlyTechnician(BooleanBuilder condition, boolean value) {


    if (value == true) {
      final QPerson person = QPerson.person;
      condition.and(person.qualification.qualification.gt(3));
    }
  }

  private void filterOnlyOnCertificate(BooleanBuilder condition, boolean value) {
    if (value) {
      final QContract contract = QContract.contract;
      condition.and(contract.onCertificate.isTrue());
    }
  }


  /**
   * Filtro su competenza abilitata.
   * 
   * @param condition
   * @param compCode
   */
  private void filterCompetenceCodeEnabled(BooleanBuilder condition,
      Optional<CompetenceCode> compCode) {

    if (compCode.isPresent()) {
      final QPerson person = QPerson.person;
      condition.and(person.competenceCode.contains(compCode.get()));
    }
  }


  /**
   * Importa tutte le informazioni della persona necessarie alla business logic ottimizzando il
   * numero di accessi al db.
   * @param id
   * @param begin
   * @param end
   */
  public Person fetchPersonForComputation(Long id, Optional<LocalDate> begin,
      Optional<LocalDate> end) {


    QPerson person = QPerson.person;

    // Fetch della persona e dei suoi contratti

    JPQLQuery query = getQueryFactory().from(person).leftJoin(person.contracts).fetch()
        .where(person.id.eq(id)).distinct();

    Person p = query.singleResult(person);

    fetchContracts(Optional.fromNullable(p), begin, end);
    // Fetch dei buoni pasto (non necessaria, una query)
    // Fetch dei personday

    personDayDao.getPersonDayInPeriod(p, begin.get(), end);

    return p;

  }

  /**
   * Fetch di tutti dati dei contratti attivi nella finestra temporale specificata. Si può filtrare
   * su una specifica persona.
   * @param person
   * @param start
   * @param end
   */
  private void fetchContracts(Optional<Person> person, Optional<LocalDate> start,
      Optional<LocalDate> end) {


    // Fetch dei contratti appartenenti all'intervallo
    QContract contract = QContract.contract;
    QContractWorkingTimeType cwtt = QContractWorkingTimeType.contractWorkingTimeType;
    QVacationPeriod vp = QVacationPeriod.vacationPeriod;
    QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;

    final BooleanBuilder condition = new BooleanBuilder();
    if (person.isPresent()) {
      condition.and(contract.person.eq(person.get()));
    }
    filterContract(condition, start, end);

    JPQLQuery query2 = getQueryFactory().from(contract).leftJoin(contract.contractMonthRecaps)
        .fetch().leftJoin(contract.contractStampProfile).fetch()
        .leftJoin(contract.contractWorkingTimeType, cwtt).fetch()
        .orderBy(contract.beginDate.asc()).distinct();
    List<Contract> contracts = query2.where(condition).list(contract);

    // fetch contract multiple bags (1) vacation periods
    JPQLQuery query2b = getQueryFactory().from(contract).leftJoin(contract.vacationPeriods, vp)
        .fetch().orderBy(contract.beginDate.asc()).orderBy(vp.beginFrom.asc()).distinct();
    contracts = query2b.where(condition).list(contract);
    // TODO: riportare a List tutte le relazioni uno a molti di contract
    // e inserire singolarmente la fetch.
    // TODO 2: in realtà questo è opinabile. Anche i Set
    // sono semanticamente corretti. Decidere.

    if (person.isPresent()) {
      // Fetch dei tipi orario associati ai contratti (verificare l'utilità)
      JPQLQuery query3 = getQueryFactory().from(cwtt).leftJoin(cwtt.workingTimeType, wtt).fetch()
          .where(cwtt.contract.in(contracts)).distinct();
      query3.list(cwtt);
    }
  }

  /**
   * Genera la lista di PersonLite contenente le persone attive nel mese specificato appartenenti ad
   * un office in offices.
   * Importante: utile perchè non sporca l'entity manager con oggetti parziali.
   * 
   * @param offices
   * @param year
   * @param month
   * @return
   */
  public List<PersonLite> liteList(Set<Office> offices, int year, int month) {

    final QPerson person = QPerson.person;

    Optional<LocalDate> beginMonth = Optional.fromNullable(new LocalDate(year, month, 1));
    Optional<LocalDate> endMonth =
        Optional.fromNullable(beginMonth.get().dayOfMonth().withMaximumValue());


    JPQLQuery lightQuery =
        getQueryFactory().from(person).leftJoin(person.contracts, QContract.contract)
        .orderBy(person.surname.asc(), person.name.asc()).distinct();


    lightQuery = personQuery(lightQuery, Optional.<String>absent(), offices, false, beginMonth,
        endMonth, true, Optional.<CompetenceCode>absent(), Optional.<BadgeReader>absent());


    QBean<PersonLite> bean =
        Projections.bean(PersonLite.class, person.id, person.name, person.surname);

    return ModelQuery.simpleResults(lightQuery, bean).list();

  }

  /**
   * Dto contenente le sole informazioni della persona richieste dalla select nel template menu.
   */
  public static class PersonLite {

    public Long id;
    public String name;
    public String surname;

    public Person person = null;

    public PersonLite(Long id, String name, String surname) {
      this.id = id;
      this.name = name;
      this.surname = surname;
    }
  }

}
