package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import models.Office;
import models.StampModificationType;
import models.Stamping;
import models.query.QPerson;
import models.query.QStampModificationType;
import models.query.QStamping;
import org.joda.time.YearMonth;

/**
 * DAO per l'accesso alle informazioni delle timbrature.
 * 
 * @author dario.
 */
public class StampingDao extends DaoBase {

  @Inject
  StampingDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Preleva una timbratura tramite il suo id.
   * 
   * @param id l'id associato alla Timbratura sul db.
   * @return la timbratura corrispondente all'id passato come parametro.
   */
  public Stamping getStampingById(Long id) {
    final QStamping stamping = QStamping.stamping;
    final JPQLQuery query = getQueryFactory().from(stamping)
        .where(stamping.id.eq(id));
    return query.singleResult(stamping);
  }

  /**
   * * @return lo stampModificationType relativo all'id passato come parametro.
   */
  @Deprecated
  public StampModificationType getStampModificationTypeById(Long id) {
    final QStampModificationType smt = QStampModificationType.stampModificationType;

    JPQLQuery query = getQueryFactory().from(smt)
        .where(smt.id.eq(id));
    return query.singleResult(smt);
  }

  
  /** 
   * Lista delle timbrature inserire dall'amministratore in un determinato mese.
   * 
   * @param yearMonth mese di riferimento
   * @param office ufficio
   * @return lista delle timbrature inserite dell'amministratore
   */
  public List<Stamping> adminStamping(YearMonth yearMonth, Office office) {
    final QStamping stamping = QStamping.stamping;
    final QPerson person = QPerson.person;
    final JPQLQuery query = getQueryFactory().from(stamping)
        .join(stamping.personDay.person, person)
        .where(stamping.markedByAdmin.eq(true)
            .and(stamping.personDay.date.goe(yearMonth.toLocalDate(1)))
            .and(stamping.personDay.date
                  .loe(yearMonth.toLocalDate(1).dayOfMonth()
                      .withMaximumValue()))
            .and(person.office.isNotNull())
            .and(person.office.eq(office)))
        .orderBy(person.surname.asc(), stamping.personDay.date.asc());
    return query.list(stamping);
  }
}
