package dao;

import com.google.common.base.Optional;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.query.QPersonReperibility;
import models.query.QPersonReperibilityDay;
import models.query.QPersonReperibilityType;

import org.joda.time.LocalDate;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

/**
 * @author dario
 */
public class PersonReperibilityDayDao extends DaoBase {

  private static final QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
  private static final QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
  private static final QPersonReperibility pr = QPersonReperibility.personReperibility;

  @Inject
  PersonReperibilityDayDao(JPQLQueryFactory queryFactory,
                           Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }


  //*********************************************************/
  // Query DAO relative al PersonReperibilityDay.          **/
  //*********************************************************/

  /**
   * @return un personReperibilityDay nel caso in cui la persona person in data date fosse
   * reperibile. Null altrimenti
   */
  public Optional<PersonReperibilityDay> getPersonReperibilityDay(Person person, LocalDate date) {

    JPQLQuery query = getQueryFactory().from(prd)
            .where(prd.personReperibility.person.eq(person).and(prd.date.eq(date)));

    return Optional.fromNullable(query.singleResult(prd));

  }


  /**
   * @return il personReperibilityDay relativo al tipo e alla data passati come parametro
   */
  public PersonReperibilityDay getPersonReperibilityDayByTypeAndDate(PersonReperibilityType type, LocalDate date) {
    JPQLQuery query = getQueryFactory().from(prd).where(prd.date.eq(date).and(prd.reperibilityType.eq(type)));
    return query.singleResult(prd);
  }

  /**
   * @return la lista dei personReperibilityDay nel periodo compreso tra begin e to e con tipo type
   */
  public List<PersonReperibilityDay> getPersonReperibilityDayFromPeriodAndType(LocalDate begin, LocalDate to, PersonReperibilityType type, Optional<PersonReperibility> pr) {
    BooleanBuilder condition = new BooleanBuilder();
    if (pr.isPresent()) {
      condition.and(prd.personReperibility.eq(pr.get()));
    }
    JPQLQuery query = getQueryFactory().from(prd).where(condition.and(prd.date.between(begin, to)
            .and(prd.reperibilityType.eq(type)))).orderBy(prd.date.asc());
    return query.list(prd);
  }


  /**
   * @return il numero di personReperibilityDay cancellati che hanno come parametri il tipo type e
   * il giorno day
   */
  public long deletePersonReperibilityDay(PersonReperibilityType type, LocalDate day) {
    Long deleted = getQueryFactory().delete(prd).where(prd.reperibilityType.eq(type).and(prd.date.eq(day))).execute();
    return deleted;
  }

  //***************************************************************/
  // Query DAO relative al personReperibilityType                **/
  //***************************************************************/

  /**
   * @return il personReperibilityType relativo all'id passato come parametro
   */
  public PersonReperibilityType getPersonReperibilityTypeById(Long id) {
    JPQLQuery query = getQueryFactory().from(prt).where(prt.id.eq(id));
    return query.singleResult(prt);
  }

  //***************************************************************/
  // Query DAO relative al personReperibility                    **/
  //***************************************************************/

  /**
   * @return il PersonReperibility relativo alla persona person e al tipo type passati come
   * parametro
   */
  public PersonReperibility getPersonReperibilityByPersonAndType(Person person, PersonReperibilityType type) {
    JPQLQuery query = getQueryFactory().from(pr).where(pr.person.eq(person).and(pr.personReperibilityType.eq(type)));
    return query.singleResult(pr);

  }


  /**
   * @return la lista dei personReperibility che hanno come personReperibilityType il tipo passato
   * come parametro
   */
  public List<PersonReperibility> getPersonReperibilityByType(PersonReperibilityType type) {
    JPQLQuery query = getQueryFactory().from(pr).where(pr.personReperibilityType.eq(type));
    return query.list(pr);
  }


}
