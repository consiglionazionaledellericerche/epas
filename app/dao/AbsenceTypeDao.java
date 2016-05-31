package dao;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.types.Projections;

import helpers.jpa.ModelQuery;

import models.AbsenceType;
import models.Person;
import models.query.QAbsence;
import models.query.QAbsenceType;

import org.bouncycastle.util.Strings;
import org.joda.time.LocalDate;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

/**
 * @author dario
 */
public class AbsenceTypeDao extends DaoBase {

  @Inject
  AbsenceTypeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  public List<AbsenceTypeDto> countersDto() {

    final QAbsenceType absenceType = QAbsenceType.absenceType;
    final QAbsence absence = QAbsence.absence;

    return getQueryFactory().from(absenceType)
        .join(absenceType.absences, absence)
        .groupBy(absenceType)
        .orderBy(absence.count().desc())
        .list(Projections.bean(AbsenceTypeDto.class,
            absenceType.code, absence.count()));
  }

  public Map<AbsenceType, Long> counters() {

    final QAbsenceType absenceType = QAbsenceType.absenceType;
    final QAbsence absence = QAbsence.absence;

    return getQueryFactory().from(absenceType)
        .join(absenceType.absences, absence)
        .groupBy(absenceType)
        .orderBy(absence.count().desc())
        .map(absenceType, absence.count());
  }

  /**
   * I tipi assenza ordinata da quelli più frequenti.
   *
   * @return lista assenze ordinata per utilizzo.
   */
  public List<AbsenceType> getFrequentTypes() {

    final QAbsenceType absenceType = QAbsenceType.absenceType;
    final QAbsence absence = QAbsence.absence;

    //TRICK selezionare anche quelle mai usate: http://stackoverflow.com/
    //questions/4076098/how-to-select-rows-with-no-matching-entry-in-another-table

    //IVV fallisce. Aggiornare alla macchina docker.
    try {
      final JPQLQuery query = getQueryFactory().from(absenceType)
          .leftJoin(absenceType.absences, absence)
          .where(absence.id.isNull().or(absence.id.isNotNull()))
          .groupBy(absenceType)
          .orderBy(absence.count().desc());

      return query.list(absenceType);

    } catch (Exception e) {
      return getQueryFactory().from(absenceType)
          .orderBy(absenceType.code.asc()).list(absenceType);
    }

  }

  public List<AbsenceType> certificateTypes() {

    final QAbsenceType absenceType = QAbsenceType.absenceType;

    return getQueryFactory().from(absenceType)
        .where(absenceType.internalUse.eq(false)).list(absenceType);
  }

  public ModelQuery.SimpleResults<AbsenceType> getAbsences(Optional<String> name) {

    final QAbsenceType absenceType = QAbsenceType.absenceType;
    final BooleanBuilder condition = new BooleanBuilder();

    final JPQLQuery query = getQueryFactory().from(absenceType).orderBy(absenceType.code.asc());

    if (name.isPresent() && !name.get().trim().isEmpty()) {
      condition.andAnyOf(absenceType.code.startsWithIgnoreCase(name.get()),
          absenceType.description.toLowerCase().like("%" + Strings.toLowerCase(name.get()) + "%"));
    }

    return ModelQuery.wrap(query.where(condition), absenceType);
  }

  /**
   * @return l'absenceType relativo all'id passato come parametro.
   */
  public AbsenceType getAbsenceTypeById(Long long1) {

    QAbsenceType absenceType = QAbsenceType.absenceType;

    final JPQLQuery query = getQueryFactory().from(absenceType)
        .where(absenceType.id.eq(long1));

    return query.singleResult(absenceType);

  }

  /**
   * @return la lista di codici di assenza che sono validi da una certa data in poi ordinati per
   * codice di assenza crescente.
   */
  public List<AbsenceType> getAbsenceTypeFromEffectiveDate(
      LocalDate date) {

    QAbsenceType absenceType = QAbsenceType.absenceType;

    final JPQLQuery query = getQueryFactory().from(absenceType)
        .where(absenceType.validTo.after(date).or(absenceType.validTo.isNull()))
        .orderBy(absenceType.code.asc());

    return query.list(absenceType);
  }

  /**
   * @return l'absenceType relativo al codice passato come parametro.
   */
  public Optional<AbsenceType> getAbsenceTypeByCode(String string) {

    QAbsenceType absenceType = QAbsenceType.absenceType;

    final JPQLQuery query = getQueryFactory().from(absenceType)
        .where(absenceType.code.eq(string));

    return Optional.fromNullable(query.singleResult(absenceType));

  }

  /**
   * Una mappa contenente gli AbsenceType fatte dalle persona nel mese e numero di assenze fatte per
   * ogni tipo.
   */
  public Map<AbsenceType, Long> getAbsenceTypeInPeriod(
      Person person, LocalDate fromDate, Optional<LocalDate> toDate) {
    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(fromDate);

    QAbsenceType absenceType = QAbsenceType.absenceType;
    QAbsence absence = QAbsence.absence;

    return getQueryFactory().from(absenceType)
        .join(absenceType.absences, absence).where(absence.personDay.person.eq(person).and(
            absence.personDay.date.between(fromDate, toDate.or(fromDate))))
        .groupBy(absenceType)
        .orderBy(absence.count().desc())
        .map(absenceType, absence.count());
  }

  public class AbsenceTypeDto {
    public String code;
    public long count;

    public AbsenceTypeDto(String code, long count) {
      this.code = code;
      this.count = count;
    }

    public boolean isUsed() {
      return count > 10;
    }
  }

}
