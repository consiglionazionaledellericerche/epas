package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import dao.wrapper.IWrapperFactory;
import models.Person;
import models.TeleworkValidation;
import models.query.QTeleworkValidation;

public class TeleworkValidationDao extends DaoBase {

  @Inject
  TeleworkValidationDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory factory) {
    super(queryFactory, emp);
  }
  
  /**
   * Ritorna la lista delle richieste di telelavoro approvate.
   * @param person la persona di cui cercare le richieste
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return la lista delle richiest di telelavoro approvate.
   */
  public List<TeleworkValidation> byPersonYearAndMonth(Person person, int year, int month) {
    final QTeleworkValidation teleworkValidation = QTeleworkValidation.teleworkValidation;
    
    final JPQLQuery<TeleworkValidation> query = getQueryFactory(
        ).selectFrom(teleworkValidation).where(teleworkValidation.person.eq(person)
            .and(teleworkValidation.year.eq(year).and(teleworkValidation.month.eq(month)
                .and(teleworkValidation.approved.isTrue()
                    .and(teleworkValidation.approvationDate.isNotNull())))));
    return query.fetch();
  }
}
