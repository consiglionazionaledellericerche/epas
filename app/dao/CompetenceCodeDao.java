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
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.CompetenceCode;
import models.CompetenceCodeGroup;
import models.Office;
import models.Person;
import models.PersonCompetenceCodes;
import models.dto.PersonCompetenceCodeDto;
import models.enumerate.LimitType;
import models.query.QCompetenceCode;
import models.query.QCompetenceCodeGroup;
import models.query.QPersonCompetenceCodes;
import models.query.QPersonsOffices;
import org.joda.time.LocalDate;

/**
 * Dao per l'accesso alle informazioni dei CompetenceCode.
 *
 * @author Dario Tagliaferri
 */
public class CompetenceCodeDao extends DaoBase {

  @Inject
  CompetenceCodeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Ritorna il competenceCode relativo al codice passato come parametro.
   *
   * @return il competenceCode relativo al codice passato come parametro.
   */
  public CompetenceCode getCompetenceCodeByCode(String code) {
    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

    return getQueryFactory().selectFrom(competenceCode)
        .where(competenceCode.code.eq(code)).fetchOne();

  }

  /**
   * Il codice di competenza associato alla descrizione in parametro.
   *
   * @return il codice competenza relativo alla descrizione passata come parametro.
   */
  public CompetenceCode getCompetenceCodeByDescription(String description) {

    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

    return getQueryFactory().selectFrom(competenceCode)
        .where(competenceCode.description.eq(description)).fetchOne();
  }

  /**
   * Il competenceCode relativo all'id passato come parametro.
   *
   * @return il codice di competenza relativo all'id passato come parametro.
   */
  public CompetenceCode getCompetenceCodeById(Long id) {

    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

    return getQueryFactory().selectFrom(competenceCode)
        .where(competenceCode.id.eq(id)).fetchOne();
  }

  /**
   * La lista di competenceCode che hanno il LimitType passato come parametro.
   *
   * @param limitType il tipo di limite di utilizzo del codice di competenza
   * @return la lista dei codici di competenza con limit type uguale a quello passato come
   *     parametro.
   */
  public List<CompetenceCode> getCompetenceCodeByLimitType(LimitType limitType) {

    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

    return getQueryFactory().selectFrom(competenceCode)
        .where(competenceCode.limitType.eq(limitType)).orderBy(competenceCode.code.asc()).fetch();
  }

  /**
   * La lista di tutti i competenceCode.
   *
   * @return la lista di tutti i codici di competenza presenti nel database.
   */
  public List<CompetenceCode> getAllCompetenceCode() {

    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

    return getQueryFactory().selectFrom(competenceCode)
        .where(competenceCode.disabled.eq(false)).fetch();
  }

  /**
   * La lista di tutti i gruppi di competenceCode.
   *
   * @return la lista dei gruppi di competenze presenti nel db.
   */
  public List<CompetenceCodeGroup> getAllGroups() {
    final QCompetenceCodeGroup group = QCompetenceCodeGroup.competenceCodeGroup;
    return getQueryFactory().selectFrom(group).orderBy(group.id.asc()).fetch();
  }

  /**
   * Il gruppo di competenceCode relativo all'id passato come parametro.
   *
   * @param competenceCodeGroupId l'id del gruppo
   * @return il gruppo di codici competenza caratterizzato dall'id passato come parametro.
   */
  public CompetenceCodeGroup getGroupById(Long competenceCodeGroupId) {
    final QCompetenceCodeGroup group = QCompetenceCodeGroup.competenceCodeGroup;
    return getQueryFactory().selectFrom(group).where(group.id.eq(competenceCodeGroupId)).fetchOne();
  }

  /**
   * La lista dei competenceCode che non appartengono ad alcun gruppo.
   *
   * @return la lista di tutti i codici di competenza che non appartengono ad alcun gruppo.
   */
  public List<CompetenceCode> getCodeWithoutGroup() {
    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
    return getQueryFactory().selectFrom(competenceCode)
        .where(competenceCode.competenceCodeGroup.isNull())
        .orderBy(competenceCode.disabled.asc(),
            competenceCode.code.asc()).fetch();
  }

