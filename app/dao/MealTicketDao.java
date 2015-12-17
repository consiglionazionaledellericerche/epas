package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import it.cnr.iit.epas.DateInterval;

import manager.MealTicketManager.MealTicketOrder;

import models.Contract;
import models.MealTicket;
import models.Office;
import models.query.QContract;
import models.query.QMealTicket;
import models.query.QPerson;

import org.joda.time.LocalDate;

import java.util.List;

import javax.persistence.EntityManager;

/**
 * DAO per i MealTicket.
 *
 * @author alessandro
 */
public class MealTicketDao extends DaoBase {

  @Inject
  MealTicketDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }
  
  /**
   * mealTickt assegnati alla persona nella finestra temporale specificata.<br>
   * Ordinati per data di scadenza (in base a expireDateOrder) e per codice blocco.
   * @param contract contratto
   * @param interval intervallo
   * @param expireDateOrder true asc, false desc
   * @return lista di buoni pasto.
   */
  public List<MealTicket> getMealTicketAssignedToPersonIntoInterval(
          Contract contract, DateInterval interval, MealTicketOrder order) {

    final QMealTicket mealTicket = QMealTicket.mealTicket;

    final JPQLQuery query = getQueryFactory()
        .from(mealTicket)
        .where(mealTicket.contract.eq(contract))
        .where(mealTicket.date.goe(interval.getBegin()))
        .where(mealTicket.date.loe(interval.getEnd()));
    
    if (order.equals(MealTicketOrder.ORDER_BY_EXPIRE_DATE_ASC)) {
      query.orderBy(mealTicket.expireDate.asc());
    } else if (order.equals(MealTicketOrder.ORDER_BY_DELIVERY_DATE_DESC)) {
      query.orderBy(mealTicket.date.desc());
    }
    
    query.orderBy(mealTicket.code.asc());

    return query.list(mealTicket);
  }

  /**
   * La scadenza massima precedentemente assegnata ai buoni pasto inseriti per le persone
   * appartenenti all'office passato come argomento.
   */
  public LocalDate getFurtherExpireDateInOffice(Office office) {

    final QMealTicket qmt = QMealTicket.mealTicket;
    final QPerson qp = QPerson.person;
    final QContract qc = QContract.contract;

    final JPQLQuery query = getQueryFactory()
            .from(qmt)
            .leftJoin(qmt.contract, qc)
            .leftJoin(qc.person, qp)
            .where(qp.office.id.eq(office.id))
            .groupBy(qmt.expireDate)
            .orderBy(qmt.expireDate.desc());

    List<LocalDate> date = query.list(qmt.expireDate);

    if (date.size() == 0) {
      return null;
    }

    return date.get(0);

    //FIXME trovare un modo di selezionare solo il primo elemento
  }

  /**
   * Ritorna la lista dei buoni pasto associati al contratto ordinata per data di consegna in ordine
   * decrescente (da quelli consegnati per ultimi a quelli consegnati per primi).
   */
  public List<MealTicket> getOrderedMealTicketInContract(Contract contract) {
    final QMealTicket mealTicket = QMealTicket.mealTicket;

    final JPQLQuery query = getQueryFactory()
            .from(mealTicket)
            .where(mealTicket.contract.eq(contract))
            .orderBy(mealTicket.date.desc());

    return query.list(mealTicket);


  }
  
  /**
   * I buoni pasto del blocco ordinati per codice asc.
   * @param codeBlock il codice del blocco.
   * @return la lista dei meal tickets nel blocco.
   */
  public List<MealTicket> getMealTicketsInCodeBlock(Integer codeBlock) {

    final QMealTicket mealTicket = QMealTicket.mealTicket;

    final JPQLQuery query = getQueryFactory()
            .from(mealTicket)
            .where(mealTicket.block.eq(codeBlock))
            .orderBy(mealTicket.code.asc());

    return query.list(mealTicket);

  }

  /**
   * @return il mealTicket corrispondente al codice code passato come parametro
   */
  public MealTicket getMealTicketByCode(String code) {

    QMealTicket mealTicket = QMealTicket.mealTicket;

    final JPQLQuery query = getQueryFactory()
            .from(mealTicket)
            .where(mealTicket.code.eq(code));

    return query.singleResult(mealTicket);
  }


}
