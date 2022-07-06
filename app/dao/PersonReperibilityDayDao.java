/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dao;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Office;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.query.QPersonReperibility;
import models.query.QPersonReperibilityDay;
import models.query.QPersonReperibilityType;
import org.joda.time.LocalDate;

/**
 * Dao per i PersonReperibilityDay.
 *
 * @author Dario Tagliaferri
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
   *
   * @param person la persona
   * @param date   la data
   * @return un personReperibilityDay nel caso in cui la persona person in data date fosse
   * reperibile. Null altrimenti.
   */
  public Optional<PersonReperibilityDay> getPersonReperibilityDay(Person person, LocalDate date) {
    QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
    final PersonReperibilityDay result = getQueryFactory().selectFrom(prd)
        .where(prd.personReperibility.person.eq(person).and(prd.date.eq(date)))
        .fetchOne();

    return Optional.fromNullable(result);

  }

  /**
   * Metodo che ritorna, una lista di personreperibilityday che rispondono ai parametri passati.
   *
   * @param person la persona
   * @param date   la data
   * @return una lista di personReperibilityDay nel caso in cui la persona person in data date fosse
   * reperibile. Null altrimenti.
   */
  public List<PersonReperibilityDay> getPersonReperibilityDayByPerson(Person person,
      LocalDate date) {
    QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
    return getQueryFactory().selectFrom(prd)
        .where(prd.personReperibility.person.eq(person)
            .and(prd.personReperibility.startDate.loe(date)
                .andAnyOf(prd.personReperibility.endDate.isNull(),
                    prd.personReperibility.endDate.goe(date))))
        .fetch();
  }

  /**
   * Metodo che ritorna il giorno di reperibilità associato al tipo e alla data passati.
   *
   * @param type il tipo di reperibilità
   * @param date la data
   * @return il personReperibilityDay relativo al tipo e alla data passati come parametro.
   */
  public PersonReperibilityDay getPersonReperibilityDayByTypeAndDate(
      PersonReperibilityType type, LocalDate date) {
    QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
    return getQueryFactory().selectFrom(prd)
        .where(prd.date.eq(date).and(prd.reperibilityType.eq(type)))
        .fetchOne();
  }

  /**
   * La lista di giorni di reperibilità per la persona nell'intervallo begin-to.
   *
   * @param begin la data di inizio
   * @param to    la data di fine
   * @param type  il tipo di reperibilità
   * @param pr    (opzionale) la reperibilità
   * @return la lista dei personReperibilityDay nel periodo compreso tra begin e to e con tipo type.
   */
  public List<PersonReperibilityDay> getPersonReperibilityDayFromPeriodAndType(
      LocalDate begin, LocalDate to, PersonReperibilityType type, Optional<PersonReperibility> pr) {
    QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
    BooleanBuilder condition = new BooleanBuilder();
    if (pr.isPresent()) {
      condition.and(prd.personReperibility.eq(pr.get()));
    }
    return getQueryFactory().selectFrom(prd).where(condition.and(prd.date.between(begin, to)
            .and(prd.reperibilityType.eq(type)))).orderBy(prd.date.asc())
        .fetch();
  }


  /**
   * Cancella i giorni di reperibilità sulla reperibilità nel giorno.
   *
   * @param type tipo di reperibilità
   * @param day  il giorno da considerare
   * @return il numero di personReperibilityDay cancellati che hanno come parametri il tipo type e
   * il giorno day.
   */
  public long deletePersonReperibilityDay(PersonReperibilityType type, LocalDate day) {
    QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
    return getQueryFactory().delete(prd)
        .where(prd.reperibilityType.eq(type).and(prd.date.eq(day))).execute();
  }


  /**
   * La lista dei giorni di reperibilità della persona nell'attività type tra begin e to.
   *
   * @param begin  la data di inizio
   * @param to     la data di fine
   * @param type   il tipo di reperibilità
   * @param person la persona
   * @return la lista dei 'personReperibilityDay' della persona 'person' di tipo 'type' presenti nel
   * periodo tra 'begin' e 'to'.
   */
  public List<PersonReperibilityDay> getPersonReperibilityDaysByPeriodAndType(
      LocalDate begin, LocalDate to, PersonReperibilityType type, Person person) {
    final QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
    return getQueryFactory().selectFrom(prd)
        .where(prd.date.between(begin, to)
            .and(prd.reperibilityType.eq(type))
            .and(prd.personReperibility.person.eq(person))
        ).orderBy(prd.date.asc())
        .fetch();
  }

  /**
   * Il giorno di reperibilità, se esiste, con id passato come parametro.
   *
   * @param personReperibilityDayId l'id del giorno di reperibilità
   * @return il personReperibilityDay, se esiste, associato all'id passato come parametro.
   */
  public Optional<PersonReperibilityDay> getPersonReperibilityDayById(
      long personReperibilityDayId) {
    final QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
    final PersonReperibilityDay result = getQueryFactory().selectFrom(prd)
        .where(prd.id.eq(personReperibilityDayId)).fetchFirst();
    return Optional.fromNullable(result);
  }

  //***************************************************************/
  // Query DAO relative al personReperibilityType                **/
  //***************************************************************/

  /**
   * Il tipo di reperibilità con id passato come parametro.
   *
   * @param id l'id del tipo di reperibilità
   * @return il personReperibilityType relativo all'id passato come parametro.
   */
  public PersonReperibilityType getPersonReperibilityTypeById(Long id) {
    final QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
    return getQueryFactory().selectFrom(prt).where(prt.id.eq(id)).fetchFirst();
  }

  /**
   * La lista di tutti i tipi di reperibilità.
   *
   * @return la lista di tutti i PersonReperibilityType presenti sul db.
   */
  public List<PersonReperibilityType> getAllReperibilityType() {
    QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
    return getQueryFactory().selectFrom(prt).orderBy(prt.description.asc())
        .fetch();
  }

  /**
   * La lista dei servizi di reperibilità della sede.
   *
   * @param office   l'ufficio per cui ritornare la lista dei servizi per cui si richiede la
   *                 reperibilità.
   * @param isActive se è attiva
   * @return la lista dei servizi per cui si vuole la reperibilità
   */
  public List<PersonReperibilityType> getReperibilityTypeByOffice(
      Office office, Optional<Boolean> isActive) {
    QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
    BooleanBuilder condition = new BooleanBuilder();
    if (isActive.isPresent()) {
      condition.and(prt.disabled.eq(isActive.get()));
    }
    return getQueryFactory().selectFrom(prt).where(prt.office.eq(office).and(condition)).fetch();
  }

  /**
   * Il tipo di reperibilità, se esiste, appartenente alla sede con la descrizione passata.
   *
   * @param description il nome del servizio
   * @param office      la sede su cui cercare
   * @return il tipo di reperibilità, se esiste, con descrizione uguale a quella passata come
   * parametro.
   */
  public Optional<PersonReperibilityType> getReperibilityTypeByDescription(String description,
      Office office) {
    final QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
    final PersonReperibilityType result = getQueryFactory().selectFrom(prt)
        .where(prt.description.eq(description).and(prt.office.eq(office))).fetchFirst();
    return Optional.fromNullable(result);
  }

  //***************************************************************/
  // Query DAO relative al personReperibility                    **/
  //***************************************************************/

  /**
   * L'associazione persona/reperibilità relativa ai parametri passati.
   *
   * @param person la persona da cercare
   * @param type   il tipo di reperibilità
   * @return il PersonReperibility relativo alla persona person e al tipo type passati come
   * parametro.
   */
  public PersonReperibility getPersonReperibilityByPersonAndType(
      Person person, PersonReperibilityType type) {
    final QPersonReperibility pr = QPersonReperibility.personReperibility;
    return getQueryFactory().selectFrom(pr)
        .where(pr.person.eq(person).and(pr.personReperibilityType.eq(type))).fetchFirst();
  }


  /**
   * La lista dei personReperibility che hanno il tipo passato come parametro.
   *
   * @param type il tipo di reperibilità
   * @return la lista dei personReperibility che hanno come personReperibilityType il tipo passato
   * come parametro.
   */
  public List<PersonReperibility> getPersonReperibilityByType(PersonReperibilityType type) {
    final QPersonReperibility pr = QPersonReperibility.personReperibility;
    return getQueryFactory().selectFrom(pr).where(pr.personReperibilityType.eq(type)).fetch();
  }

  /**
   * Ritorna, se esiste, il personReperibility corrispondente all'id passato come parametro.
   *
   * @param id l'id dell'attività di reperibilità
   * @return l'attività di reperibilità associata all'id passato come parametro se presente.
   */
  public Optional<PersonReperibility> getPersonReperibilityById(Long id) {
    final QPersonReperibility pr = QPersonReperibility.personReperibility;
    final PersonReperibility result = getQueryFactory().selectFrom(pr).where(pr.id.eq(id))
        .fetchOne();
    return Optional.fromNullable(result);
  }

  /**
   * Metodo che ricerca la lista dei PersonReperibility che rispondono al tipo e al periodo passati
   * come parametro.
   *
   * @param type il tipo di reperibilità
   * @param from la data da cui cercare
   * @param to   la data entro cui cercare
   * @return la lista di personReperibility associati ai parametri passati.
   */
  public List<PersonReperibility> byTypeAndPeriod(PersonReperibilityType type,
      LocalDate from, LocalDate to) {
    final QPersonReperibility pr = QPersonReperibility.personReperibility;

    final BooleanBuilder condition = new BooleanBuilder().and(pr.personReperibilityType.eq(type));
    condition.andAnyOf(pr.startDate.loe(from).andAnyOf(pr.endDate.isNull(), pr.endDate.goe(to)),
        pr.startDate.goe(from).and(pr.startDate.loe(to))
            .andAnyOf(pr.endDate.isNull(), pr.endDate.goe(to)));
    return getQueryFactory().selectFrom(pr)
        .where(condition).fetch();
  }

  /**
   * Metodo che ritorna la lista delle persone reperibili per la sede alla data.
   *
   * @param office la sede per cui si stanno cercando i reperibili
   * @param date   la data in cui stiamo stiamo facendo la richiesta
   * @return la lista di PersonReperibility per i parametri passati.
   */
  public List<PersonReperibility> byOffice(Office office, LocalDate date) {
    QPersonReperibility pr = QPersonReperibility.personReperibility;
    return getQueryFactory().selectFrom(pr)
        .where(pr.person.office.eq(office)
            .and(pr.startDate.isNotNull().andAnyOf(pr.endDate.isNull(), pr.endDate.goe(date))))
        .fetch();
  }

  /**
   * Metodo di ricerca che ritorna, se esiste, l'attività associata ai parametri specificati.
   *
   * @param person la persona di cui si vuole l'attività associata
   * @param date   la data da verificare se è presente nel periodo per cui è associato all'attività
   * @param type   il tipo di attività
   * @return l'attività associata alla persona nella data specificata.
   */
  public Optional<PersonReperibility> byPersonDateAndType(Person person,
      LocalDate date, PersonReperibilityType type) {
    final QPersonReperibility pr = QPersonReperibility.personReperibility;
    final PersonReperibility result = getQueryFactory().selectFrom(pr)
        .where(pr.person.eq(person).and(pr.personReperibilityType.eq(type)
            .and(pr.startDate.loe(date).andAnyOf(pr.endDate.isNull(), pr.endDate.goe(date)))))
        .fetchOne();
    return Optional.fromNullable(result);
  }

  /**
   * Ricerca, se esiste, l'attività di reperibilità che praticano la lista di persone passata come
   * parametro.
   *
   * @param list la lista di persone di cui cercare l'attività di reperibilità
   * @return l'attività, se esiste, di cui fanno parte le persone della lista passata.
   */
  public Optional<PersonReperibilityType> byListOfPerson(List<Person> list) {
    final QPersonReperibilityType type = QPersonReperibilityType.personReperibilityType;
    final QPersonReperibility pr = QPersonReperibility.personReperibility;
    final PersonReperibilityType result = getQueryFactory()
        .selectFrom(type).leftJoin(type.personReperibilities, pr)
        .where(pr.person.in(list)).fetchOne();
    return Optional.fromNullable(result);
  }
}