  /**
   * La lista dei competenceCode che appartengono al gruppo group.
   *
   * @param group il gruppo di codici di competenza
   * @param except opzionale: se presente serve per tralasciare quel competenceCode nella ricerca
   *     dei codici appartenenti al gruppo group
   * @return la lista dei codici di competenza che appartengono al gruppo passato come parametro.
   */
  public List<CompetenceCode> getCodeWithGroup(CompetenceCodeGroup group,
      Optional<CompetenceCode> except) {
    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
    BooleanBuilder condition = new BooleanBuilder();
    if (except.isPresent()) {
      condition.and(competenceCode.ne(except.get()));
    }
    return getQueryFactory().selectFrom(competenceCode)
        .where(competenceCode.competenceCodeGroup.eq(group).and(condition))
        .orderBy(competenceCode.code.asc()).fetch();
  }

  /**
   * La lista dei competenceCode che sono senza gruppo e che appartengono al gruppo group.
   *
   * @param group il gruppo che si vuole inserire nella ricerca dei codici di competenza
   * @return la lista dei codici di competenza senza gruppo più quelli che appartengono al gruppo
   *     passato come parametro.
   */
  public List<CompetenceCode> allCodesContainingGroupCodes(CompetenceCodeGroup group) {
    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
    return getQueryFactory().selectFrom(competenceCode)
        .where(competenceCode.competenceCodeGroup.isNull()
            .or(competenceCode.competenceCodeGroup.eq(group)))
        .fetch();
  }


  /**
   * La lista dei personCompetenceCode per persona ad una certa data.
   *
   * @param person la person
   * @return la lista di PersonCompetenceCodes associata alla persona passata come parametro.
   */
  public List<PersonCompetenceCodes> listByPerson(Person person, Optional<LocalDate> date) {
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
    final BooleanBuilder condition = new BooleanBuilder();
    if (date.isPresent()) {
      condition.and(pcc.beginDate.loe(date.get().dayOfMonth().withMaximumValue())
          .andAnyOf(pcc.endDate.isNull(),
              pcc.endDate.goe(date.get().dayOfMonth().withMaximumValue())));

    }
    return getQueryFactory().selectFrom(pcc).where(pcc.person.eq(person).and(condition)).fetch();
  }

  /**
   * Il personCompetenceCode, se esiste, per persona, codice di competenza a una certa data.
   *
   * @param person la persona
   * @param code il codice di competenza
   * @return il PersonCompetenceCode relativo ai parametri passati, se presente.
   */
  public Optional<PersonCompetenceCodes> getByPersonAndCodeAndDate(Person person,
      CompetenceCode code, LocalDate date) {
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;

    final PersonCompetenceCodes result = getQueryFactory().selectFrom(pcc)
        .where(pcc.person.eq(person)
            .and(pcc.competenceCode.eq(code))
            .and(pcc.beginDate.loe(date).andAnyOf(pcc.endDate.goe(date), pcc.endDate.isNull())))
        .orderBy(pcc.beginDate.asc())
        .fetchFirst();

    return Optional.fromNullable(result);
  }

  /**
   * La lista dei personCompetenceCode per persona e per codice.
   *
   * @param person la persona di cui si vuole il PersonCompetenceCodes
   * @param code il codice di competenza per quel PersonCompetenceCodes
   * @return la lista di PersonCompetenceCodes relativa ai parametri passati al metodo.
   */
  public List<PersonCompetenceCodes> listByPersonAndCode(Person person, CompetenceCode code) {
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;

    return getQueryFactory().selectFrom(pcc)
        .where(pcc.person.eq(person).and(pcc.competenceCode.eq(code)))
        .orderBy(pcc.beginDate.desc())
        .fetch();
  }

  /**
   * Il personCompetenceCode, se esiste, per persona, per codice ad una certa data.
   *
   * @param person la persona di cui cercare la competenza
   * @param code il codice di competenza
   * @param date la data da cui cercare
   * @return se esiste, il personcompetencecode temporalmente più vicino nel futuro alla data
   *     passata come parametro per la persona passata come parametro.
   */
  public Optional<PersonCompetenceCodes> getNearFuture(Person person,
      CompetenceCode code, LocalDate date) {
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
    final PersonCompetenceCodes result = getQueryFactory().selectFrom(pcc)
        .where(pcc.person.eq(person).and(pcc.competenceCode.eq(code).and(pcc.beginDate.gt(date))))
        .orderBy(pcc.beginDate.asc()).fetchFirst();
    return Optional.fromNullable(result);
  }

