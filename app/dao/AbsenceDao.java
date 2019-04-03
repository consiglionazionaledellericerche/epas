package dao;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import helpers.jpa.ModelQuery;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import models.Contract;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.query.QAbsence;
import models.absences.query.QAbsenceType;
import models.absences.query.QJustifiedType;
import models.exports.FrequentAbsenceCode;
import models.query.QPersonDay;
import org.joda.time.LocalDate;

/**
 * Dao per l'accesso alle informazioni delle Absence.
 *
 * @author dario
 */
public class AbsenceDao extends DaoBase {

  private final IWrapperFactory factory;

  @Inject
  AbsenceDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory factory) {
    super(queryFactory, emp);
    this.factory = factory;
  }

  /**
   * @param id id dell'Absence.
   * @return la Absence corrispondente all'id passato.
   */
  public Absence getAbsenceById(Long id) {

    final QAbsence absence = QAbsence.absence;
    final QAbsenceType absenceType = QAbsenceType.absenceType;
    final QJustifiedType justifiedType = QJustifiedType.justifiedType;
    final JPQLQuery<?> query = getQueryFactory().from(absence)
        .join(absence.absenceType, absenceType).fetchJoin()
        .join(absence.justifiedType, justifiedType).fetchJoin()
        .where(absence.id.eq(id));
    return (Absence) query.fetchOne();
  }

  /**
   * @return la lista di assenze di una persona tra due date se e solo se il campo dateTo isPresent.
   * In caso non sia valorizzato, verrano ritornate le assenze relative a un solo giorno. Se il
   * booleano forAttachment è true, si cercano gli allegati relativi a un certo periodo.
   */
  public List<Absence> getAbsencesInPeriod(Optional<Person> person,
      LocalDate dateFrom, Optional<LocalDate> dateTo, boolean forAttachment) {

    final QAbsence absence = QAbsence.absence;

    final BooleanBuilder condition = new BooleanBuilder();

    if (person.isPresent()) {
      condition.and(absence.personDay.person.eq(person.get()));
    }
    if (forAttachment) {
      condition.and(absence.absenceFile.isNotNull());
    }
    if (dateTo.isPresent()) {
      condition.and(absence.personDay.date.between(dateFrom, dateTo.get()));
    } else {
      condition.and(absence.personDay.date.eq(dateFrom));
    }

    return getQueryFactory().selectFrom(absence)
        .where(condition).orderBy(absence.absenceType.code.asc()).fetch();

  }

  /**
   * // TODO: questo metodo deve essere privato e esportarne le viste.
   */
  public List<Absence> getAbsenceByCodeInPeriod(
      Optional<Person> person, Optional<String> code, LocalDate from, LocalDate to,
      Optional<JustifiedTypeName> justifiedTypeName, boolean forAttachment,
      boolean ordered) {

    final QAbsence absence = QAbsence.absence;

    final BooleanBuilder condition = new BooleanBuilder();
    if (forAttachment) {
      condition.and(absence.absenceFile.isNotNull());
    }
    if (person.isPresent()) {
      condition.and(absence.personDay.person.eq(person.get()));
    }
    if (justifiedTypeName.isPresent()) {
      condition.and(absence.justifiedType.name.eq(justifiedTypeName.get()));
    }
    if (code.isPresent()) {
      condition.and(absence.absenceType.code.eq(code.get()));
    }
    condition.and(absence.personDay.date.between(from, to));

    JPQLQuery<Absence> query = getQueryFactory().selectFrom(absence).where(condition);
    if (ordered) {
      query.orderBy(absence.personDay.date.asc());
    }
    return query.fetch();

  }


  /**
   * @param person la persona di cui cercare le assenze.
   * @param begin la data di inizio da cui cercare le assenze (compresa)
   * @param end la data di fine fino a cui cercare le assenze (compresa
   * @param code il codice dell'assenza da cercare
   * @return la lista delle assenze corrispondenti ai parametri passati
   */
  public List<Absence> absenceInPeriod(
      Person person, LocalDate begin, LocalDate end, String code) {

    final QAbsence absence = QAbsence.absence;

    return getQueryFactory().selectFrom(absence)
        .leftJoin(absence.personDay)
        .leftJoin(absence.personDay.person)
        .where(absence.absenceType.code.eq(code)
            .and(absence.personDay.date.between(begin, end))
            .and(absence.personDay.person.eq(person))).fetch();

  }

  /**
   * @return il quantitativo di assenze presenti in un certo periodo temporale delimitato da begin e
   * end che non appartengono alla lista di codici passata come parametro nella lista di stringhe
   * absenceCode.
   */
  public Long howManyAbsenceInPeriodNotInList(
      LocalDate begin, LocalDate end, List<String> absenceCode) {

    final QAbsence absence = QAbsence.absence;

    final JPQLQuery<?> query = getQueryFactory().from(absence)
        .where(absence.personDay.date.between(begin, end)
            .and(absence.absenceType.code.notIn(absenceCode)));
    if (query.fetchCount() == 0) {
      return 0L;
    } else {
      return query.fetchCount();
    }
  }

  /**
   * @param person La persona della quale recuperare le assenze
   * @param fromDate La data iniziale dell'intervallo temporale da considerare
   * @param toDate La data finale dell'intervallo temporale da considerare (opzionale)
   * @param absenceType Il tipo di assenza specifico
   * @return La lista delle assenze sull'intervallo e la persona specificati.
   */
  public ModelQuery.SimpleResults<Absence> findByPersonAndDate(Person person,
      LocalDate fromDate, Optional<LocalDate> toDate, Optional<AbsenceType> absenceType) {

    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(fromDate);

    final QAbsence absence = QAbsence.absence;

    BooleanBuilder conditions = new BooleanBuilder(absence.personDay.person.eq(person)
        .and(absence.personDay.date.between(fromDate, toDate.or(fromDate))));

    if (absenceType.isPresent()) {
      conditions.and(absence.absenceType.eq(absenceType.get()));
    }

    return ModelQuery.wrap(getQueryFactory().from(absence).where(conditions), absence);
  }

  /**
   * @return la lista delle assenze contenenti un tipo di assenza con uso interno = false relative a
   * una persona nel periodo compreso tra begin e end ordinate per per data.
   */
  public List<Absence> getAbsenceWithNotInternalUseInMonth(
      Person person, LocalDate begin, LocalDate end) {

    final QAbsence absence = QAbsence.absence;

    return getQueryFactory().selectFrom(absence)
        .where(absence.personDay.person.eq(person)
            .and(absence.personDay.date.between(begin, end)
                .and(absence.absenceType.internalUse.eq(false))))
        .orderBy(absence.personDay.date.asc()).fetch();
  }


  /**
   * Controlla che nell'intervallo passato in args non esista gia' una assenza giornaliera.
   *
   * @return true se esiste un'assenza giornaliera nel periodo passato, false altrimenti.
   */
  public List<Absence> allDayAbsenceAlreadyExisting(
      Person person, LocalDate fromDate, Optional<LocalDate> toDate) {
    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(fromDate);

    final QAbsence absence = QAbsence.absence;

    return getQueryFactory()
        .selectFrom(absence)
        .where(absence.personDay.person.eq(person).and(
            absence.personDay.date.between(fromDate, toDate.or(fromDate))).and(
            absence.justifiedType.name.eq(JustifiedTypeName.all_day)
                .or(absence.justifiedType.name.eq(JustifiedTypeName.assign_all_day))))
        .fetch();

  }

  /**
   * La lista delle assenze restituite è prelevata in FETCH JOIN con le absenceType i personDay e la
   * person in modo da non effettuare ulteriori select.
   *
   * @return la lista delle assenze che non sono di tipo internalUse effettuate in questo mese dalla
   * persona relativa a questo personMonth.
   */
  public List<Absence> getAbsencesNotInternalUseInMonth(
      Person person, Integer year, Integer month) {

    return getAbsenceWithNotInternalUseInMonth(
        person, new LocalDate(year, month, 1),
        new LocalDate(year, month, 1).dayOfMonth().withMaximumValue());
  }

  /**
   * @return la lista di assenze effettuate dalle persone presenti nella lista personList nel
   * periodo temporale compreso tra from e to.
   */
  public List<Absence> getAbsenceForPersonListInPeriod(
      List<Person> personList, LocalDate from, LocalDate to) {

    final QAbsence absence = QAbsence.absence;

    return getQueryFactory().selectFrom(absence)
        .where(absence.personDay.date.between(from, to)
            .and(absence.personDay.person.in(personList)))
        .orderBy(absence.personDay.person.id.asc(), absence.personDay.date.asc()).fetch();
  }


  /**
   * @return la lista di assenze effettuate dal titolare del contratto del tipo ab nell'intervallo
   * temporale inter.
   */
  public List<Absence> getAbsenceDays(DateInterval inter, Contract contract, AbsenceType ab) {

    DateInterval contractInterInterval =
        DateUtility.intervalIntersection(inter, factory.create(contract).getContractDateInterval());
    if (contractInterInterval == null) {
      return new ArrayList<Absence>();
    }

    List<Absence> absences =
        getAbsenceByCodeInPeriod(
            Optional.fromNullable(contract.person), Optional.fromNullable(ab.code),
            contractInterInterval.getBegin(), contractInterInterval.getEnd(),
            Optional.<JustifiedTypeName>absent(), false, true);

    return absences;

  }

  /**
   * @return la lista dei frequentAbsenceCode, ovvero dei codici di assenza più frequentemente usati
   * nel periodo compreso tra 'dateFrom' e 'dateTo'.
   */
  public List<FrequentAbsenceCode> getFrequentAbsenceCodeForAbsenceFromJson(
      LocalDate dateFrom, LocalDate dateTo) {
    List<FrequentAbsenceCode> frequentAbsenceCodeList = new ArrayList<>();
    QAbsence absence = QAbsence.absence;
    QPersonDay personDay = QPersonDay.personDay;

    BooleanBuilder conditions = new BooleanBuilder(personDay.date.between(dateFrom, dateTo));

    List<String> listaRiposiCompensativi =
        getQueryFactory().select(absence.absenceType.code).from(absence)
            .join(absence.personDay, personDay)
            .where(conditions
                .and(absence.absenceType.description.containsIgnoreCase("Riposo compensativo")))
        .distinct().fetch();

    List<String> listaFerie = getQueryFactory().select(absence.absenceType.code).from(absence)
        .join(absence.personDay, personDay)
        .where(conditions.and(absence.absenceType.description.containsIgnoreCase("ferie")
            .or(absence.absenceType.code.eq("94"))))
        .distinct().fetch();

    List<String> listaMissioni = getQueryFactory().select(absence.absenceType.code)
        .from(absence).join(absence.personDay, personDay)
        .where(conditions.and(absence.absenceType.code.eq("92")))
        .distinct().fetch();

    Joiner joiner = Joiner.on("-").skipNulls();

    frequentAbsenceCodeList.add(new FrequentAbsenceCode(joiner.join(listaFerie), "Ferie"));
    frequentAbsenceCodeList.add(
        new FrequentAbsenceCode(joiner.join(listaRiposiCompensativi), "Riposo compensativo"));
    frequentAbsenceCodeList.add(new FrequentAbsenceCode(joiner.join(listaMissioni), "Missione"));

    return frequentAbsenceCodeList;
  }

  /**
   * @return la lista delle assenze effettuate dalla persona nell'anno.
   */
  public List<Absence> getYearlyAbsence(Person person, int year) {

    return getAbsencesInPeriod(Optional.fromNullable(person),
        new LocalDate(year, 1, 1), Optional.of(new LocalDate(year, 12, 31)), false);
  }

  /**
   * @param person la persona di cui si vogliono le assenze
   * @param start la data di inizio da cui cercare
   * @param end la data di fine fino a cui cercare
   * @param types i tipi di assenza
   * @return la lista di assenze filtrata per persona, data inizio, data fine.
   */
  public List<Absence> filteredByTypes(Person person, LocalDate start, LocalDate end,
      Collection<JustifiedTypeName> types) {
    final QAbsence absence = QAbsence.absence;
    final QPersonDay personDay = QPersonDay.personDay;

    return  getQueryFactory().selectFrom(absence)
        .leftJoin(absence.personDay, personDay).fetchJoin()
        .where(personDay.person.eq(person)
            .and(personDay.date.between(start, end))
            .and(absence.justifiedType.name.in(types)))
        .orderBy(personDay.date.asc()).fetch();

  }

  /**
   * Metodo che ritorna la lista delle assenze per missione contrassegnate dall'identificativo.
   *
   * @param externalId l'identificativo esterno fornito dal client missioni in fase di persistenza
   * dell'assenza sul db
   * @return la lista di assenze che hanno come identificativo l'id passato come parametro.
   */
  public List<Absence> absencesPersistedByMissions(long externalId) {
    final QAbsence absence = QAbsence.absence;

    return  getQueryFactory().selectFrom(absence)
        .where(absence.externalIdentifier.eq(externalId)).fetch();
  }


}
