package dao;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.DateUtility;

import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Person;
import models.WorkingTimeType;
import models.query.QContract;
import models.query.QContractStampProfile;
import models.query.QContractWorkingTimeType;

import org.joda.time.LocalDate;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

/**
 * Dao per i contract.
 *
 * @author dario
 */
public class ContractDao extends DaoBase {

  private final IWrapperFactory factory;

  @Inject
  ContractDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory factory) {
    super(queryFactory, emp);
    this.factory = factory;
  }

  /**
   * @return il contratto corrispondente all'id passato come parametro.
   */
  public Contract getContractById(Long id) {
    QContract contract = QContract.contract;
    final JPQLQuery query = getQueryFactory().from(contract)
        .where(contract.id.eq(id));
    return query.singleResult(contract);
  }

  /**
   * @return la lista di contratti che sono attivi nel periodo compreso tra begin e end.
   */
  public List<Contract> getActiveContractsInPeriod(Optional<List<Person>> people,
      LocalDate begin, Optional<LocalDate> end) {

    final QContract contract = QContract.contract;

    final BooleanBuilder condition = new BooleanBuilder()
        .andAnyOf(contract.endContract.isNull().and(contract.endDate.isNull()),
            contract.endContract.isNull().and(contract.endDate.goe(begin)),
            contract.endDate.isNull().and(contract.endContract.goe(begin)));

    if (end.isPresent()) {
      condition.and(contract.beginDate.loe(end.get()));
    }

    if (people.isPresent()) {
      condition.and(contract.person.in(people.get()));
    }

    return getQueryFactory().from(contract).where(condition).list(contract);
  }

  public List<Contract> getActiveContractsInPeriod(LocalDate begin, Optional<LocalDate> end) {
    return getActiveContractsInPeriod(Optional.absent(), begin, end);
  }

  public List<Contract> getActiveContractsInPeriod(Person person, LocalDate begin,
      Optional<LocalDate> end) {
    return getActiveContractsInPeriod(Optional.of(ImmutableList.of(person)), begin, end);
  }

  /**
   * @return la lista di contratti associati alla persona person passata come parametro ordinati per
   * data inizio contratto.
   */
  public List<Contract> getPersonContractList(Person person) {
    QContract contract = QContract.contract;
    final JPQLQuery query = getQueryFactory().from(contract)
        .where(contract.person.eq(person)).orderBy(contract.beginDate.asc());
    return query.list(contract);
  }


  /**
   * @return la lista di contratti associata al workingTimeType passato come parametro.
   */
  public List<Contract> getContractListByWorkingTimeType(WorkingTimeType wtt) {
    QContractWorkingTimeType cwtt = QContractWorkingTimeType.contractWorkingTimeType;
    QContract contract = QContract.contract;
    final JPQLQuery query = getQueryFactory().from(contract)
        .leftJoin(contract.contractWorkingTimeType, cwtt).where(cwtt.workingTimeType.eq(wtt));

    return query.list(contract);
  }


  // Per la delete quindi per adesso permettiamo l'eliminazione solo di contratti particolari
  // di office bisogna controllare che this non sia default ma abbia l'associazione con office

  public List<Contract> getAssociatedContract(WorkingTimeType wtt) {

    List<Contract> contractList = getContractListByWorkingTimeType(wtt);

    return contractList;
  }

  /**
   * @return il contratto attivo per quella persona alla date date.
   */
  public Contract getContract(LocalDate date, Person person) {

    for (Contract c : person.contracts) {
      if (DateUtility.isDateIntoInterval(date, factory.create(c).getContractDateInterval())) {
        return c;
      }
    }
    /*
    //FIXME sommani aprile 2014, lui ha due contratti ma nello heap ce ne sono due identici e
    //manca quello nuovo.
    List<Contract> contractList = getPersonContractList(person);
    for(Contract c : contractList){
      if(DateUtility.isDateIntoInterval(date, factory.create(c).getContractDateInterval()))
        return c;
    }
    */
    //-----------------------
    return null;
  }

  /**
   * @return la lista dei contractStampProfile relativi alla persona person o al contratto contract
   * passati come parametro e ordinati per data inizio del contractStampProfile La funzione permette
   * di scegliere quale dei due parametri indicare per effettuare la ricerca. Sono mutuamente
   * esclusivi.
   */
  public List<ContractStampProfile> getPersonContractStampProfile(Optional<Person> person,
      Optional<Contract> contract) {
    QContractStampProfile csp = QContractStampProfile.contractStampProfile;
    final BooleanBuilder condition = new BooleanBuilder();
    if (person.isPresent()) {
      condition.and(csp.contract.person.eq(person.get()));
    }
    if (contract.isPresent()) {
      condition.and(csp.contract.eq(contract.get()));
    }
    final JPQLQuery query = getQueryFactory().from(csp)
        .where(condition).orderBy(csp.beginDate.asc());
    return query.list(csp);

  }

  /**
   * @return il contractStampProfile relativo all'id passato come parametro.
   */
  public ContractStampProfile getContractStampProfileById(Long id) {
    QContractStampProfile csp = QContractStampProfile.contractStampProfile;
    final JPQLQuery query = getQueryFactory().from(csp)
        .where(csp.id.eq(id));
    return query.singleResult(csp);
  }


  //***********************************************************************************************/
  // Inserisco in questa parte del Dao le query relative ai ContractWorkingTimeType per evitare   */
  // di creare una classe specifica che contenga una o al più due query e risulti pertanto troppo */
  // dispersiva                                                                                   */
  //    *******************************************************************************************/


  /**
   * @return la lista di contractWorkingTimeType associati al contratto passato come parametro.
   */
  public List<ContractWorkingTimeType> getContractWorkingTimeTypeList(Contract contract) {
    QContractWorkingTimeType cwtt = QContractWorkingTimeType.contractWorkingTimeType;
    final JPQLQuery query = getQueryFactory().from(cwtt)
        .where(cwtt.contract.eq(contract));
    return query.list(cwtt);

  }
}
