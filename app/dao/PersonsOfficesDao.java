package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import dao.wrapper.IWrapperFactory;
import models.Office;
import models.Person;
import models.PersonsOffices;
import models.absences.Absence;
import models.query.QPersonsOffices;

public class PersonsOfficesDao extends DaoBase{

  private final IWrapperFactory factory;

  @Inject
  PersonsOfficesDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory factory) {
    super(queryFactory, emp);
    this.factory = factory;
  }
  
  /**
   * Ritorna la lista delle affiliazioni della persona in ordine decrescente.
   * 
   * @param person la persona di cui si richiedono le affiliazioni alle sedi
   * @return la lista delle affiliazioni della persona in ordine decrescente.
   */
  public List<PersonsOffices> listByPerson(Person person) {
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    return getQueryFactory().selectFrom(personsOffices)
        .where(personsOffices.person.eq(person))
        .orderBy(personsOffices.beginDate.desc()).fetch();
  }
  
  /**
   * Ritorna la lista delle affiliazioni alla sede in ordine decrescente.
   * 
   * @param office la sede di cui si richiedono le affiliazioni di personale
   * @return la lista delle affiliazioni alla sede office.
   */
  public List<PersonsOffices> listByOffice(Office office) {
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    return getQueryFactory().selectFrom(personsOffices)
        .where(personsOffices.office.eq(office))
        .orderBy(personsOffices.beginDate.desc()).fetch();
  }
}
