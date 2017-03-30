package dao.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import dao.DaoBase;

import java.util.List;
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
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.query.QAbsence;
import models.absences.query.QAbsenceType;
import models.absences.query.QCategoryGroupAbsenceType;
import models.absences.query.QCategoryTab;
import models.absences.query.QComplationAbsenceBehaviour;
import models.absences.query.QGroupAbsenceType;
import models.absences.query.QInitializationGroup;
import models.absences.query.QJustifiedType;
import models.absences.query.QTakableAbsenceBehaviour;

import org.joda.time.LocalDate;

import play.db.jpa.JPA;

/**
 * Dao per il componente assenze.
 * @author alessandro
 */
public class AbsenceComponentDao extends DaoBase {

  @Inject
  AbsenceComponentDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  
  
  /**
   * @return l'absenceType relativo all'id passato come parametro.
   */
  public Optional<AbsenceType> absenceTypeById(Long id) {

    QAbsenceType absenceType = QAbsenceType.absenceType;
    return Optional.fromNullable(getQueryFactory().from(absenceType)
        .where(absenceType.id.eq(id)).singleResult(absenceType));
  }

  /**
   * @return l'absenceType relativo al codice passato come parametro.
   */
  public Optional<AbsenceType> absenceTypeByCode(String string) {

    QAbsenceType absenceType = QAbsenceType.absenceType;
    final JPQLQuery query = getQueryFactory()
        .from(absenceType)
        .where(absenceType.code.eq(string).or(absenceType.code.equalsIgnoreCase(string)));
    return Optional.fromNullable(query.singleResult(absenceType));
  }
  
  /**
   * @return l'absenceType relativo al codice passato come parametro nel campo certification.
   */
  public Optional<AbsenceType> absenceTypeByCertificationCode(String string) {

    QAbsenceType absenceType = QAbsenceType.absenceType;
    final JPQLQuery query = getQueryFactory()
        .from(absenceType)
        .where(absenceType.certificateCode.eq(string)
            .or(absenceType.certificateCode.equalsIgnoreCase(string)));
    return Optional.fromNullable(query.singleResult(absenceType));
  }
  
