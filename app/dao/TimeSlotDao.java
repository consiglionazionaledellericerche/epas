/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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
import com.querydsl.jpa.JPQLQueryFactory;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Contract;
import models.ContractMandatoryTimeSlot;
import models.Office;
import models.Person;
import models.TimeSlot;
import models.query.QTimeSlot;
import org.joda.time.LocalDate;

/**
 * DAO per TimeSlot.
 */
public class TimeSlotDao extends DaoBase {

  private ContractDao contractDao;
  
  @Inject
  TimeSlotDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp, ContractDao contractDao) {
    super(queryFactory, emp);
    this.contractDao = contractDao;
  }

  /**
   * Il timeslot con id passato come parametro.
   *
   * @param id l'id del timeslot
   * @return il timeSlot, se esiste, con id passato come parametro.
   */
  public Optional<TimeSlot> byId(long id) {
    final QTimeSlot ts = QTimeSlot.timeSlot;
    return Optional.fromNullable(getQueryFactory()
        .selectFrom(ts)
        .where(ts.id.eq(id)).fetchOne());
  }
  
  /**
   * Tutte le fasce di orario attive predefinite (non associate a nessun ufficio).
   */
  public List<TimeSlot> getPredefinedEnabledTimeSlots() {

    final QTimeSlot ts = QTimeSlot.timeSlot;
    return getQueryFactory()
        .selectFrom(ts)
        .where(ts.disabled.eq(false).and(ts.office.isNull())).fetch();
  }

  
  /**
   * Tutti le fasce di orario associate all'office.
   */
  public List<TimeSlot> getEnabledTimeSlotsForOffice(Office office) {

    final QTimeSlot ts = QTimeSlot.timeSlot;
    return getQueryFactory()
        .selectFrom(ts)
        .where(ts.office.isNull().or(ts.office.eq(office))
            .and(ts.disabled.eq(false))).fetch();
  }
  
  /**
   * L'eventuale fascia oraria di presenza obbligatoria per la persona attiva
   * nel giorno.
   *
   * @param date data
   * @param person persona
   * @return la fascia oraria obbligatoria se presente
   */
  public Optional<TimeSlot> getMandatoryTimeSlot(LocalDate date, Person person) {

    Contract contract = contractDao.getContract(date, person);

    if (contract != null) {
      for (ContractMandatoryTimeSlot mts : contract.getContractMandatoryTimeSlots()) {

        if (DateUtility.isDateIntoInterval(date, 
            new DateInterval(mts.getBeginDate(), mts.getEndDate()))) {
          return Optional.of(mts.getTimeSlot());
        }
      }
    }
    return Optional.absent();
  }
}