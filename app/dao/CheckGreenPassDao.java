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
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.CheckGreenPass;
import models.Office;
import models.Person;
import models.query.QCheckGreenPass;
import models.query.QPerson;
import org.joda.time.LocalDate;


/**
 * Dao per le query sul green pass.
 *
 * @author dario
 *
 */
public class CheckGreenPassDao extends DaoBase {

  
  @Inject
  CheckGreenPassDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }
  
  /**
   * Ritorna la lista dei sorteggiati per la data passata come parametro.
   *
   * @param date la data per cui cercare i check ai green pass
   * @return la lista dei sorteggiati per la data in oggetto.
   */
  public List<CheckGreenPass> listByDate(LocalDate date, Office office) {
    final QCheckGreenPass checkGreenPass = QCheckGreenPass.checkGreenPass;
    final QPerson person = QPerson.person;
    final JPQLQuery<CheckGreenPass> query = getQueryFactory()
        .selectFrom(checkGreenPass).leftJoin(checkGreenPass.person, person)
        .where(checkGreenPass.checkDate.eq(date)
            .and(person.office.eq(office)))
        .orderBy(person.surname.asc());
    
    return query.fetch();
  }
  
  /**
   * Ritorna, se esiste, il checkGreenPass identificato dall'id passato come parametro.
   *
   * @param checkGreenPassId l'identificativo del checkGreenPass
   * @return l'optional contenenente o meno l'oggetto identificato dall'id passato come parametro.
   */
  public CheckGreenPass getById(long checkGreenPassId) {
    final QCheckGreenPass checkGreenPass = QCheckGreenPass.checkGreenPass;
    final CheckGreenPass result = getQueryFactory().selectFrom(checkGreenPass)
        .where(checkGreenPass.id.eq(checkGreenPassId)).fetchFirst();
    return result;
  }
  
  /**
   * Verifica se esiste già una entry in tabella per la persona e la data passati.
   *
   * @param person la persona da controllare
   * @param date la data in cui controllare
   * @return se esiste il check green pass per i parametri passati.
   */
  public Optional<CheckGreenPass> byPersonAndDate(Person person, LocalDate date) {
    final QCheckGreenPass checkGreenPass = QCheckGreenPass.checkGreenPass;
    final CheckGreenPass result = getQueryFactory().selectFrom(checkGreenPass)
        .where(checkGreenPass.person.eq(person)
            .and(checkGreenPass.checkDate.eq(date))).fetchFirst();
    return Optional.fromNullable(result);
  }

  /**
   * Conta le volte in cui una persona è stata controllata.
   *
   * @param person la persona di cui controllare il numero di volte in cui è stata
   *     controllata
   * @return quante volte la persona passata come parametro è stata controllata.
   */
  public long howManyTimesChecked(Person person) {
    final QCheckGreenPass checkGreenPass = QCheckGreenPass.checkGreenPass;
    return getQueryFactory().selectFrom(checkGreenPass)
        .where(checkGreenPass.person.eq(person)).fetchCount();
  }
}