  /**
   * La lista dei personCompetenceCode per sede di un certo codice ad una certa data.
   *
   * @param code il codice di competenza
   * @param date la data, se presente, in cui cercare
   * @param office la sede
   * @return la lista dei personCompetenceCodes che rispondono ai parametri passati.
   */
  public List<PersonCompetenceCodes> listByCompetenceCode(CompetenceCode code,
      Optional<LocalDate> date, Office office) {
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    final BooleanBuilder condition = new BooleanBuilder();
    if (date.isPresent()) {
      condition.and(pcc.beginDate.loe(date.get().dayOfMonth().withMaximumValue())
          .andAnyOf(pcc.endDate.isNull(),
              pcc.endDate.goe(date.get().dayOfMonth().withMaximumValue())));
    }
    return getQueryFactory().selectFrom(pcc)
        .leftJoin(pcc.person.personsOffices, personsOffices)
        .where(pcc.competenceCode.eq(code)
            .and(personsOffices.office.eq(office)).and(condition))
        .fetch();
  }

  /**
   * La lista dei personCompetenceCode per codice di assenza ad una certa data.
   *
   * @param codesList la lista dei codici di assenza da ricercare
   * @param date la data (opzionale) che deve essere contenuta nel periodo di possesso di una certa
   *     competenza
   * @return la lista delle persone con abilitate le competenze passate nella lista come parametro.
   */
  public List<PersonCompetenceCodes> listByCodes(List<CompetenceCode> codesList,
      Optional<LocalDate> date) {
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
    final BooleanBuilder condition = new BooleanBuilder();
    if (date.isPresent()) {
      condition.and(pcc.beginDate.loe(date.get().dayOfMonth().withMaximumValue())
          .andAnyOf(pcc.endDate.isNull(),
              pcc.endDate.goe(date.get().dayOfMonth().withMaximumValue())));
    }
    return getQueryFactory().selectFrom(pcc)
        .where(pcc.competenceCode.in(codesList).and(condition))
        .fetch();
  }

  /**
   * La lista dei personCompetenceCode per lista di codici, nell'ufficio ad una certa data.
   *
   * @param codesList la lista dei codici di assenza da ricercare
   * @param date la data (opzionale) che deve essere contenuta nel periodo di possesso di una certa
   *     competenza
   * @return la lista delle persone con abilitate le competenze passate nella lista come parametro.
   */
  public List<PersonCompetenceCodes> listByCodesAndOffice(List<CompetenceCode> codesList,
      Office office, Optional<LocalDate> date) {
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    final BooleanBuilder condition = new BooleanBuilder();
    if (date.isPresent()) {
      condition.and(pcc.beginDate.loe(date.get().dayOfMonth().withMaximumValue())
          .andAnyOf(pcc.endDate.isNull(),
              pcc.endDate.goe(date.get().dayOfMonth().withMaximumValue())));
    }
    return getQueryFactory().selectFrom(pcc)
        .leftJoin(pcc.person.personsOffices, personsOffices)
        .where(pcc.competenceCode.in(codesList).and(personsOffices.office.eq(office)).and(condition))
        .fetch();
  }
  
  /**
   * Ritorna la lista dei person competence codes duplicati.
   *
   * @return la lista di pcc duplicati.
   */
  public List<PersonCompetenceCodeDto> getDuplicates() {
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;

    return getQueryFactory().from(pcc)
        .select(Projections.constructor(PersonCompetenceCodeDto.class, 
            pcc.person.id, pcc.competenceCode.id))
        .where(pcc.beginDate.lt(LocalDate.now())
        .andAnyOf(pcc.endDate.isNull(), pcc.endDate.gt(LocalDate.now())))
        .groupBy(pcc.person.id, pcc.competenceCode.id).having(pcc.count().gt(1L))
        .fetch();
  }
  
  /**
   * Ritorna la lista dei person competence codes con date sballate.
   *
   * @return la lista dei pcc con date sballate.
   */
  public List<PersonCompetenceCodes> getWrongs() {
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
    
    return getQueryFactory().selectFrom(pcc).where(pcc.beginDate.goe(pcc.endDate)).fetch();
  }

}