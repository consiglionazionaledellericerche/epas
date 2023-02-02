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
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.Contract;
import models.ContractMandatoryTimeSlot;
import models.ContractStampProfile;
import models.Office;
import models.Person;
import models.WorkingTimeType;
import models.query.QContract;
import models.query.QContractMandatoryTimeSlot;
import models.query.QContractStampProfile;
import models.query.QContractWorkingTimeType;
import org.joda.time.LocalDate;

/**
 * Dao per i contract.
 *
 * @author Dario Tagliaferri
 * @author Cristian Lucchesi
 */
@Slf4j
public class ContractDao extends DaoBase {

  private final IWrapperFactory factory;

  @Inject
  ContractDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory factory) {
    super(queryFactory, emp);
    this.factory = factory;
  }

  /**
   * Il contratto relativo all'id passato come parametro e facendo la joinFetch con la persona.
   *
   * @return il contratto corrispondente all'id passato come parametro.
   */
  public Contract byId(Long id) {
    QContract contract = QContract.contract;
    return getQueryFactory().selectFrom(contract)
        .join(contract.person).fetchJoin()
        .where(contract.id.eq(id)).fetchOne();
  }

  /**
   * Il contratto relativo all'id passato come parametro.
   *
   * @return il contratto corrispondente all'id passato come parametro.
   */
  public Contract getContractById(Long id) {
    QContract contract = QContract.contract;
    return getQueryFactory().selectFrom(contract)
        .where(contract.id.eq(id)).fetchOne();
  }

  /**
   * La lista dei contratti attivi che hanno un periodo attivo con associato
   * il tipo di orario di lavoro indicato.
   */
  public List<Contract> getAllAssociatedActiveContracts(WorkingTimeType cwtt) {
    final QContract contract = QContract.contract;
    final QContractWorkingTimeType qCwtt = QContractWorkingTimeType.contractWorkingTimeType;

    val now = LocalDate.now();
    final BooleanBuilder condition = new BooleanBuilder()
        .andAnyOf(contract.endContract.isNull().and(contract.endDate.isNull()),
            contract.endContract.isNull().and(contract.endDate.goe(now)),
            contract.endDate.isNull().and(contract.endContract.goe(now)),
            contract.endDate.goe(now).and(contract.endContract.goe(now)));
    
    val cwttCondition = new BooleanBuilder();
    cwttCondition.andAnyOf(
        qCwtt.id.eq(cwtt.id), 
        qCwtt.beginDate.before(now),
        qCwtt.endDate.isNull().or(qCwtt.endDate.after(now)));
    cwttCondition.and(qCwtt.workingTimeType.eq(cwtt));
    return getQueryFactory().selectFrom(contract)
        .join(contract.contractWorkingTimeType, qCwtt).on(cwttCondition).where(condition).fetch();
  }

  /**
   * La lista di contratti che sono attivi nel periodo compreso tra begin e end.
   *
   * @param people la lista di persone (opzionale)
   * @param begin  la data di inizio
   * @param end    la data di fine (opzionale)
   * @param office la sede (opzionale)
   * @return la lista di contratti che sono attivi nel periodo compreso tra begin e end.
   */
  private List<Contract> getActiveContractsInPeriod(Optional<List<Person>> people,
      LocalDate begin, Optional<LocalDate> end, Optional<Office> office) {

    final QContract contract = QContract.contract;

    final BooleanBuilder condition = new BooleanBuilder()
        .andAnyOf(contract.endContract.isNull().and(contract.endDate.isNull()),
            contract.endContract.isNull().and(contract.endDate.goe(begin)),
            contract.endDate.isNull().and(contract.endContract.goe(begin)),
            contract.endDate.goe(begin).and(contract.endContract.goe(begin)));

    if (end.isPresent()) {
      condition.and(contract.beginDate.loe(end.get()));
    }

    if (people.isPresent()) {
      condition.and(contract.person.in(people.get()));
    }

    if (office.isPresent()) {
      condition.and(contract.person.office.eq(office.get()));
    }

    return getQueryFactory().selectFrom(contract).where(condition).fetch();
  }

  public List<Contract> getActiveContractsInPeriod(LocalDate begin, Optional<LocalDate> end,
      Optional<Office> office) {
    return getActiveContractsInPeriod(Optional.absent(), begin, end, office);
  }

  public List<Contract> getActiveContractsInPeriod(Person person, LocalDate begin,
      Optional<LocalDate> end) {
    return getActiveContractsInPeriod(
        Optional.of(ImmutableList.of(person)), begin, end, Optional.absent());
  }

  /**
   * La lista di contratti che sono scaduti nel periodo compreso tra begin e end.
   *
   * @param people la lista di persone (opzionale)
   * @param begin  la data di inizio
   * @param end    la data di fine
   * @param office la sede (opzionale)
   * @return la lista di contratti che sono scaduti nel periodo compreso tra begin e end.
   */
  private List<Contract> getExpiredContractsInPeriod(Optional<List<Person>> people,
      LocalDate begin, LocalDate end, Optional<Office> office) {

    final QContract contract = QContract.contract;

    final BooleanBuilder condition = new BooleanBuilder().andAnyOf(
        contract.endDate.goe(begin).and(contract.endDate.loe(end)),
        contract.endContract.goe(begin).and(contract.endContract.loe(end)));

    if (people.isPresent()) {
      condition.and(contract.person.in(people.get()));
    }

    if (office.isPresent()) {
      condition.and(contract.person.office.eq(office.get()));
    }

    return getQueryFactory().selectFrom(contract).where(condition).fetch();
  }

  public List<Contract> getExpiredContractsInPeriod(LocalDate begin, LocalDate end,
      Optional<Office> office) {
    return getExpiredContractsInPeriod(Optional.absent(), begin, end, office);
  }

  public List<Contract> getExpiredContractsInPeriod(Person person, LocalDate begin,
      LocalDate end) {
    return getExpiredContractsInPeriod(
        Optional.of(ImmutableList.of(person)), begin, end, Optional.absent());
  }

  /**
   * La lista di contratti della persona.
   *
   * @return la lista di contratti associati alla persona person passata come parametro ordinati per
   *     data inizio contratto.
   */
  public List<Contract> getPersonContractList(Person person) {
    QContract contract = QContract.contract;
    return getQueryFactory().selectFrom(contract)
        .where(contract.person.eq(person)).orderBy(contract.beginDate.asc()).fetch();
  }

  /**
   * Il contratto di una persona ad una certa data.
   *
   * @return il contratto attivo per quella persona alla data date.
   */
  public Contract getContract(LocalDate date, Person person) {
    // FIXME ci sono alcuni casi nei quali i valori in person.contracts (in questo metodo) non sono
    // allineati con tutti i record presenti sul db e capita che viene restituito un valore nullo
    // incongruente con i dati presenti
    // TODO da sostituire con una query?
    for (Contract c : person.getContracts()) {
      if (DateUtility.isDateIntoInterval(date, factory.create(c).getContractDateInterval())) {
        return c;
      }
    }
    return null;
  }

  /**
   * Se presente preleva l'eventuale fascia di presenza obbligatoria di una persona in una data
   * indicata.
   *
   * @param date     la data in cui cercare la fascia obbligatoria
   * @param personId l'id della persona di cui cercare la fascia obbligatoria
   * @return la fascia oraria obbligatoria se presente, Optional.absent() altrimenti.
   */
  public Optional<ContractMandatoryTimeSlot> getContractMandatoryTimeSlot(
      LocalDate date, Long personId) {
    QContract contract = QContract.contract;
    QContractMandatoryTimeSlot contractMandatoryTimeSlot =
        QContractMandatoryTimeSlot.contractMandatoryTimeSlot;
    Person person = Person.findById(personId);
    val cmts = getQueryFactory().selectFrom(contractMandatoryTimeSlot)
        .join(contractMandatoryTimeSlot.contract, contract)
        .where(
            contract.person.eq(person), contract.beginDate.before(date)
                .or(contract.beginDate.eq(date)),
            contract.endContract.isNull().or(contract.endContract.after(date)
                .or(contract.endContract.eq(date))),
            contract.endDate.isNull().or(contract.endDate.after(date)
                .or(contract.endDate.eq(date))),
            contractMandatoryTimeSlot.beginDate.before(date)
                .or(contractMandatoryTimeSlot.beginDate.eq(date)),
            contractMandatoryTimeSlot.endDate.isNull()
                .or(contractMandatoryTimeSlot.endDate.after(date))
                .or(contractMandatoryTimeSlot.endDate.eq(date)))
        .fetchOne();
    return Optional.fromNullable(cmts);
  }

  /**
   * La lista di contractStampProfile di una persona o di un contratto.
   *
   * @param person   la persona (opzionale)
   * @param contract il contratto (opzionale)
   * @return la lista dei contractStampProfile relativi alla persona person o al contratto contract
   *     passati come parametro e ordinati per data inizio del contractStampProfile. La funzione
   *     permette di scegliere quale dei due parametri indicare per effettuare la ricerca. Sono
   *     mutuamente esclusivi.
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
    return getQueryFactory().selectFrom(csp)
        .where(condition).orderBy(csp.beginDate.asc()).fetch();

  }

  /**
   * Il contractstampprofile associato all'id passato.
   *
   * @return il contractStampProfile relativo all'id passato come parametro.
   */
  public ContractStampProfile getContractStampProfileById(Long id) {
    QContractStampProfile csp = QContractStampProfile.contractStampProfile;
    return getQueryFactory().selectFrom(csp)
        .where(csp.id.eq(id)).fetchOne();
  }

  /**
   * Ritorna il contratto precedente.
   *
   * @param actualContract il contratto attuale del dipendente
   * @return il contratto precedente
   */
  public Optional<Contract> getPreviousContract(Contract actualContract) {
    Verify.verifyNotNull(actualContract);
    Contract previousContract = null;
    List<Contract> contractList = getPersonContractList(actualContract.getPerson());

    for (Contract contract : contractList) {
      if (contract.getId().equals(actualContract.getId())) {
        log.trace("scarto il contratto id = {}, perché uguale al contratto corrente id = {}",
            contract.getId(), actualContract.getId());
        continue;
      }

      if (contract.calculatedEnd() != null
          && contract.calculatedEnd().isBefore(actualContract.getBeginDate()) 
          && (previousContract == null 
                || contract.getBeginDate().isAfter(previousContract.getEndDate()))) {
        previousContract = contract;
      }
    }
    return Optional.fromNullable(previousContract);
  }

  /**
   * Lista dei contratti che hanno impostato come previousContract se stesso.
   *
   * @return la lista dei contratti che hanno impostato come previousContract 
   *      se stesso (erroneamente).
   */
  public List<Contract> getContractsWithWrongPreviousContract() {
    QContract contract = QContract.contract;
    BooleanBuilder contractEqPreviousCondition = 
        new BooleanBuilder(
            contract.previousContract.isNotNull().and(
                contract.previousContract.id.eq(contract.id)));
    BooleanBuilder previousConditionAfterCurrentContract = 
        new BooleanBuilder(
            contract.previousContract.isNotNull().and(
                contract.beginDate.before(contract.previousContract.beginDate)));
    return getQueryFactory().selectFrom(contract)
        .where(contractEqPreviousCondition.or(previousConditionAfterCurrentContract)).fetch();
  }
}