  /**
   * Gli absenceTypes con quegli id. Se non vengono caricati tutti gli id ritorna null.
   * @param ids gli id
   * @return list o null
   */
  public List<AbsenceType> absenceTypesByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Lists.newArrayList();
    }
    QAbsenceType absenceType = QAbsenceType.absenceType;
    List<AbsenceType> types = getQueryFactory().from(absenceType)
        .where(absenceType.id.in(ids)).list(absenceType);
    if (types.size() != ids.size()) {
      return null;
    }
    return types;
  }
  
  /**
   * Ritorna il JustifiedType con quel nome. Se non esiste lo crea.
   * @param name nome
   * @return entity
   */
  public JustifiedType getOrBuildJustifiedType(JustifiedTypeName name) {
    
    QJustifiedType justifiedType = QJustifiedType.justifiedType;
    JustifiedType obj = getQueryFactory().from(justifiedType).where(justifiedType.name.eq(name))
        .singleResult(justifiedType);
    if (obj == null) {
      obj = new JustifiedType();
      obj.name = name;
      obj.save();
    }
    return obj;
  }
  
  /**
   * Ritorna la categoria con quel nome. Se non esiste la crea.
   * @param name name
   * @param priority priorità
   * @return la categoria
   */
  public CategoryGroupAbsenceType getOrBuildCategoryType(String name, int priority) {
    
    QCategoryGroupAbsenceType categoryType = QCategoryGroupAbsenceType.categoryGroupAbsenceType;
    CategoryGroupAbsenceType obj = getQueryFactory().from(categoryType)
        .where(categoryType.name.eq(name))
        .singleResult(categoryType);
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
   * @param names nomi
   * @return list
   */
  public List<CategoryGroupAbsenceType> categoryByNames(List<String> names) {
    QCategoryGroupAbsenceType category = QCategoryGroupAbsenceType.categoryGroupAbsenceType;
    return getQueryFactory().from(category)
        .where(category.name.in(names)).list(category);
  }
  
  /**
   * La categoria con quel nome.
   * @param name nome
   * @return entity
   */
  public CategoryGroupAbsenceType categoryByName(String name) {
    QCategoryGroupAbsenceType category = QCategoryGroupAbsenceType.categoryGroupAbsenceType;
    return getQueryFactory().from(category)
        .where(category.name.eq(name)).singleResult(category);
  }
  
  /**
   * Le categorie ordinate per priorità.
   */
  public List<CategoryGroupAbsenceType> categoriesByPriority() {
    QCategoryGroupAbsenceType category = QCategoryGroupAbsenceType.categoryGroupAbsenceType;
    return getQueryFactory().from(category).orderBy(category.priority.asc()).list(category);
  }
  
  /**
   * Le tab ordinate per priorità.
   */
  public List<CategoryTab> tabsByPriority() {
    QCategoryTab categoryTab = QCategoryTab.categoryTab;
    return getQueryFactory().from(categoryTab).orderBy(categoryTab.priority.asc())
        .list(categoryTab);
  }
  
  /**
   * La tab con quel nome.
   * @param name nome
   * @return entity
   */
  public CategoryTab tabByName(String name) {
    QCategoryTab categoryTab = QCategoryTab.categoryTab;
    return getQueryFactory().from(categoryTab)
        .where(categoryTab.name.eq(name)).singleResult(categoryTab);
  }
  
  /**
   * Il comportamento completamento con quel nome.
   * @param name nome
   * @return entity
   */
  public Optional<ComplationAbsenceBehaviour> complationAbsenceBehaviourByName(String name) {
    
    QComplationAbsenceBehaviour complationAbsenceBehaviour = 
        QComplationAbsenceBehaviour.complationAbsenceBehaviour;
    
    return Optional.fromNullable(getQueryFactory().from(complationAbsenceBehaviour)
        .where(complationAbsenceBehaviour.name.eq(name)).singleResult(complationAbsenceBehaviour));
  }
  
  /**
   * Il comportamento limiti con quel nome.
   * @param name nome
   * @return entity
   */
  public Optional<TakableAbsenceBehaviour> takableAbsenceBehaviourByName(String name) {
    
    QTakableAbsenceBehaviour takableAbsenceBehaviour = 
        QTakableAbsenceBehaviour.takableAbsenceBehaviour;
    
    return Optional.fromNullable(getQueryFactory().from(takableAbsenceBehaviour)
        .where(takableAbsenceBehaviour.name.eq(name)).singleResult(takableAbsenceBehaviour));
  }
  
  
  /**
   * Il gruppo con quel nome.
   * @param name nome
   * @return entity
   */
  public Optional<GroupAbsenceType> groupAbsenceTypeByName(String name) {
    
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;
    
    return Optional.fromNullable(getQueryFactory().from(groupAbsenceType)
        .where(groupAbsenceType.name.eq(name)).singleResult(groupAbsenceType));
  }
  
  /**
   * I gruppi con quei nomi.
   * @param names nomi
   * @return entity list
   */
  public List<GroupAbsenceType> groupsAbsenceTypeByName(List<String> names) {
    
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;
    
    return getQueryFactory().from(groupAbsenceType)
        .where(groupAbsenceType.name.in(names)).list(groupAbsenceType);
  }
  
  /**
   * GroupAbsenceType by id.
   * @param id id
   * @return entity
   */
  public GroupAbsenceType groupAbsenceTypeById(Long id) {
    
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;
    
    return getQueryFactory().from(groupAbsenceType)
        .where(groupAbsenceType.id.eq(id)).singleResult(groupAbsenceType);
  }
  
  /**
   * Tutti i gruppi.
   * @return entity list
   */
  public List<GroupAbsenceType> allGroupAbsenceType() {
    
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;

    return getQueryFactory().from(groupAbsenceType)
        .leftJoin(groupAbsenceType.category).fetch()
        .leftJoin(groupAbsenceType.previousGroupChecked).fetch()
        .list(groupAbsenceType);
  }
  
  /**
   * Il primo gruppo della catena cui appartiene groupAbsenceType.
   * @param groupAbsenceType gruppo indagato
   * @return entity
   */
  public GroupAbsenceType firstGroupOfChain(GroupAbsenceType groupAbsenceType) {
    GroupAbsenceType group = groupAbsenceType;
    List<GroupAbsenceType> all = allGroupAbsenceType();
    boolean changed = true;
    while (changed) {
      changed = false;
      for (GroupAbsenceType previous : all) {
        if (previous.nextGroupToCheck != null 
            && previous.nextGroupToCheck.equals(group)) {
          group = previous;
          changed = true;
        }
      }
    }
    return group;
  }
  
  /**
   * I gruppi con quel pattern.
   * @param pattern pattern
   * @return entity list
   */
  public List<GroupAbsenceType> groupAbsenceTypeOfPattern(GroupAbsenceTypePattern pattern) {
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;

    return getQueryFactory().from(groupAbsenceType)
        .leftJoin(groupAbsenceType.category).fetch()
        .leftJoin(groupAbsenceType.previousGroupChecked).fetch()
        .where(groupAbsenceType.pattern.eq(pattern))
        .list(groupAbsenceType);
  }
  
  /**
   * Crea l'assenza con queste caratteristiche. Se esiste già una entity con quel codice la 
   * aggiorna.
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
    AbsenceType obj = getQueryFactory()
        .from(absenceType)
        .where(absenceType.code.equalsIgnoreCase(code)).singleResult(absenceType);
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
   * Rinomina l'assenza code con il nuovo codice newCode. Se newCode era già presente appende
   * al suo codice il postfisso ex.
   * @param code codice 
   * @param newCode nuovo codice
   */
  public void renameCode(String code, String newCode) {
    QAbsenceType absenceType = QAbsenceType.absenceType;

    AbsenceType obj = getQueryFactory()
        .from(absenceType)
        .where(absenceType.code.equalsIgnoreCase(code)).singleResult(absenceType);
    if (obj == null) {
      return;
    }
    
    AbsenceType exObj = getQueryFactory()
        .from(absenceType)
        .where(absenceType.code.equalsIgnoreCase(newCode)).singleResult(absenceType);
    if (exObj != null) {
      exObj.code = exObj.code + "ex";
      exObj.save();
      JPA.em().flush();
    }

    obj.code = newCode;
    obj.save();
  }
  
  /**
   * Le assenze effettuate dalla persona nel periodo specificato e con i codici riportati.
   * Ordinamento per data in ordine crescente.
   * @param person person
   * @param begin data inizio
   * @param end data fine
   * @param codeSet set dei codici
   * @return entity list
   */
  public List<Absence> orderedAbsences(Person person, LocalDate begin, LocalDate end, 
      Set<AbsenceType> codeSet) {

    final QAbsence absence = QAbsence.absence;
   
    
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
    return getQueryFactory().from(absence)
        .leftJoin(absence.justifiedType).fetch()
        .leftJoin(absence.absenceType).fetch()
        .leftJoin(absence.absenceType.complationGroup).fetch()
        .leftJoin(absence.absenceType.replacingGroup).fetch()
        .leftJoin(absence.absenceType.takableGroup).fetch()
        .leftJoin(absence.absenceType.takenGroup).fetch()
        .leftJoin(absence.troubles).fetch()
        .leftJoin(absence.personDay).fetch()
        .where(absence.personDay.person.eq(person)
        .and(conditions))
        .orderBy(absence.personDay.date.asc()).distinct().list(absence);
  }
  
  /**
   * Inizializzazioni per quella persona.
   * @param person persona
   * @return list
   */
  public List<InitializationGroup> personInitializationGroups(Person person) {
    QInitializationGroup initializationGroup = QInitializationGroup.initializationGroup;
    final JPQLQuery query = getQueryFactory()
        .from(initializationGroup)
        .where(initializationGroup.person.eq(person));
    return query.list(initializationGroup);
  }
  
  /**
   * Le categorie che contengono gruppi inizializzabili.
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
} 
