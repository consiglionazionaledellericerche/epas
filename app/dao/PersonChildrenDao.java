package dao;

import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Person;
import models.PersonChildren;
import models.query.QPersonChildren;


/**
 * Dao per i PersonChildren.
 *
 * @author dario
 */
public class PersonChildrenDao extends DaoBase {

  @Inject
  PersonChildrenDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }


  /**
   * @return il personChildren relativo all'id passato come parametro.
   */
  public PersonChildren getById(Long id) {
    final QPersonChildren personChildren = QPersonChildren.personChildren;
    return getQueryFactory().selectFrom(personChildren)
        .where(personChildren.id.eq(id))
        .fetchOne();
  }


  /**
   * @return la lista di tutti i figli della persona.
   */
  public List<PersonChildren> getAllPersonChildren(Person person) {
    final QPersonChildren personChildren = QPersonChildren.personChildren;
    return getQueryFactory().selectFrom(personChildren)
        .where(personChildren.person.eq(person))
        .orderBy(personChildren.bornDate.asc())
        .fetch();
  }
}
