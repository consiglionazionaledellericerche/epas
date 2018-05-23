package dao;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import models.Office;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonShiftDay;
import models.ShiftType;
import models.query.QPersonReperibility;
import models.query.QPersonReperibilityDay;
import models.query.QPersonReperibilityType;
import models.query.QPersonShiftDay;

import org.joda.time.LocalDate;

/**
 * Dao per i PersonReperibilityDay.
 *
 * @author dario
 */
public class PersonReperibilityDayDao extends DaoBase {

  @Inject
  PersonReperibilityDayDao(JPQLQueryFactory queryFactory,
                           Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }


  //*********************************************************/
  // Query DAO relative al PersonReperibilityDay.          **/
  //*********************************************************/

  /**
   * Metodo che ritorna, se esiste, il personreperibilityday che risponde ai parametri passati.
   * @param person la persona
   * @param date la data
   * @return un personReperibilityDay nel caso in cui la persona person in data date fosse
   *     reperibile. Null altrimenti.
   */
  public Optional<PersonReperibilityDay> getPersonReperibilityDay(Person person, LocalDate date) {
    QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
    JPQLQuery query = getQueryFactory().from(prd)
            .where(prd.personReperibility.person.eq(person).and(prd.date.eq(date)));

    return Optional.fromNullable(query.singleResult(prd));

  }


  /**
   * Metodo che ritorna il giorno di reperibilità associato al tipo e alla data passati.
   * @param type il tipo di reperibilità
   * @param date la data
   * @return il personReperibilityDay relativo al tipo e alla data passati come parametro.
   */
  public PersonReperibilityDay getPersonReperibilityDayByTypeAndDate(
      PersonReperibilityType type, LocalDate date) {
    QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
    JPQLQuery query =
        getQueryFactory().from(prd)
          .where(prd.date.eq(date).and(prd.reperibilityType.eq(type)));
    return query.singleResult(prd);
  }

