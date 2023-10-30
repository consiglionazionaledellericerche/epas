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
import com.querydsl.core.group.GroupBy;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Competence;
import models.CompetenceCode;
import models.CompetenceCodeGroup;
import models.Office;
import models.Person;
import models.PersonHourForOvertime;
import models.PersonReperibilityType;
import models.TotalOvertime;
import models.query.QCompetence;
import models.query.QCompetenceCode;
import models.query.QPerson;
import models.query.QPersonCompetenceCodes;
import models.query.QPersonHourForOvertime;
import models.query.QPersonReperibility;
import models.query.QPersonReperibilityType;
import models.query.QTotalOvertime;
import org.joda.time.LocalDate;

/**
 * DAO per le Compentence.
 *
 */
public class CompetenceDao extends DaoBase {

  @Inject
  CompetenceDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory wrapperFactory) {
    super(queryFactory, emp);
  }

  /**
   * La competenza con id passato come parametro.
   *
   * @return la competenza relativa all'id passato come parametro.
   */
  public Competence getCompetenceById(Long id) {

    final QCompetence competence = QCompetence.competence;

    return getQueryFactory().selectFrom(competence)
        .where(competence.id.eq(id)).fetchOne();
  }

  /**
   * La lista dei CompetenceCode abilitati ad almeno una persona appartenente all'office.
   */
  public List<CompetenceCode> activeCompetenceCode(Office office, LocalDate date) {

    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;

    return getQueryFactory().selectFrom(competenceCode)
        .leftJoin(competenceCode.personCompetenceCodes, pcc).fetchJoin()
        .where(pcc.person.office.eq(office)
            .and(pcc.beginDate.loe(date)
                .andAnyOf(pcc.endDate.isNull(), pcc.endDate.goe(date))))
        .orderBy(competenceCode.code.asc())
        .distinct().fetch();
  }

  /**
   * La lista delle competenze per persona (opzionale), anno, mese (opzionale) e lista di codici.
   *
   * @return la lista di competenze appartenenti alla lista di codici codes relative all'anno year e
   *     al mese month per la persona person.
   */
  public List<Competence> getCompetences(
      Optional<Person> person, Integer year, Optional<Integer> month, List<CompetenceCode> codes) {

    final QCompetence competence = QCompetence.competence;
    final BooleanBuilder condition = new BooleanBuilder();
    condition.and(competence.year.eq(year)
        .and(competence.competenceCode.in(codes)));
    if (month.isPresent()) {
      condition.and(competence.month.eq(month.get()));
    }
    if (person.isPresent()) {
      condition.and(competence.person.eq(person.get()));
    }
    return getQueryFactory().selectFrom(competence)
        .leftJoin(competence.competenceCode).fetchJoin()
        .where(condition)
        .fetch();
  }

  /**
   * La competenza (se esiste) per persona, anno, mese, codice di competenza.
   *
   * @return la competenza se esiste relativa all'anno year e al mese month con codice code per la
   *     persona person.
   */
  public Optional<Competence> getCompetence(
      Person person, Integer year, Integer month, CompetenceCode code) {

    final QCompetence competence = QCompetence.competence;

    final Competence result = getQueryFactory().selectFrom(competence)
        .where(competence.person.eq(person)
            .and(competence.year.eq(year)
                .and(competence.month.eq(month)
                    .and(competence.competenceCode.eq(code))))).fetchOne();

    return Optional.fromNullable(result);

  }

  /**
   * La lista delle competence assegnate nell'office.
   *
   * <p>
   * Se untilThisMonth è true, viene presa la lista delle competenze dall'inizio dell'anno fino a
   * quel mese compreso, se è false solo quelle del mese specificato.
   * </p>
   *
   * @param codes filtra i codici di competenza.
   * @param office filtra per persone dell'office.
   */
  public List<Competence> getCompetencesInOffice(
      Integer year, Integer month, List<String> codes, Office office, boolean untilThisMonth) {

    final QCompetence competence = QCompetence.competence;
    final BooleanBuilder condition = new BooleanBuilder();

    condition.and(competence.year.eq(year))
        .and(competence.competenceCode.code.in(codes))
        .and(competence.person.office.eq(office));

    if (untilThisMonth) {
      condition.and(competence.month.loe(month));
    } else {
      condition.and(competence.month.eq(month));
    }

    return getQueryFactory().selectFrom(competence)
        .where(condition).fetch();
  }

  /**
   * Le competenze nell'anno year. Se office è present filtra sulle sole competenze assegnate alle
   * persone nell'office.
   */
  public List<Competence> getCompetenceInYear(Integer year, Optional<Office> office) {

    final QCompetence competence = QCompetence.competence;
    final BooleanBuilder condition = new BooleanBuilder();

    condition.and(competence.year.eq(year));

    if (office.isPresent()) {
      condition.and(competence.person.office.eq(office.get()));
    }

    return getQueryFactory().selectFrom(competence)
        .where(condition).orderBy(competence.competenceCode.code.asc())
        .fetch();
  }

  /**
   * La quantità di ore approvate di straordinario nell'anno, mese (opzionale), persona (opzionale)
   * e lista di codici di competenza.
   *
   * @return sulla base dei parametri passati alla funzione ritorna la quantità di ore approvate di
   *     straordinario (sommando i codici S1 S2 e S3).
   */
  public Optional<Integer> valueOvertimeApprovedByMonthAndYear(
      Integer year, Optional<Integer> month, Optional<Person> person,
      List<CompetenceCode> codeList) {

    final QCompetence competence = QCompetence.competence;
    final BooleanBuilder condition = new BooleanBuilder();

    if (month.isPresent()) {
      condition.and(competence.month.eq(month.get()));
    }
    if (person.isPresent()) {
      condition.and(competence.person.eq(person.get()));
    }
    final Integer result =
        getQueryFactory().select(competence.valueApproved.sum())
            .from(competence)
            .where(condition.and(competence.year.eq(year)
                .and(competence.competenceCode.in(codeList))))
            .fetchOne();

    return Optional.fromNullable(result);

  }

  /**
   * La lista di competenze per persona, anno, mese.
   *
   * @return la lista di tutte le competenze di una persona nel mese month e nell'anno year che
   *     abbiano un valore approvato > 0.
   */
  public List<Competence> getAllCompetenceForPerson(Person person, Integer year, Integer month) {
    return competenceInMonth(person, year, month, Optional.<List<String>>absent());
  }

  /**
   * La lista di competenze per persona, anno, mese, lista di codici (opzionale).
   *
   * @param person la persona per cui si cercano le competenze approvate
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @param codes (opzionale) la lista di codici
   * @return la lista di competenze approvate per persona, anno, mese, lista di codici
   */
  public List<Competence> competenceInMonth(
      Person person, Integer year, Integer month, Optional<List<String>> codes) {

    final QCompetence competence = QCompetence.competence;
    final BooleanBuilder condition = new BooleanBuilder();

    condition.and(competence.year.eq(year))
        .and(competence.person.eq(person))
        .and(competence.month.eq(month).and(competence.valueApproved.gt(0)));

    if (codes.isPresent()) {
      condition.and(competence.competenceCode.code.in(codes.get()));
    }

    return getQueryFactory().selectFrom(competence)
        .where(condition).fetch();
  }

  /**
   * Mappa con le competenze in un mese per le persone passate
   * come parametro.
   */
  public Map<Person, List<Competence>> competencesInMonth(
      List<Person> persons, int year, int month) {
    final QCompetence competence = QCompetence.competence;
    return getQueryFactory().selectFrom(competence)
        .where(competence.person.in(persons), 
            competence.year.eq(year), competence.month.eq(month),
            competence.valueApproved.gt(0))
        .transform(GroupBy.groupBy(competence.person).as(GroupBy.list(competence)));
  }
  
  /**
   * Ritorna una mappa contenente come chiave le persone e come valore la lista di competenze
   * che hanno avuto nell'anno.
   * 
   * @param persons la lista delle persone di cui si vogliono le competenze di tipo CompetenceCode
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @param code il codice di competenza su cui filtrare
   * @return la mappa contenente per ogni persona la lista di competenze di un certo tipo
   */
  public Map<Person, List<Competence>> competencesInYear(
      List<Person> persons, int year, CompetenceCode code) {
    final QCompetence competence = QCompetence.competence;
    return getQueryFactory().selectFrom(competence)
        .where(competence.person.in(persons), 
            competence.year.eq(year), competence.competenceCode.eq(code))
        .transform(GroupBy.groupBy(competence.person).as(GroupBy.list(competence)));
  }

  private List<Competence> competenceFromGroupInMonth(Person person, Integer year, 
      Integer month, CompetenceCodeGroup group) {
    final QCompetence competence = QCompetence.competence;
    final BooleanBuilder condition = new BooleanBuilder();
    
    condition.and(competence.year.eq(year))
        .and(competence.person.eq(person))
        .and(competence.month.eq(month))
        .and(competence.competenceCode.competenceCodeGroup.eq(group))
        .and(competence.valueApproved.gt(0));
    return getQueryFactory().selectFrom(competence).where(condition).fetch();
  }

  /**
   * metodo di utilità per il controller UploadSituation.
   *
   * @return la lista delle competenze del dipendente in questione per quel mese in quell'anno
   */
  public List<Competence> getCompetenceInMonthForUploadSituation(
      Person person, Integer year, Integer month, Optional<CompetenceCodeGroup> group) {
    List<Competence> competenceList = null;
    if (group.isPresent()) {
      competenceList = getAllCompetenceFromGroupForPerson(person, group, year, month);
    } else {
      competenceList = getAllCompetenceForPerson(person, year, month);
    }

    return competenceList;
  }

  /**
   * La lista di competenze per persona, gruppo di codici (opzionale), anno, mese.
   *
   * @param person la persona di cui si cerca la lista di competenze
   * @param group il gruppo di codici di competenza
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return la lista di competenze per persona, gruppo di codici (opzionale), anno, mese.
   */
  private List<Competence> getAllCompetenceFromGroupForPerson(Person person,
      Optional<CompetenceCodeGroup> group, Integer year, Integer month) {

    return competenceFromGroupInMonth(person, year, month, group.get());
  }

  /**
   * La lista di competenze relative alla reperibilità nell'anno, mese, e codice.
   *
   * @return la lista di competenze relative all'anno year, al mese month e al codice code di
   *     persone che hanno reperibilità di tipo type associata.
   */
  public List<Competence> getCompetenceInReperibility(
      PersonReperibilityType type, int year, int month, CompetenceCode code) {
    final QCompetence competence = QCompetence.competence;
    final QPerson person = QPerson.person;
    final QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
    final QPersonReperibility rep = QPersonReperibility.personReperibility;

    return getQueryFactory().selectFrom(competence)
        .leftJoin(competence.person, person)
        .leftJoin(person.reperibility, rep)
        .leftJoin(rep.personReperibilityType, prt)
        .where(prt.eq(type)
            .and(competence.year.eq(year)
                .and(competence.month.eq(month)
                    .and(competence.competenceCode.eq(code)))))
        .orderBy(competence.person.surname.asc()).fetch();
  }


  /**
   * L'ultima competenza assegnata di un certo tipo nell'anno, mese per una persona.
   *
   * @return l'ultima competenza assegnata din un certo typo in un determinato anno.
   */
  public Competence getLastPersonCompetenceInYear(
      Person person, int year, int month, CompetenceCode competenceCode) {
    final QCompetence com = QCompetence.competence;
    return getQueryFactory().selectFrom(com)
        .where(
            com.person.eq(person)
                .and(com.year.eq(year))
                .and(com.month.lt(month))
                .and(com.competenceCode.eq(competenceCode))
        )
        .orderBy(com.month.desc())
        .limit(1)
        .fetchOne();
  }

  /**
   * La lista di competenze per un certo codice di competenza.
   *
   * @param code il codice competenza da cercare
   * @return la lista di tutte le competenze che contengono quel codice competenza.
   */
  public List<Competence> findCompetence(CompetenceCode code) {
    final QCompetence comp = QCompetence.competence;
    return getQueryFactory().selectFrom(comp).where(comp.competenceCode.eq(code)).fetch();
  }


  /**
   * La lista di quantitativi di straordinario assegnati per la sede nell'anno.
   *
   * @return dei quantitativi di straordinario assegnati per l'ufficio office nell'anno year.
   */
  public List<TotalOvertime> getTotalOvertime(Integer year, Office office) {
    final QTotalOvertime totalOvertime = QTotalOvertime.totalOvertime;

    return getQueryFactory().selectFrom(totalOvertime)
        .where(totalOvertime.year.eq(year).and(totalOvertime.office.eq(office)))
        .fetch();
  }

  /**
   * La quantità di ore di straordinario per la persona passata come parametro.
   *
   * @return il personHourForOvertime relativo alla persona person passata come parametro.
   */
  public PersonHourForOvertime getPersonHourForOvertime(Person person) {

    final QPersonHourForOvertime personHourForOvertime =
        QPersonHourForOvertime.personHourForOvertime;

    return getQueryFactory().selectFrom(personHourForOvertime)
        .where(personHourForOvertime.person.eq(person))
        .fetchOne();
  }

}