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
package dao.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.DaoBase;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.CategoryTab;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.InitializationGroup;
import models.absences.JustifiedBehaviour;
import models.absences.JustifiedBehaviour.JustifiedBehaviourName;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.query.QAbsence;
import models.absences.query.QAbsenceTrouble;
import models.absences.query.QAbsenceType;
import models.absences.query.QCategoryGroupAbsenceType;
import models.absences.query.QCategoryTab;
import models.absences.query.QComplationAbsenceBehaviour;
import models.absences.query.QGroupAbsenceType;
import models.absences.query.QInitializationGroup;
import models.absences.query.QJustifiedBehaviour;
import models.absences.query.QJustifiedType;
import models.absences.query.QTakableAbsenceBehaviour;
import models.enumerate.MealTicketBehaviour;
import models.query.QContract;
import models.query.QPerson;
import models.query.QPersonDay;
import org.joda.time.LocalDate;
import play.db.jpa.JPA;

/**
 * Dao per il componente assenze.
 *
 * @author Alessandro Martelli
 */
@Slf4j
public class AbsenceComponentDao extends DaoBase {

  @Inject
  AbsenceComponentDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }


  /**
   * AbsenceType per id.
   *
   * @return l'absenceType relativo all'id passato come parametro.
   */
  public Optional<AbsenceType> absenceTypeById(Long id) {

    QAbsenceType absenceType = QAbsenceType.absenceType;
    return Optional.fromNullable((AbsenceType) getQueryFactory().from(absenceType)
        .where(absenceType.id.eq(id)).fetchOne());
  }

  /**
   * AbsenceType per campo code.
   *
   * @return l'absenceType relativo al codice passato come parametro.
   */
  public Optional<AbsenceType> absenceTypeByCode(String string) {

    QAbsenceType absenceType = QAbsenceType.absenceType;
    final JPQLQuery<?> query = getQueryFactory()
        .from(absenceType)
        .where(absenceType.code.eq(string).or(absenceType.code.equalsIgnoreCase(string)));
    return Optional.fromNullable((AbsenceType) query.fetchOne());
  }

  /**
   * AbsenceType per campo certification.
   *
   * @return l'absenceType relativo al codice passato come parametro nel campo certification.
   */
  public List<AbsenceType> absenceTypesByCertificationCode(String string) {

    QAbsenceType absenceType = QAbsenceType.absenceType;
    return getQueryFactory()
        .selectFrom(absenceType)
        .where(absenceType.certificateCode.eq(string)
            .or(absenceType.certificateCode.equalsIgnoreCase(string))).fetch();
  }

  /**
   * Gli absenceTypes con quegli id. Se non vengono caricati tutti gli id ritorna null.
   *
   * @param ids gli id
   * @return list o null
   */
  public List<AbsenceType> absenceTypesByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Lists.newArrayList();
    }
    QAbsenceType absenceType = QAbsenceType.absenceType;
    List<AbsenceType> types = getQueryFactory().selectFrom(absenceType)
        .where(absenceType.id.in(ids)).fetch();
    if (types.size() != ids.size()) {
      return null;
    }
    return types;
  }

  /**
   * Ritorna il JustifiedType con quel nome. Se non esiste lo crea.
   *
   * @param name nome
   * @return entity
   */
  public JustifiedType getOrBuildJustifiedType(JustifiedTypeName name) {

    QJustifiedType justifiedType = QJustifiedType.justifiedType;
    JustifiedType obj = (JustifiedType) getQueryFactory().from(justifiedType)
        .where(justifiedType.name.eq(name))
        .fetchOne();
    if (obj == null) {
      obj = new JustifiedType();
      obj.setName(name);
      obj.save();
    }
    return obj;
  }

  /**
   * Ritorna il JustifiedBehaviour con quel nome. Se non esiste lo crea.
   *
   * @param name nome
   * @return entity
   */
  public JustifiedBehaviour getOrBuildJustifiedBehaviour(JustifiedBehaviourName name) {
    QJustifiedBehaviour justifiedBehaviour = QJustifiedBehaviour.justifiedBehaviour;
    JustifiedBehaviour obj = (JustifiedBehaviour) getQueryFactory()
        .from(justifiedBehaviour).where(justifiedBehaviour.name.eq(name))
        .fetchOne();
    if (obj == null) {
      obj = new JustifiedBehaviour();
      obj.setName(name);
      obj.save();
    }
    return obj;
  }

  /**
   * Ritorna la categoria con quel nome. Se non esiste la crea.
   *
   * @param name name
   * @param priority priorità
   * @return la categoria
   */
  public CategoryGroupAbsenceType getOrBuildCategoryType(String name, int priority) {

    QCategoryGroupAbsenceType categoryType = QCategoryGroupAbsenceType.categoryGroupAbsenceType;
    CategoryGroupAbsenceType obj = (CategoryGroupAbsenceType) getQueryFactory().from(categoryType)
        .where(categoryType.name.eq(name)).fetchOne();

    if (obj == null) {
      obj = new CategoryGroupAbsenceType();
      obj.setName(name);
      obj.setPriority(priority);
      obj.save();
    }
    if (obj.getPriority() != priority) {
      obj.setPriority(priority);
      obj.save();
    }
    return obj;
  }

  /**
   * Le categorie con quei nomi.
   *
   * @param names nomi
   * @return list
   */
  public List<CategoryGroupAbsenceType> categoryByNames(List<String> names) {
    QCategoryGroupAbsenceType category = QCategoryGroupAbsenceType.categoryGroupAbsenceType;
    return getQueryFactory().selectFrom(category)
        .where(category.name.in(names)).fetch();
  }

  /**
   * La categoria con quel nome.
   *
   * @param name nome
   * @return entity
   */
  public Optional<CategoryGroupAbsenceType> categoryByName(String name) {
    QCategoryGroupAbsenceType category = QCategoryGroupAbsenceType.categoryGroupAbsenceType;
    return Optional.fromNullable((CategoryGroupAbsenceType) getQueryFactory().selectFrom(category)
        .where(category.name.eq(name)).fetchOne());
  }

  /**
   * Le categorie ordinate per priorità.
   */
  public List<CategoryGroupAbsenceType> categoriesByPriority() {
    QCategoryGroupAbsenceType category = QCategoryGroupAbsenceType.categoryGroupAbsenceType;
    return getQueryFactory().selectFrom(category).orderBy(category.priority.asc()).fetch();
  }

  /**
   * Le tab ordinate per priorità.
   */
  public List<CategoryTab> tabsByPriority() {
    QCategoryTab categoryTab = QCategoryTab.categoryTab;
    return getQueryFactory().selectFrom(categoryTab).orderBy(categoryTab.priority.asc())
        .fetch();
  }

  /**
   * La tab con quel nome.
   *
   * @param name nome
   * @return entity
   */
  public Optional<CategoryTab> tabByName(String name) {
    QCategoryTab categoryTab = QCategoryTab.categoryTab;
    return Optional.fromNullable((CategoryTab) getQueryFactory().from(categoryTab)
        .where(categoryTab.name.eq(name)).fetchOne());
  }

  /**
   * Il comportamento completamento con quel nome.
   *
   * @param name nome
   * @return entity
   */
  public Optional<ComplationAbsenceBehaviour> complationAbsenceBehaviourByName(String name) {

    QComplationAbsenceBehaviour complationAbsenceBehaviour =
        QComplationAbsenceBehaviour.complationAbsenceBehaviour;

    return Optional.fromNullable(
        (ComplationAbsenceBehaviour) getQueryFactory().from(complationAbsenceBehaviour)
        .where(complationAbsenceBehaviour.name.eq(name)).fetchOne());
  }

  /**
   * Il comportamento limiti con quel nome.
   *
   * @param name nome
   * @return entity
   */
  public Optional<TakableAbsenceBehaviour> takableAbsenceBehaviourByName(String name) {

    QTakableAbsenceBehaviour takableAbsenceBehaviour =
        QTakableAbsenceBehaviour.takableAbsenceBehaviour;

    return Optional
        .fromNullable((TakableAbsenceBehaviour) getQueryFactory().from(takableAbsenceBehaviour)
            .where(takableAbsenceBehaviour.name.eq(name)).fetchOne());
  }


  /**
   * Il gruppo con quel nome.
   *
   * @param name nome
   * @return entity
   */
  public Optional<GroupAbsenceType> groupAbsenceTypeByName(String name) {

    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;

    return Optional.fromNullable((GroupAbsenceType) getQueryFactory().from(groupAbsenceType)
        .where(groupAbsenceType.name.eq(name)).fetchOne());
  }

  /**
   * I gruppi con quei nomi.
   *
   * @param names nomi
   * @return entity list
   */
  public List<GroupAbsenceType> groupsAbsenceTypeByName(List<String> names) {

    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;

    return getQueryFactory().selectFrom(groupAbsenceType)
        .where(groupAbsenceType.name.in(names)).fetch();
  }

  /**
   * GroupAbsenceType by id.
   *
   * @param id id
   * @return entity
   */
  public GroupAbsenceType groupAbsenceTypeById(Long id) {

    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;

    return getQueryFactory().selectFrom(groupAbsenceType)
        .where(groupAbsenceType.id.eq(id)).fetchOne();
  }

  /**
   * Tutti i gruppi.
   *
   * @return entity list
   */
  public List<GroupAbsenceType> allGroupAbsenceType(boolean alsoAutomatic) {

    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;

    BooleanBuilder conditions = new BooleanBuilder();
    if (!alsoAutomatic) {
      conditions.and(groupAbsenceType.automatic.eq(false));
    }
    return getQueryFactory().selectFrom(groupAbsenceType)
        .leftJoin(groupAbsenceType.category).fetchJoin()
        .leftJoin(groupAbsenceType.previousGroupChecked).fetchJoin()
        .where(conditions)
        .fetch();
  }

  /**
   * Il primo gruppo della catena cui appartiene groupAbsenceType.
   *
   * @param groupAbsenceType gruppo indagato
   * @return entity
   */
  public GroupAbsenceType firstGroupOfChain(GroupAbsenceType groupAbsenceType) {
    GroupAbsenceType group = groupAbsenceType;
    List<GroupAbsenceType> all = allGroupAbsenceType(true);
    boolean changed = true;
    while (changed) {
      changed = false;
      for (GroupAbsenceType previous : all) {
        if (Objects.equals(previous.getNextGroupToCheck(), group)) {
          group = previous;
          changed = true;
        }
      }
    }
    return group;
  }

  /**
   * I gruppi con quel pattern.
   *
   * @param pattern pattern
   * @return entity list
   */
  public List<GroupAbsenceType> groupAbsenceTypeOfPattern(GroupAbsenceTypePattern pattern) {
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;

    return getQueryFactory().selectFrom(groupAbsenceType)
        .leftJoin(groupAbsenceType.category).fetchJoin()
        .leftJoin(groupAbsenceType.previousGroupChecked).fetchJoin()
        .where(groupAbsenceType.pattern.eq(pattern))
        .fetch();
  }

  /**
   * Crea l'assenza con queste caratteristiche. Se esiste già una entity con quel codice la
   * aggiorna.
   *
   * @param code codice
   * @param description descrizione
   * @param minutes minuti giustificati
   * @param justifiedTypePermitted tipi giustificativi permessi
   * @param complationType tipo del completamento
   * @param complationTime tempo di completamento
   * @param internalUse se uso interno
   * @param consideredWeekEnd se considerare week end
   * @param timeForMealticket se il tempo contribuisce al buono pasto
   * @param certificateCode codice per attestati
   * @param expire data scadenza
   * @return entity creata o modificata
   */
  public AbsenceType buildOrEditAbsenceType(String code, String description, int minutes,
      Set<JustifiedType> justifiedTypePermitted, JustifiedType complationType, int complationTime,
      boolean internalUse, boolean consideredWeekEnd,
      MealTicketBehaviour mealTicketBehaviour, String certificateCode, LocalDate expire) {

    QAbsenceType absenceType = QAbsenceType.absenceType;
    AbsenceType obj = (AbsenceType) getQueryFactory()
        .from(absenceType)
        .where(absenceType.code.equalsIgnoreCase(code)).fetchOne();
    if (obj == null) {
      obj = new AbsenceType();
    }
    obj.setCode(code);
    obj.setDescription(description);
    obj.setJustifiedTime(minutes);
    obj.getJustifiedTypesPermitted().clear();
    //obj.justifiedTimeAtWork = null;
    obj.save();
    JPA.em().flush();
    for (JustifiedType justified : justifiedTypePermitted) {
      obj.getJustifiedTypesPermitted().add(justified);
    }
    obj.setReplacingType(complationType);
    obj.setReplacingTime(complationTime);
    obj.setInternalUse(internalUse);
    obj.setMealTicketBehaviour(mealTicketBehaviour);
    obj.setConsideredWeekEnd(consideredWeekEnd);
    obj.setCertificateCode(code);
    if (expire != null) {
      obj.setValidTo(expire);
    } else {
      obj.setValidTo(new LocalDate(2099, 12, 31));
    }

    obj.save();
    return obj;

  }

  /**
   * Rinomina l'assenza code con il nuovo codice newCode. Se newCode era già presente appende al suo
   * codice il postfisso ex.
   *
   * @param code codice
   * @param newCode nuovo codice
   */
  public void renameCode(String code, String newCode) {
    QAbsenceType absenceType = QAbsenceType.absenceType;

    AbsenceType obj = (AbsenceType) getQueryFactory()
        .from(absenceType)
        .where(absenceType.code.equalsIgnoreCase(code)).fetchOne();
    if (obj == null) {
      return;
    }

    AbsenceType exObj = (AbsenceType) getQueryFactory()
        .from(absenceType)
        .where(absenceType.code.equalsIgnoreCase(newCode)).fetchOne();
    if (exObj != null) {
      exObj.setCode(code + "ex");
      exObj.save();
      JPA.em().flush();
    }

    obj.setCode(newCode);
    obj.save();
  }

  /**
   * Le assenze effettuate dalla persona nel periodo specificato e con i codici riportati.
   * Ordinamento per data in ordine crescente.
   *
   * @param person person
   * @param begin data inizio
   * @param end data fine
   * @param codeSet set dei codici
   * @return entity list
   */
  public List<Absence> orderedAbsences(Person person, LocalDate begin, LocalDate end,
      Set<AbsenceType> codeSet) {

    final QAbsence absence = QAbsence.absence;
    final QAbsenceType absenceType = QAbsenceType.absenceType;

    final long start = System.currentTimeMillis();
    log.trace("Inizio metodo orderedAbsences, person={}, begin={}, end={}, codeSet={}",
        person.getFullname(), begin, end, codeSet);
    
    BooleanBuilder conditions = new BooleanBuilder();
    if (begin != null) {
      conditions.and(absence.personDay.date.goe(begin));
    }
    if (end != null) {
      conditions.and(absence.personDay.date.loe(end));
    }
    if (!codeSet.isEmpty()) {
      conditions.and(absence.absenceType.in(codeSet));
    }
    List<Absence> absences =  getQueryFactory().selectFrom(absence)
        .leftJoin(absence.justifiedType).fetchJoin()
        .leftJoin(absence.absenceType, absenceType).fetchJoin()
        .leftJoin(absenceType.complationGroup).fetchJoin()
        .leftJoin(absenceType.replacingGroup).fetchJoin()
        .leftJoin(absence.personDay).fetchJoin()
        .where(absence.personDay.person.eq(person)
            .and(conditions)).distinct().fetch();
    //comparatore per ovviare al problema della orderby che non funziona in questo 
    //caso con il querydsl nuovo
    Comparator<Absence> absenceComparator = 
        Comparator.comparing(
            Absence::getPersonDay, (s1, s2) -> {
              return s2.getDate().compareTo(s1.getDate());
            });
    val result = absences.stream().sorted(absenceComparator).collect(Collectors.toList());
    log.trace("Terminato metodo orderedAbsences in {} millisecondi", 
        System.currentTimeMillis() - start);
    return result;
  }

  /**
   * Inizializzazioni per quella persona.
   *
   * @param person persona
   * @return list
   */
  public List<InitializationGroup> personInitializationGroups(Person person) {
    QInitializationGroup initializationGroup = QInitializationGroup.initializationGroup;
    return getQueryFactory()
        .selectFrom(initializationGroup)
        .where(initializationGroup.person.eq(person)).fetch();
  }

  /**
   * Le categorie che contengono gruppi inizializzabili.
   *
   * @return list
   */
  public List<CategoryGroupAbsenceType> initializablesCategory() {
    SortedMap<Integer, CategoryGroupAbsenceType> categories = Maps.newTreeMap();
    List<GroupAbsenceType> allGroups = GroupAbsenceType.findAll();
    for (GroupAbsenceType group : allGroups) {
      if (group.isInitializable()) {
        categories.put(group.getCategory().getPriority(), group.getCategory());
      }
    }
    return Lists.newArrayList(categories.values());
  }

  /**
   * Il primo gruppo inizializzabile per quella categoria.
   *
   * @param category category
   * @return gruppo
   */
  public GroupAbsenceType firstGroupInitializable(CategoryGroupAbsenceType category) {
    for (GroupAbsenceType group : category.getGroupAbsenceTypes()) {
      if (group.isInitializable()) {
        return group;
      }
    }
    return null;
  }

  /**
   * I tipi giustificativi con quei nomi.
   *
   * @param justifiedTypeName nomi
   * @return list
   */
  public List<JustifiedType> justifiedTypes(List<JustifiedTypeName> justifiedTypeName) {
    List<JustifiedType> types = Lists.newArrayList();
    for (JustifiedTypeName name : justifiedTypeName) {
      types.add(getOrBuildJustifiedType(name));
    }
    return types;
  }

  /**
   * Gli absenceTroubles delle persone passate.
   *
   * @param people le persone
   * @return list
   */

  public Map<Person, List<Absence>> absenceTroubles(List<Person> people) {

    QAbsence absence = QAbsence.absence;
    QContract contract = QContract.contract;

    final JPQLQuery<Absence> query = getQueryFactory()
        .selectFrom(absence)
        .leftJoin(absence.troubles, QAbsenceTrouble.absenceTrouble)
        .leftJoin(absence.personDay, QPersonDay.personDay)
        .leftJoin(absence.personDay.person, QPerson.person)
        .leftJoin(absence.personDay.person.contracts, contract)        
        .where(absence.troubles.isNotEmpty().and(absence.personDay.person.in(people)
            .andAnyOf((contract.sourceDateResidual.isNotNull()
                .and(absence.personDay.date.goe(contract.sourceDateResidual))),
                (contract.sourceDateResidual.isNull()
                    .and(absence.personDay.date.goe(contract.beginDate))))));
    List<Absence> absences = query.fetch();
    Map<Person, List<Absence>> map = Maps.newHashMap();
    for (Absence trouble : absences) {
      List<Absence> personAbsences = map.get(trouble.getPersonDay().getPerson());
      if (personAbsences == null) {
        personAbsences = Lists.newArrayList();
        map.put(trouble.getPersonDay().getPerson(), personAbsences);
      }
      personAbsences.add(trouble);
    }
    return map;
  }


  /**
   * Le assenze con quel codice in quel giorno per la persona.
   *
   * @param person persona
   * @param date data
   * @param code codice
   * @return list
   */
  public List<Absence> findAbsences(Person person, LocalDate date, String code) {
    QAbsence absence = QAbsence.absence;
    return getQueryFactory()
        .selectFrom(absence)
        .leftJoin(absence.personDay, QPersonDay.personDay)
        .leftJoin(absence.personDay.person, QPerson.person)
        .where(absence.personDay.person.eq(person)
            .and(absence.personDay.date.eq(date)
                .and(absence.absenceType.code.eq(code))))
        .fetch();
  }

  /**
   * Le assenze con quei codici.
   *
   * @param codes codici
   * @return la lista di assenze con quei codici.
   */
  public List<Absence> absences(List<String> codes) {
    QAbsence absence = QAbsence.absence;
    return getQueryFactory()
        .selectFrom(absence)
        .leftJoin(absence.personDay, QPersonDay.personDay)
        .where(absence.absenceType.code.in(codes))
        .fetch();
  }

}