  /**
   * @return la lista dei personReperibilityDay nel periodo compreso tra begin e to e con tipo type.
   */
  public List<PersonReperibilityDay> getPersonReperibilityDayFromPeriodAndType(
      LocalDate begin, LocalDate to, PersonReperibilityType type, Optional<PersonReperibility> pr) {
    QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
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
   *     il giorno day.
   */
  public long deletePersonReperibilityDay(PersonReperibilityType type, LocalDate day) {
    QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
    Long deleted =
        getQueryFactory().delete(prd)
          .where(prd.reperibilityType.eq(type).and(prd.date.eq(day))).execute();
    return deleted;
  }
  
  /**
   * @return la lista dei 'personReperibilityDay' della persona 'person' di tipo 'type' presenti nel
   *     periodo tra 'begin' e 'to'.
   */
  public List<PersonReperibilityDay> getPersonReperibilityDaysByPeriodAndType(
      LocalDate begin, LocalDate to, PersonReperibilityType type, Person person) {
    final QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
    JPQLQuery query = getQueryFactory().from(prd)
            .where(prd.date.between(begin, to)
                    .and(prd.reperibilityType.eq(type))
                    .and(prd.personReperibility.person.eq(person))
            )
            .orderBy(prd.date.asc());
    return query.list(prd);
  }
  
  /**
   * 
   * @param personReperibilityDayId l'id del giorno di reperibilità
   * @return il personReperibilityDay, se esiste, associato all'id passato come parametro.
   */
  public Optional<PersonReperibilityDay> getPersonReperibilityDayById(
      long personReperibilityDayId) {
    final QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
    JPQLQuery query = getQueryFactory().from(prd).where(prd.id.eq(personReperibilityDayId));
    return Optional.fromNullable(query.singleResult(prd));
  }

  //***************************************************************/
  // Query DAO relative al personReperibilityType                **/
  //***************************************************************/

  /**
   * @return il personReperibilityType relativo all'id passato come parametro.
   */
  public PersonReperibilityType getPersonReperibilityTypeById(Long id) {
    QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
    JPQLQuery query = getQueryFactory().from(prt).where(prt.id.eq(id));
    return query.singleResult(prt);
  }
  
  /**
   * 
   * @return la lista di tutti i PersonReperibilityType presenti sul db.
   */
  public List<PersonReperibilityType> getAllReperibilityType() {
    QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
    JPQLQuery query = getQueryFactory().from(prt).orderBy(prt.description.asc());
    return query.list(prt);
  }
  
  /**
   * 
   * @param office l'ufficio per cui ritornare la lista dei servizi per cui si richiede la 
   *     reperibilità.
   * @return la lista dei servizi per cui si vuole la reperibilità
   */
  public List<PersonReperibilityType> getReperibilityTypeByOffice(
      Office office, Optional<Boolean> isActive) {
    QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
    BooleanBuilder condition = new BooleanBuilder();
    if (isActive.isPresent()) {
      condition.and(prt.disabled.eq(isActive.get()));
    }
    JPQLQuery query = getQueryFactory().from(prt).where(prt.office.eq(office).and(condition));
    return query.list(prt);
  }
  
  /**
   * 
   * @param description il nome del servizio
   * @return il tipo di reperibilità, se esiste, con descrizione uguale a quella passata 
   *     come parametro.
   */
  public Optional<PersonReperibilityType> getReperibilityTypeByDescription(String description, 
      Office office) {
    QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
    JPQLQuery query = getQueryFactory().from(prt)
        .where(prt.description.eq(description).and(prt.office.eq(office)));
    return Optional.fromNullable(query.singleResult(prt));
  }

  //***************************************************************/
  // Query DAO relative al personReperibility                    **/
  //***************************************************************/

  /**
   * @return il PersonReperibility relativo alla persona person e al tipo type passati come
   *     parametro.
   */
  public PersonReperibility getPersonReperibilityByPersonAndType(
      Person person, PersonReperibilityType type) {
    QPersonReperibility pr = QPersonReperibility.personReperibility;
    JPQLQuery query =
        getQueryFactory().from(pr)
          .where(pr.person.eq(person).and(pr.personReperibilityType.eq(type)));
    return query.singleResult(pr);

  }


  /**
   * @return la lista dei personReperibility che hanno come personReperibilityType il tipo passato
   *     come parametro.
   */
  public List<PersonReperibility> getPersonReperibilityByType(PersonReperibilityType type) {
    QPersonReperibility pr = QPersonReperibility.personReperibility;
    JPQLQuery query = getQueryFactory().from(pr).where(pr.personReperibilityType.eq(type));
    return query.list(pr);
  }
  
  /**
   * Ritorna, se esiste, il personReperibility corrispondente all'id passato come parametro.
   * @param id l'id dell'attività di reperibilità
   * @return l'attività di reperibilità associata all'id passato come parametro se presente.
   */
  public Optional<PersonReperibility> getPersonReperibilityById(Long id) {
    QPersonReperibility pr = QPersonReperibility.personReperibility;
    JPQLQuery query = getQueryFactory().from(pr).where(pr.id.eq(id));
    return Optional.fromNullable(query.singleResult(pr));
  }
  
  /**
   * Metodo che ricerca la lista dei PersonReperibility che rispondono al tipo e al periodo
   *     passati come parametro.
   * @param type il tipo di reperibilità 
   * @param from la data da cui cercare
   * @param to la data entro cui cercare
   * @return la lista di personReperibility associati ai parametri passati.
   */
  public List<PersonReperibility> byTypeAndPeriod(PersonReperibilityType type, 
      LocalDate from, LocalDate to) {
    QPersonReperibility pr = QPersonReperibility.personReperibility;
    JPQLQuery query = getQueryFactory()
        .from(pr).where(pr.personReperibilityType.eq(type)
            .and(pr.startDate.loe(from).andAnyOf(pr.endDate.isNull(), pr.endDate.goe(to))));
    return query.list(pr);
  }

  /**
   * Metodo che ritorna la lista delle persone reperibili per la sede alla data.
   * @param office la sede per cui si stanno cercando i reperibili
   * @param date la data in cui stiamo stiamo facendo la richiesta
   * @return la lista di PersonReperibility per i parametri passati.
   */
  public List<PersonReperibility> byOffice(Office office, LocalDate date) {
    QPersonReperibility pr = QPersonReperibility.personReperibility;
    JPQLQuery query = getQueryFactory().from(pr)
        .where(pr.person.office.eq(office)
            .and(pr.startDate.isNotNull().andAnyOf(pr.endDate.isNull(), pr.endDate.goe(date))));
    return query.list(pr);
  }
  
  /**
   * Metodo di ricerca che ritorna, se esiste, l'attività associata ai parametri specificati.
   * @param person la persona di cui si vuole l'attività associata
   * @param date la data da verificare se è presente nel periodo per cui è associato all'attività
   * @param type il tipo di attività
   * @return l'attività associata alla persona nella data specificata.
   */
  public Optional<PersonReperibility> byPersonDateAndType(Person person, 
      LocalDate date, PersonReperibilityType type) {
    QPersonReperibility pr = QPersonReperibility.personReperibility;
    JPQLQuery query = getQueryFactory().from(pr)
        .where(pr.person.eq(person).and(pr.personReperibilityType.eq(type)
            .and(pr.startDate.loe(date).andAnyOf(pr.endDate.isNull(), pr.endDate.goe(date)))));
    return Optional.fromNullable(query.singleResult(pr));
  }
  
 
}
