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
   * Il personChildren con id passato come parametro.
   * @param id l'identificativo del figlio del dipendente
   * @return il personChildren relativo all'id passato come parametro.
   */
  public PersonChildren getById(Long id) {
    final QPersonChildren personChildren = QPersonChildren.personChildren;
    return getQueryFactory().selectFrom(personChildren)
        .where(personChildren.id.eq(id))
        .fetchOne();
  }


  /**
   * La lista dei figli di una persona passata come parametro.
   * @param person la persona di cui si vogliono i figli
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
