package dao.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import dao.DaoBase;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.query.QAbsence;
import models.absences.query.QAbsenceType;
import models.absences.query.QCategoryGroupAbsenceType;
import models.absences.query.QComplationAbsenceBehaviour;
import models.absences.query.QGroupAbsenceType;
import models.absences.query.QJustifiedType;
import models.absences.query.QTakableAbsenceBehaviour;

import org.joda.time.LocalDate;

import play.db.jpa.JPA;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.persistence.EntityManager;

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
   * Ritorna il JustifiedType con quel nome. Se non esiste lo crea.
   * @param name
   * @return
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
   * Ritorna il JustifiedType con quel nome. Se non esiste lo crea.
   * @param name
   * @return
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
  
  public Optional<ComplationAbsenceBehaviour> complationAbsenceBehaviourByName(String name) {
    
    QComplationAbsenceBehaviour complationAbsenceBehaviour = 
        QComplationAbsenceBehaviour.complationAbsenceBehaviour;
    
    return Optional.fromNullable(getQueryFactory().from(complationAbsenceBehaviour)
        .where(complationAbsenceBehaviour.name.eq(name)).singleResult(complationAbsenceBehaviour));
  }
  
  public Optional<TakableAbsenceBehaviour> takableAbsenceBehaviourByName(String name) {
    
    QTakableAbsenceBehaviour takableAbsenceBehaviour = 
        QTakableAbsenceBehaviour.takableAbsenceBehaviour;
    
    return Optional.fromNullable(getQueryFactory().from(takableAbsenceBehaviour)
        .where(takableAbsenceBehaviour.name.eq(name)).singleResult(takableAbsenceBehaviour));
  }
  
  public Optional<GroupAbsenceType> groupAbsenceTypeByName(String name) {
    
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;
    
    return Optional.fromNullable(getQueryFactory().from(groupAbsenceType)
        .where(groupAbsenceType.name.eq(name)).singleResult(groupAbsenceType));
  }
  
  public List<GroupAbsenceType> groupsAbsenceTypeByName(List<String> names) {
    
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;
    
    return getQueryFactory().from(groupAbsenceType)
        .where(groupAbsenceType.name.in(names)).list(groupAbsenceType);
  }
  
  public GroupAbsenceType groupAbsenceTypeById(Long id) {
    
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;
    
    return getQueryFactory().from(groupAbsenceType)
        .where(groupAbsenceType.id.eq(id)).singleResult(groupAbsenceType);
  }
  
  public List<GroupAbsenceType> allGroupAbsenceType() {
    
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;

    return getQueryFactory().from(groupAbsenceType)
        .leftJoin(groupAbsenceType.category).fetch()
        .leftJoin(groupAbsenceType.previousGroupChecked).fetch()
        .list(groupAbsenceType);
  }
  
  public GroupAbsenceType firstGroupOfChain(GroupAbsenceType groupAbsenceType) {
    GroupAbsenceType group = groupAbsenceType;
    List<GroupAbsenceType> all = allGroupAbsenceType();
    boolean changed = true;
    while (changed) {
      changed = false;
      for (GroupAbsenceType previous : all) {
        if (previous.nextGroupToCheck != null 
            && previous.nextGroupToCheck.equals(group)) {
          group= previous;
          changed = true;
        }
      }
    }
    return group;
  }
  
  
  public List<GroupAbsenceType> groupAbsenceTypeOfPattern(GroupAbsenceTypePattern pattern) {
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;

    return getQueryFactory().from(groupAbsenceType)
        .leftJoin(groupAbsenceType.category).fetch()
        .leftJoin(groupAbsenceType.previousGroupChecked).fetch()
        .where(groupAbsenceType.pattern.eq(pattern))
        .list(groupAbsenceType);
  }
  
  public Set<GroupAbsenceType> involvedGroupAbsenceType(AbsenceType absenceType, 
      boolean filterNotProgrammed) {

    //TODO: da fare la fetch perchè è usato in tabellone timbrature per ogni codice assenza.
    
    Set<GroupAbsenceType> groups = Sets.newHashSet();
    for (TakableAbsenceBehaviour behaviour : absenceType.takableGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    for (TakableAbsenceBehaviour behaviour : absenceType.takenGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    for (ComplationAbsenceBehaviour behaviour : absenceType.complationGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    for (ComplationAbsenceBehaviour behaviour : absenceType.replacingGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    if (!filterNotProgrammed) {
      return groups;
    }
    Set<GroupAbsenceType> filteredGroup = Sets.newHashSet();
    for (GroupAbsenceType groupAbsenceType : groups) {
      if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.programmed)) {
        filteredGroup.add(groupAbsenceType);
      }
    }
    return filteredGroup;
  }
  
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
    obj.justifiedTypesPermitted = Sets.newHashSet();
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
   * @param person
   * @param begin
   * @param end
   * @param codeList
   * @param ordered
   * @return
   */
  public List<Absence> orderedAbsences(Person person, LocalDate begin, LocalDate end, 
      List<AbsenceType> codeList) {

    final QAbsence absence = QAbsence.absence;
   
    
    BooleanBuilder conditions = new BooleanBuilder();
    if (begin != null) {
      conditions.and(absence.personDay.date.goe(begin));
    }
    if (end!= null) {
      conditions.and(absence.personDay.date.loe(end));
    }
    if (!codeList.isEmpty()) {
      conditions.and(absence.absenceType.in(codeList));
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
  
  public void fetchAbsenceTypes() {
    
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;
    QAbsenceType absenceType = QAbsenceType.absenceType;
    
    //fetch all absenceTypes
    getQueryFactory()
        .from(absenceType)
        .leftJoin(absenceType.justifiedTypesPermitted).fetch()
        .leftJoin(absenceType.complationGroup).fetch()
        .leftJoin(absenceType.replacingGroup).fetch()
        .leftJoin(absenceType.takableGroup).fetch()
        .leftJoin(absenceType.takenGroup).fetch()
        .list(absenceType);
    
    //fetch all absenceTypeGroups
    getQueryFactory().from(groupAbsenceType)
        .leftJoin(groupAbsenceType.category).fetch()
        .leftJoin(groupAbsenceType.complationAbsenceBehaviour).fetch()
        .leftJoin(groupAbsenceType.complationAbsenceBehaviour.complationCodes).fetch()
        .leftJoin(groupAbsenceType.complationAbsenceBehaviour.replacingCodes).fetch()
        .leftJoin(groupAbsenceType.takableAbsenceBehaviour).fetch()
        .leftJoin(groupAbsenceType.takableAbsenceBehaviour.takableCodes).fetch()
        .leftJoin(groupAbsenceType.takableAbsenceBehaviour.takenCodes).fetch()
        .leftJoin(groupAbsenceType.nextGroupToCheck).fetch()
        .leftJoin(groupAbsenceType.previousGroupChecked).fetch()
        .list(groupAbsenceType);
    
  }
  
  /**
   * Ordina per data tutte le liste di assenze in una unica lista.
   * @param absences
   * @return
   */
  public List<Absence> orderAbsences(List<Absence>... absences) {
    SortedMap<LocalDate, Set<Absence>> map = Maps.newTreeMap();
    for (List<Absence> list : absences) {
      for (Absence absence : list) {
        Set<Absence> set = map.get(absence.getAbsenceDate());
        if (set == null) {
          set = Sets.newHashSet();
          map.put(absence.getAbsenceDate(), set);
        }
        set.add(absence);
      }
    }
    List<Absence> result = Lists.newArrayList();
    for (Set<Absence> set : map.values()) {
      result.addAll(set);
    }
    return result;
  }
  
  public Map<LocalDate, Set<Absence>> mapAbsences(List<Absence> absences, Map<LocalDate, Set<Absence>> map) {
    if (map == null) {
      map = Maps.newHashMap();
    }
    for (Absence absence : absences) {
      Set<Absence> set = map.get(absence);
      if (set == null) {
        set = Sets.newHashSet();
        map.put(absence.getAbsenceDate(), set);
      }
      set.add(absence);
    }
    return map;
  }
}
