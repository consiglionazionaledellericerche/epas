package dao;

import static com.querydsl.core.group.GroupBy.groupBy;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import models.Person;
import models.absences.AbsenceType;
import models.absences.query.QAbsence;
import models.absences.query.QAbsenceType;
import org.joda.time.LocalDate;

/**
 * Dao per l'accesso alle informazioni degli AbsenceType.
 *
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

    return getQueryFactory().select(Projections.bean(AbsenceTypeDto.class,
        absenceType.code, absence.count())).from(absenceType)
        .join(absenceType.absences, absence)
        .groupBy(absenceType)
        .orderBy(absence.count().desc())
        .fetch();
  }

  public Map<AbsenceType, Long> counters() {

    final QAbsenceType absenceType = QAbsenceType.absenceType;
    final QAbsence absence = QAbsence.absence;

    return getQueryFactory().from(absenceType)
        .join(absenceType.absences, absence)
        .groupBy(absenceType)
        .orderBy(absence.count().desc())
        .transform(groupBy(absenceType).as(absence.count()));

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
      return getQueryFactory().selectFrom(absenceType)
          .leftJoin(absenceType.absences, absence)
          .where(absence.id.isNull().or(absence.id.isNotNull()))
          .groupBy(absenceType)
          .orderBy(absence.count().desc())
          .fetch();

    } catch (Exception ex) {
      return getQueryFactory().selectFrom(absenceType)
          .orderBy(absenceType.code.asc()).fetch();
    }

  }

  /**
   * @return la lista degli AbsenceType che non siano di internalUse.
   */
  public List<AbsenceType> certificateTypes() {

    final QAbsenceType absenceType = QAbsenceType.absenceType;

    return getQueryFactory().selectFrom(absenceType)
        .where(absenceType.internalUse.eq(false)).fetch();
  }

  /**
   * @return l'absenceType relativo all'id passato come parametro.
   */
  public AbsenceType getAbsenceTypeById(Long long1) {

    QAbsenceType absenceType = QAbsenceType.absenceType;

    return getQueryFactory().selectFrom(absenceType)
        .where(absenceType.id.eq(long1))
        .fetchOne();

  }

  /**
   * @return la lista di codici di assenza che sono validi da una certa data in poi ordinati per
   * codice di assenza crescente.
   */
  public List<AbsenceType> getAbsenceTypeFromEffectiveDate(
      LocalDate date) {

    QAbsenceType absenceType = QAbsenceType.absenceType;

    return getQueryFactory().selectFrom(absenceType)
        .where(absenceType.validTo.after(date).or(absenceType.validTo.isNull()))
        .orderBy(absenceType.code.asc())
        .fetch();
  }

  /**
   * @return l'absenceType relativo al codice passato come parametro.
   */
  public Optional<AbsenceType> getAbsenceTypeByCode(String string) {

    QAbsenceType absenceType = QAbsenceType.absenceType;

    final JPQLQuery<AbsenceType> query = getQueryFactory().selectFrom(absenceType)
        .where(absenceType.code.eq(string).or(absenceType.code.equalsIgnoreCase(string)));

    return Optional.fromNullable(query.fetchOne());

  }

  /**
   * Le absenceType con quei codici di assenza.
   *
   * @param codes la lista dei codice di assenza da cercare
   * @return la lista degli AbsenceType corrispondenti ai codici passati.
   */
  public List<AbsenceType> absenceTypeCodeSet(Set<String> codes) {

    QAbsenceType absenceType = QAbsenceType.absenceType;

    return getQueryFactory().selectFrom(absenceType)
        .where(absenceType.code.in(codes))
        .fetch();
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
        .transform(groupBy(absenceType).as(absence.count()));
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
