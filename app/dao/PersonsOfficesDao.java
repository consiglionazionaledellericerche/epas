package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import org.joda.time.LocalDate;
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
  
  /**
   * L'affiliazione della persona alla sede nell'anno/mese passati come parametro.
   * @param person la persona da cercare
   * @param office la sede su cui cercare
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return l'associazione periodica della persona alla sede se esiste
   */
  public Optional<PersonsOffices> affiliation(Person person, Office office, int year, int month) {
    LocalDate beginMonth = new LocalDate(year, month, 1);
    LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    return Optional.fromNullable(getQueryFactory().selectFrom(personsOffices)
        .where(personsOffices.person.eq(person)
            .and(personsOffices.office.eq(office)).and(personsOffices.beginDate.loe(endMonth))
            .andAnyOf(personsOffices.endDate.isNull(), personsOffices.endDate.goe(beginMonth)))
        .fetchFirst());
  }
}
