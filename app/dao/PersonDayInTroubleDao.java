package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.Person;
import models.PersonDayInTrouble;
import models.query.QPersonDayInTrouble;

import org.joda.time.LocalDate;

import java.util.List;

import javax.persistence.EntityManager;

public class PersonDayInTroubleDao extends DaoBase {

  @Inject
  PersonDayInTroubleDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @return la lista dei personDayInTrouble relativi alla persona person nel periodo begin-end.
   *     E' possibile specificare se si vuole ottenere quelli fixati (fixed = true) o no
   *     (fixed = false).
   */
  public List<PersonDayInTrouble> getPersonDayInTroubleInPeriod(
      Person person, Optional<LocalDate> begin, Optional<LocalDate> end) {

    QPersonDayInTrouble pdit = QPersonDayInTrouble.personDayInTrouble;

    BooleanBuilder conditions = new BooleanBuilder(pdit.personDay.person.eq(person));
    if (begin.isPresent()) {
      conditions.and(pdit.personDay.date.goe(begin.get()));
    }
    if (end.isPresent()) {
      conditions.and(pdit.personDay.date.loe(end.get()));
    }

    final JPQLQuery query = getQueryFactory().from(pdit).where(conditions);

    return query.list(pdit);
  }
}
