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
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import lombok.val;
import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.query.QContractWorkingTimeType;
import models.query.QWorkingTimeType;
import org.joda.time.LocalDate;

/**
 * Dao per i WorkingTimeType.
 *
 * @author Dario Tagliaferri
 */
public class WorkingTimeTypeDao extends DaoBase {

  private final ContractDao contractDao;

  @Inject
  WorkingTimeTypeDao(JPQLQueryFactory queryFactory,
      Provider<EntityManager> emp, ContractDao contractDao) {
    super(queryFactory, emp);
    this.contractDao = contractDao;
  }

  /**
   * Se office è present il tipo orario di con quella descrizione se esiste. Se office non è present
   * il tipo orario di default con quella descrizione.
   */
  public WorkingTimeType workingTypeTypeByDescription(String description,
      Optional<Office> office) {

    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    final BooleanBuilder condition = new BooleanBuilder();

    if (office.isPresent()) {
      condition.and(wtt.description.eq(description).and(wtt.office.eq(office.get())));
    } else {
      condition.and(wtt.description.eq(description).and(wtt.office.isNull()));
    }

    return getQueryFactory().selectFrom(wtt).where(condition).fetchOne();

  }

  /**
   * Tutti gli orari.
   */
  public List<WorkingTimeType> getAllWorkingTimeType() {
    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    return getQueryFactory().selectFrom(wtt).fetch();
  }

  /**
   * Tutti gli orari di lavoro default e quelli speciali dell'office.
   */
  public List<WorkingTimeType> getEnabledWorkingTimeTypeForOffice(Office office) {

    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    return getQueryFactory()
        .selectFrom(wtt)
        .where(wtt.office.isNull().and(wtt.disabled.eq(false))
            .or(wtt.office.eq(office).and(wtt.disabled.eq(false)))).fetch();
  }

  /**
   * WorkingTimeType by id.
   *
   * @param id identificativo dell'orario di lavoro
   * @return l'orario di lavoro con id id.
   */
  public WorkingTimeType getWorkingTimeTypeById(Long id) {
    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    return getQueryFactory().selectFrom(wtt)
        .where(wtt.id.eq(id)).fetchOne();
  }


  /**
   * La lista degli orari di lavoro di default.
   *
   * @return la lista degli orari di lavoro presenti di default sul database.
   */
  public List<WorkingTimeType> getDefaultWorkingTimeType(Optional<Boolean> disabled) {
    final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
    val condition = new BooleanBuilder(wtt.office.isNull());
    if (disabled.isPresent()) {
      condition.and(wtt.disabled.eq(disabled.get()));
    }
    return getQueryFactory().selectFrom(wtt)
        .where(condition).orderBy(wtt.description.asc()).fetch();
  }


  /**
   * Il tipo orario per la persona attivo nel giorno.
   *
   * @param date data
   * @param person persona
   * @return il tipo orario se presente
   */
  public Optional<WorkingTimeType> getWorkingTimeType(LocalDate date, Person person) {

    Contract contract = contractDao.getContract(date, person);

    if (contract != null) {
      for (ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {

        if (DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate))) {
          return Optional.of(cwtt.workingTimeType);
        }
      }
    }
    return Optional.absent();
  }

  /**
   * Il tipo orario del giorno per la persona.
   *
   * @param date data
   * @param person persona
   * @return il tipo orario del giorno se presente
   */
  public Optional<WorkingTimeTypeDay> getWorkingTimeTypeDay(LocalDate date, Person person) {
    Optional<WorkingTimeType> wtt = getWorkingTimeType(date, person);
    if (!wtt.isPresent()) {
      return Optional.absent();
    }
    int index = date.getDayOfWeek() - 1;
    Verify.verify(index < wtt.get().workingTimeTypeDays.size(),
        String.format("Definiti %d giorni nel WorkingTimeType %s, "
                + "richiesto giorno non presente con indice %d",
            wtt.get().workingTimeTypeDays.size(), wtt.get(), index));

    Optional<WorkingTimeTypeDay> wttd =
        Optional.fromNullable(wtt.get().workingTimeTypeDays.get(index));

    Verify.verify(wttd.isPresent());
    Verify.verify(wttd.get().dayOfWeek == date.getDayOfWeek());

    return wttd;
  }

  /**
   * ContractWorkingTimeType by id.
   *
   * @param id identificativo dell'associazione tra contratto e tipologia di orario di lavoro
   * @return associazione tra contratto e tipologia di orario di lavoro con l'id passato.
   */
  public ContractWorkingTimeType getContractWorkingTimeType(Long id) {
    final QContractWorkingTimeType cwtt = QContractWorkingTimeType.contractWorkingTimeType;
    return getQueryFactory().selectFrom(cwtt)
        .where(cwtt.id.eq(id)).fetchOne();
  }

}
