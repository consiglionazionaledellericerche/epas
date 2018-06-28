package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import java.util.List;

import javax.persistence.EntityManager;

import models.Person;
import models.TimeVariation;
import models.query.QTimeVariation;

import org.joda.time.LocalDate;

public class TimeVariationDao extends DaoBase {

  @Inject
  TimeVariationDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }
  
  /**
   * Metodo che ritorna la lista delle variazioni temporali nell'arco di tempo specificato relative
   * alla persona passata come parametro.
   * @param person la persona cui sono state assegnate le variazioni temporali
   * @param begin la data di inizio da cui cercare
   * @param end la data di fine da cui cercare
   * @return la lista delle variazioni temporali assegnate alla persona per recuperare
   *      i riposi compensativi per chiusura ente nell'intervallo temporale specificato.
   */
  public List<TimeVariation> getByPersonAndPeriod(Person person, LocalDate begin, LocalDate end) {
    final QTimeVariation timeVariation = QTimeVariation.timeVariation1;
    final JPQLQuery query = getQueryFactory().from(timeVariation)
        .where(timeVariation.absence.personDay.person.eq(person)
            .and(timeVariation.dateVariation.between(begin, end)));
    return query.list(timeVariation);
        
  }
  
  /**
   * Metodo che ritorna la variazione oraria recuperata tramite l'id.
   * @param timeVariationId l'identificativo della variazione oraria
   * @return la variazione oraria associata all'id passato come parametro.
   */
  public TimeVariation getById(long timeVariationId) {
    final QTimeVariation timeVariation = QTimeVariation.timeVariation1;
    final JPQLQuery query = getQueryFactory().from(timeVariation)
        .where(timeVariation.id.eq(timeVariationId));
    return query.singleResult(timeVariation);
  }
}
