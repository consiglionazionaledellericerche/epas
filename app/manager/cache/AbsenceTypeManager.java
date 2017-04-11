package manager.cache;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

import java.util.Set;

import javax.persistence.EntityManager;

import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.query.QAbsenceType;
import models.absences.query.QGroupAbsenceType;
import models.enumerate.AbsenceTypeMapping;

import org.apache.commons.lang.NotImplementedException;
import org.testng.collections.Sets;

import play.cache.Cache;

public class AbsenceTypeManager {

  protected final JPQLQueryFactory queryFactory;
  private static final String ABT_PREFIX = "abt";
  private static final String REDUCING_SET = "reducing-set";
  private static final String CODES_FOR_VACATION = "codes-for-vacation";

  @Inject
  AbsenceTypeManager(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    this.queryFactory = new JPAQueryFactory(emp);
  }

  /**
   * Costruisce o se presente aggiorna l'AbsenceType. In caso di aggiornamento invalida la cache.
   */
  public void saveAbsenceType(String code, String value)
          throws NotImplementedException {
    throw new NotImplementedException();
  }

  /**
   * Preleva dalla cache l'absenceType.
   */
  public AbsenceType getAbsenceType(
          String code) {

    Preconditions.checkNotNull(code);

    String key = ABT_PREFIX + code;

    AbsenceType value = (AbsenceType) Cache.get(key);

    if (value == null) {

      value = getAbsenceTypeByCode(code);

      Preconditions.checkNotNull(value);

      Cache.set(key, value);
    }
    value.merge();
    return value;

  }

  /**
   * Codici per il calcolo della riduzione delle ferie cache.
   * @return set
   */
  public Set<AbsenceType> reducingCodes() {

    Set<AbsenceType> value = (Set<AbsenceType>) Cache.get(REDUCING_SET);

    if (value == null) {
      value = getReducingAccruingDaysForVacations();
      Cache.set(REDUCING_SET, value);
    }
    for (AbsenceType abt : value) {
      abt.merge();
    }

    return value;
  }

  /**
   * Codici per il calcolo delle ferie cache.
   * @return set
   */
  public Set<AbsenceType> codesForVacations() {
    Set<AbsenceType> value = (Set<AbsenceType>) Cache.get(CODES_FOR_VACATION);
    value = getCodesForVacations(); //To remove
    if (value == null) {
      value = getCodesForVacations();
      Cache.set(CODES_FOR_VACATION, value);
    }
    for (AbsenceType abt : value) {
      abt.merge();
    }

    return value;
  }

  /**
   * @return lo AbsenceType relativo al codice code passato come parametro.
   */
  private AbsenceType getAbsenceTypeByCode(
          String code) {

    Preconditions.checkNotNull(code);

    final QAbsenceType absenceType = QAbsenceType.absenceType;

    JPQLQuery query = queryFactory.from(absenceType)
            .where(absenceType.code.eq(code));

    return query.singleResult(absenceType);
  }

  /**
   * @return set di tutti i codici di assenza che prevedono la riduzione dei giorni dell'anno
   *     su cui computare la maturazione delle ferie.
   */
  private Set<AbsenceType> getReducingAccruingDaysForVacations() {

    QGroupAbsenceType groupAbsenceType = QGroupAbsenceType.groupAbsenceType;

    GroupAbsenceType group = queryFactory.from(groupAbsenceType)
            .leftJoin(groupAbsenceType.takableAbsenceBehaviour)
            .leftJoin(groupAbsenceType.takableAbsenceBehaviour.takableCodes)
            .where(groupAbsenceType.name.eq(GroupAbsenceType.DefaultGroup.RIDUCE_FERIE_CNR.name()))
            .singleResult(groupAbsenceType);
    
    if (group != null) {
      return group.takableAbsenceBehaviour.takableCodes;
    }
    
    // Nel caso il gruppo non esista si applica la versione precedente.
    
    QAbsenceType absenceType = QAbsenceType.absenceType;
    
    JPQLQuery query = queryFactory.from(absenceType)
        .where(absenceType.code.startsWith("24")
        .or(absenceType.code.startsWith("25")
        .or(absenceType.code.startsWith("34")
        .or(absenceType.code.startsWith("17C")
        .or(absenceType.code.startsWith("C17")
        .or(absenceType.code.startsWith("C18")))))));
    return Sets.newHashSet(query.list(absenceType));
  }

  /**
   * @return set di tutti i codici di assenza da considerare per il calcolo delle ferie.
   */
  private Set<AbsenceType> getCodesForVacations() {

    Set<AbsenceType> codes = Sets.newHashSet();
    codes.add(getAbsenceType(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode()));
    codes.add(getAbsenceType(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode()));
    codes.add(getAbsenceType(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode()));
    codes.add(getAbsenceType(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode()));
    codes.addAll(reducingCodes());

    return codes;

  }

}
