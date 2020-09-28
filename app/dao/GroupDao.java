package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import java.time.LocalDate;
import java.util.List;
import javax.persistence.EntityManager;
import models.Office;
import models.Person;
import models.flows.Group;
import models.flows.query.QAffiliation;
import models.flows.query.QGroup;

public class GroupDao extends DaoBase {

  @Inject
  GroupDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory wrapperFactory) {
    super(queryFactory, emp);
  }

  public Optional<Group> byId(Long id) {
    final QGroup group = QGroup.group;
    return Optional.fromNullable(
        getQueryFactory().selectFrom(group).where(group.id.eq(id)).fetchFirst());
  }
  
  /**
   * Metodo che ritorna la lista dei gruppi che appartengono alla sede passata come parametro.
   *
   * @param office la sede di cui si richiedono i gruppi
   * @return la lista dei gruppi i cui responsabili appartengono alla sede passata come parametro.
   */
  public List<Group> groupsByOffice(Office office, Optional<Person> manager) {
    final QGroup group = QGroup.group;
    BooleanBuilder condition = new BooleanBuilder();
    if (manager.isPresent()) {
      condition.and(group.manager.eq(manager.get()));
    }
    return getQueryFactory().selectFrom(group).where(group.office.eq(office)).where(condition)
        .fetch();
  }

  /**
   * Metodo che ritorna la lista dei gruppi di cui Person è responsabile.
   *
   * @param person la persona di cui cerco i gruppi in cui è responsabile
   * @return la lista dei gruppi di cui Person è responsabile.
   */
  public List<Group> groupsByManager(Optional<Person> person) {
    final QGroup group = QGroup.group;
    BooleanBuilder condition = new BooleanBuilder();
    if (person.isPresent()) {
      condition.and(group.manager.eq(person.get()));
    }
    return getQueryFactory().selectFrom(group).where(condition).fetch();
  }

  private Predicate personAffiliationCondition(QAffiliation affiliation, Person person) {
    BooleanBuilder endDateCondition = new BooleanBuilder(affiliation.endDate.isNull());
    endDateCondition = endDateCondition.or(affiliation.endDate.after(LocalDate.now()));
    return affiliation.person.eq(person)
        .and(affiliation.beginDate.before(LocalDate.now())
            .or(endDateCondition));
  } 
  
  /**
   * Metodo che ritorna la lista dei gruppi di cui fa parte la person passata come parametro.
   *
   * @param person la persona di cui si cercano i gruppi di cui fa parte
   * @return la lista dei gruppi di cui fa parte la persona passata come parametro.
   */
  public List<Group> myGroups(Person person) {
    final QGroup group = QGroup.group;
    final QAffiliation affiliation = QAffiliation.affiliation;
    BooleanBuilder endDateCondition = new BooleanBuilder(affiliation.endDate.isNull());
    endDateCondition = endDateCondition.or(affiliation.endDate.after(LocalDate.now()));
    return getQueryFactory().selectFrom(group)
        .join(group.affiliations, affiliation)
        .where(personAffiliationCondition(affiliation, person)).fetch();
  }

  /**
   * Metodo che ritorna il gruppo, se esiste, con manager manager e come appartenente person.
   *
   * @param manager il manager del gruppo
   * @param person la persona appartenente al gruppo
   * @return il gruppo, se esiste, che ha come manager manager e come appartenente person.
   */
  public Optional<Group> checkManagerPerson(Person manager, Person person) {
    final QGroup group = QGroup.group;
    final QAffiliation affiliation = QAffiliation.affiliation;
    final Group result = getQueryFactory().selectFrom(group)
        .join(group.affiliations, affiliation)
        .where(personAffiliationCondition(affiliation, person), group.manager.eq(manager))
        .fetchFirst();
    return Optional.fromNullable(result);
  }
}
