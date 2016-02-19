package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import it.cnr.iit.epas.DateInterval;

import manager.services.mealTickets.MealTicketsServiceImpl.MealTicketOrder;

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
   * @return il mealTicket corrispondente al codice code passato come parametro
   */
  public MealTicket getMealTicketByCode(String code) {

    QMealTicket mealTicket = QMealTicket.mealTicket;

    final JPQLQuery query = getQueryFactory()
            .from(mealTicket)
            .where(mealTicket.code.eq(code));

    return query.singleResult(mealTicket);
  }


  /**
   * MealTickets assegnati alla persona nella finestra temporale specificata.<br>
   * Ordinati in base al tipo di ordinamento enumerato e come secondo ordine per codice.
   * 
   * @param contract contratto
   * @param interval intervallo
   * @param order ordinamento
   * @param returned se voglio quelli riconsegnati o quelli disponibili
   * @return
   */
  public List<MealTicket> getMealTicketAssignedToPersonIntoInterval(Contract contract, 
      DateInterval interval, MealTicketOrder order, boolean returned) {

    final QMealTicket mealTicket = QMealTicket.mealTicket;

    final JPQLQuery query = getQueryFactory()
        .from(mealTicket)
        .where(mealTicket.contract.eq(contract))
        .where(mealTicket.date.goe(interval.getBegin()))
        .where(mealTicket.date.loe(interval.getEnd()));
    
    if (returned) {
      query.where(mealTicket.returned.eq(true));
    } else {
      query.where(mealTicket.returned.eq(false));
    }
    
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
   * @param office sede
   * @return data
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
   * FIXME: utilizzare getMealTicketAssignedToPersonIntoInterval con gli opportuni parametri
   * Ritorna la lista dei buoni pasto associati al contratto ordinata per data di consegna in ordine
   * decrescente (da quelli consegnati per ultimi a quelli consegnati per primi).
   * @param contract
   * @return
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
   * utilizzare getMealTicketAssignedToPersonIntoInterval con gli opportuni parametri 
   * I buoni pasto di un blocco ordinati per codice asc. 
   * Se contract presente i soli associati a quel contratto.
   * @param codeBlock il codice del blocco.
   * @param contract contratto
   * @return la lista dei meal tickets nel blocco.
   */
  public List<MealTicket> getMealTicketsInCodeBlock(Integer codeBlock, 
      Optional<Contract> contract) {

    final QMealTicket mealTicket = QMealTicket.mealTicket;
    
    final JPQLQuery query = getQueryFactory()
            .from(mealTicket)
            .where(mealTicket.block.eq(codeBlock));
    
    if (contract.isPresent()) {
      query.where(mealTicket.contract.eq(contract.get()));
    }
    
    query.orderBy(mealTicket.code.asc());

    return query.list(mealTicket);

  }

  /**
   * I buoni pasto che matchano il codice passato. Se office presente i soli appartenti a contratti
   * di quell'office.
   * @param code codice match
   * @param office sede
   * @return elenco 
   */
  public List<MealTicket> getMealTicketsMatchCodeBlock(String code, Optional<Office> office) {
    
    QMealTicket mealTicket = QMealTicket.mealTicket;
    QContract contract = QContract.contract;
    QPerson person = QPerson.person;
    
    final JPQLQuery query = getQueryFactory()
        .from(mealTicket);
    
    if (office.isPresent()) {
      query.leftJoin(mealTicket.contract, contract);
      query.leftJoin(contract.person, person);
    }

    query.where(mealTicket.code.like("%"+code+"%"));
    if (office.isPresent()) {
      query.where(person.office.eq(office.get()).and(mealTicket.returned.eq(true)));
    }
    
    query.orderBy(mealTicket.code.asc());

    return query.list(mealTicket);
    
  }

}
