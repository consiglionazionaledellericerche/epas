package dao.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.DaoBase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import javax.persistence.EntityManager;
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
import models.absences.query.QAbsenceTypeJustifiedBehaviour;
import models.absences.query.QCategoryGroupAbsenceType;
import models.absences.query.QCategoryTab;
import models.absences.query.QComplationAbsenceBehaviour;
import models.absences.query.QGroupAbsenceType;
import models.absences.query.QInitializationGroup;
import models.absences.query.QJustifiedBehaviour;
import models.absences.query.QJustifiedType;
import models.absences.query.QTakableAbsenceBehaviour;
import models.query.QPerson;
import models.query.QPersonDay;
import org.joda.time.LocalDate;
import play.db.jpa.JPA;

/**
 * Dao per il componente assenze.
 *
 * @author alessandro
 */
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
    final JPQLQuery<?> query = getQueryFactory()
        .from(absenceType)
        .where(absenceType.certificateCode.eq(string)
            .or(absenceType.certificateCode.equalsIgnoreCase(string)));
    return (List<AbsenceType>) query.fetch();
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
    List<AbsenceType> types = (List<AbsenceType>) getQueryFactory().from(absenceType)
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
      obj.name = name;
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
      obj.name = name;
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
      obj.name = name;
      obj.priority = priority;
      obj.save();
    }
    if (obj.priority != priority) {
      obj.priority = priority;
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
    return (List<CategoryGroupAbsenceType>) getQueryFactory().from(category)
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
    return Optional.fromNullable((CategoryGroupAbsenceType) getQueryFactory().from(category)
        .where(category.name.eq(name)).fetchOne());
  }

  /**
   * Le categorie ordinate per priorità.
   */
  public List<CategoryGroupAbsenceType> categoriesByPriority() {
    QCategoryGroupAbsenceType category = QCategoryGroupAbsenceType.categoryGroupAbsenceType;
    return (List<CategoryGroupAbsenceType>) getQueryFactory().from(category)
        .orderBy(category.priority.asc()).fetch();
  }

  /**
   * Le tab ordinate per priorità.
   */
  public List<CategoryTab> tabsByPriority() {
    QCategoryTab categoryTab = QCategoryTab.categoryTab;
    return (List<CategoryTab>) getQueryFactory().from(categoryTab).orderBy(categoryTab.priority.asc())
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

    return (List<GroupAbsenceType>) getQueryFactory().from(groupAbsenceType)
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

    return (GroupAbsenceType) getQueryFactory().from(groupAbsenceType)
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
    return (List<GroupAbsenceType>) getQueryFactory().from(groupAbsenceType)
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
        if (Objects.equals(previous.nextGroupToCheck, group)) {
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

    return (List<GroupAbsenceType>) getQueryFactory().from(groupAbsenceType)
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
      boolean timeForMealticket, String certificateCode, LocalDate expire) {

    QAbsenceType absenceType = QAbsenceType.absenceType;
    AbsenceType obj = (AbsenceType) getQueryFactory()
        .from(absenceType)
        .where(absenceType.code.equalsIgnoreCase(code)).fetchOne();
    if (obj == null) {
      obj = new AbsenceType();
    }
    obj.code = code;
    obj.description = description;
    obj.justifiedTime = minutes;
    obj.justifiedTypesPermitted.clear();
    //obj.justifiedTimeAtWork = null;
    obj.save();
    JPA.em().flush();
    for (JustifiedType justified : justifiedTypePermitted) {
      obj.justifiedTypesPermitted.add(justified);
    }
    obj.replacingType = complationType;
    obj.replacingTime = complationTime;
    obj.internalUse = internalUse;
    obj.timeForMealTicket = timeForMealticket;
    obj.consideredWeekEnd = consideredWeekEnd;
    obj.certificateCode = code;
    if (expire != null) {
      obj.validTo = expire;
    } else {
      obj.validTo = new LocalDate(2099, 12, 31);
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
      exObj.code += "ex";
      exObj.save();
      JPA.em().flush();
    }

    obj.code = newCode;
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
    final QAbsenceTypeJustifiedBehaviour behaviour =
        QAbsenceTypeJustifiedBehaviour.absenceTypeJustifiedBehaviour;

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
    return (List<Absence>) getQueryFactory().from(absence)
        .leftJoin(absence.justifiedType).fetchJoin()
        .leftJoin(absence.absenceType, absenceType).fetchJoin()
        .leftJoin(absenceType.complationGroup).fetchJoin()
        .leftJoin(absenceType.replacingGroup).fetchJoin()
        .leftJoin(absenceType.takableGroup).fetchJoin()
        .leftJoin(absenceType.takenGroup).fetchJoin()
        .leftJoin(absenceType.justifiedBehaviours, behaviour).fetchJoin()
        .leftJoin(behaviour.justifiedBehaviour).fetchJoin()
        .leftJoin(absence.troubles).fetchJoin()
        .leftJoin(absence.personDay).fetchJoin()
        .where(absence.personDay.person.eq(person)
            .and(conditions))
        .orderBy(absence.personDay.date.asc()).distinct().fetch();
  }

  /**
   * Inizializzazioni per quella persona.
   *
   * @param person persona
   * @return list
   */
  public List<InitializationGroup> personInitializationGroups(Person person) {
    QInitializationGroup initializationGroup = QInitializationGroup.initializationGroup;
    final JPQLQuery<?> query = getQueryFactory()
        .from(initializationGroup)
        .where(initializationGroup.person.eq(person));
    return (List<InitializationGroup>) query.fetch();
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
      if (group.initializable) {
        categories.put(group.category.priority, group.category);
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
    for (GroupAbsenceType group : category.groupAbsenceTypes) {
      if (group.initializable) {
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
    final JPQLQuery<?> query = getQueryFactory()
        .from(absence)
        .leftJoin(absence.troubles, QAbsenceTrouble.absenceTrouble)
        .leftJoin(absence.personDay, QPersonDay.personDay)
        .leftJoin(absence.personDay.person, QPerson.person)
        .where(absence.troubles.isNotEmpty().and(absence.personDay.person.in(people)));
    List<Absence> absences = (List<Absence>) query.fetch();
    Map<Person, List<Absence>> map = Maps.newHashMap();
    for (Absence trouble : absences) {
      List<Absence> personAbsences = map.get(trouble.personDay.person);
      if (personAbsences == null) {
        personAbsences = Lists.newArrayList();
        map.put(trouble.personDay.person, personAbsences);
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
    return (List<Absence>) getQueryFactory()
        .from(absence)
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
    return (List<Absence>) getQueryFactory()
        .from(absence)
        .leftJoin(absence.personDay, QPersonDay.personDay)
        .where(absence.absenceType.code.in(codes))
        .fetch();
  }
} 
