package dao.absences;

import com.google.common.base.Optional;
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

import java.util.List;
import java.util.Set;

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
  
  public List<GroupAbsenceType> allGroupAbsenceType() {
    
    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;
    
    return getQueryFactory().from(groupAbsenceType).list(groupAbsenceType);
  }

  
  public AbsenceType buildOrEditAbsenceType(String code, String description, int minutes, 
      Set<JustifiedType> justifiedTypePermitted, boolean internalUse, boolean consideredWeekEnd, 
      boolean timeForMealticket, String certificateCode) {
    
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
    obj.justifiedTypesPermitted = justifiedTypePermitted;
    obj.internalUse = internalUse;
    obj.timeForMealTicket = timeForMealticket;
    obj.consideredWeekEnd = consideredWeekEnd;
    obj.certificateCode = code;         
    
    obj.save();
    return obj;

  }
  
  /**
   * Le assenze effettuate dalla persona nel periodo specificato e con i codici riportati.
   * @param person
   * @param begin
   * @param end
   * @param codeList
   * @param ordered
   * @return
   */
  public List<Absence> getAbsencesInCodeList(Person person, LocalDate begin, LocalDate end, 
      List<AbsenceType> codeList, boolean ordered) {

    final QAbsence absence = QAbsence.absence;
    BooleanBuilder conditions = new BooleanBuilder();
    if (begin != null) {
      conditions.and(absence.personDay.date.goe(begin));
    }
    if (end!= null) {
      conditions.and(absence.personDay.date.loe(end));
    }
    final JPQLQuery query = getQueryFactory().from(absence)
        .leftJoin(absence.personDay).fetch()
        .where(absence.personDay.person.eq(person)
            .and(conditions)
            .and(absence.absenceType.in(codeList)));
    if (ordered) {
      query.orderBy(absence.personDay.date.asc());
    }
    return query.list(absence);
  }
}
