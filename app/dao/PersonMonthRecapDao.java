package dao;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.CertificatedData;
import models.Office;
import models.Person;
import models.PersonMonthRecap;
import models.query.QCertificatedData;
import models.query.QPersonMonthRecap;
import org.joda.time.LocalDate;

/**
 * Dao relativo ai PersonMonthRecap.
 *
 * @author dario
 */
public class PersonMonthRecapDao extends DaoBase {

  @Inject
  PersonMonthRecapDao(JPQLQueryFactory queryFactory,
      Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @return la lista di personMonthRecap relativa all'anno year per la persona person.
   */
  public List<PersonMonthRecap> getPersonMonthRecapInYearOrWithMoreDetails(Person person,
      Integer year, Optional<Integer> month, Optional<Boolean> hoursApproved) {

    QPersonMonthRecap personMonthRecap = QPersonMonthRecap.personMonthRecap;

    final BooleanBuilder condition = new BooleanBuilder();

    if (month.isPresent()) {
      condition.and(personMonthRecap.month.eq(month.get()));
    }
    if (hoursApproved.isPresent()) {
      condition.and(personMonthRecap.hoursApproved.eq(hoursApproved.get()));
    }

    return getQueryFactory().selectFrom(personMonthRecap)
        .where(condition.and(personMonthRecap.person.eq(person)
            .and(personMonthRecap.year.eq(year))))
        .fetch();
  }

  /**
   * @return il personMonthRecap relativo all'id passato come parametro.
   */
  public PersonMonthRecap getPersonMonthRecapById(Long id) {
    QPersonMonthRecap personMonthRecap = QPersonMonthRecap.personMonthRecap;
    return getQueryFactory().selectFrom(personMonthRecap)
        .where(personMonthRecap.id.eq(id))
        .fetchOne();
  }


  public List<PersonMonthRecap> getPersonMonthRecaps(
      Person person, Integer year, Integer month, LocalDate begin, LocalDate end) {
    QPersonMonthRecap personMonthRecap = QPersonMonthRecap.personMonthRecap;
    return getQueryFactory().selectFrom(personMonthRecap)
        .where(personMonthRecap.person.eq(person).and(personMonthRecap.year.eq(year)
            .and(personMonthRecap.month.eq(month)
                .andAnyOf(
                    personMonthRecap.fromDate.loe(begin).and(personMonthRecap.toDate.goe(end)),
                    personMonthRecap.fromDate.loe(end)
                        .and(personMonthRecap.toDate.goe(end))))))
        .fetch();
  }

  /**
   * @return la lista dei personMonthRecap di tutte le persone che appartengono all'ufficio office
   * nell'anno year e nel mese month passati come parametro.
   */
  public List<PersonMonthRecap> getPeopleMonthRecaps(Integer year, Integer month, Office office) {
    QPersonMonthRecap personMonthRecap = QPersonMonthRecap.personMonthRecap;
    return getQueryFactory().selectFrom(personMonthRecap)
        .where(personMonthRecap.month.eq(month)
            .and(personMonthRecap.year.eq(year)
                .and(personMonthRecap.person.office.eq(office))))
        .fetch();
  }


  /**
   * @return il personMonthRecap, se esiste, relativo ai parametri passati come riferimento.
   */
  public Optional<PersonMonthRecap> getPersonMonthRecapByPersonYearAndMonth(
      Person person, Integer year, Integer month) {
    QPersonMonthRecap personMonthRecap = QPersonMonthRecap.personMonthRecap;
    final PersonMonthRecap result = getQueryFactory().selectFrom(personMonthRecap)
        .where(personMonthRecap.person.eq(person).and(personMonthRecap.year.eq(year)
            .and(personMonthRecap.month.eq(month)))).fetchOne();
    return Optional.fromNullable(result);
  }


  /* *****************************************************************************************/
  /* Parte relativa a query su CertificatedData per la quale, essendo unica, non si è deciso */
  /* di creare un Dao ad hoc                                                                 */
  /* *****************************************************************************************/

  /**
   * @return il certificatedData relativo all'id passato come parametro.
   */
  public CertificatedData getCertificatedDataById(Long id) {
    QCertificatedData cert = QCertificatedData.certificatedData;
    return getQueryFactory().selectFrom(cert)
        .where(cert.id.eq(id)).fetchOne();
  }


  /**
   * @return il certificatedData relativo alla persona 'person' per il mese 'month' e l'anno 'year'.
   */
  public CertificatedData getPersonCertificatedData(Person person, Integer month, Integer year) {

    QCertificatedData cert = QCertificatedData.certificatedData;

    return getQueryFactory().selectFrom(cert)
        .where(cert.person.eq(person).and(cert.month.eq(month).and(cert.year.eq(year))))
        .fetchOne();
  }


}
