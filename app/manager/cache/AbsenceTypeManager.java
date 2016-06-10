package manager.cache;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

import models.AbsenceType;
import models.enumerate.AbsenceTypeMapping;
import models.query.QAbsenceType;

import org.apache.commons.lang.NotImplementedException;

import play.cache.Cache;

import java.util.List;

import javax.persistence.EntityManager;

public class AbsenceTypeManager {

  protected final JPQLQueryFactory queryFactory;
  private static final String ABT_PREFIX = "abt";
  private static final String POST_PARTUM_LIST = "post-partum-list";
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

  public List<AbsenceType> postPartumCodes() {

    List<AbsenceType> value = (List<AbsenceType>) Cache.get(POST_PARTUM_LIST);

    if (value == null) {
      value = getReducingAccruingDaysForVacations();
      Cache.set(POST_PARTUM_LIST, value);
    }
    for (AbsenceType abt : value) {
      abt.merge();
    }

    return value;
  }

  public List<AbsenceType> codesForVacations() {
    List<AbsenceType> value = (List<AbsenceType>) Cache.get(CODES_FOR_VACATION);

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
   * @return la lista di tutti i codici di assenza che prevedono la riduzione dei giorni dell'anno
   *     su cui computare la maturazione delle ferie.
   */
  private List<AbsenceType> getReducingAccruingDaysForVacations() {

    QAbsenceType absenceType = QAbsenceType.absenceType;

    JPQLQuery query = queryFactory.from(absenceType)
            .where(absenceType.code.startsWith("24")
            .or(absenceType.code.startsWith("25")
            .or(absenceType.code.startsWith("34")
            .or(absenceType.code.startsWith("17C")
            .or(absenceType.code.startsWith("C17")
            .or(absenceType.code.startsWith("C18")))))));
    return query.list(absenceType);

  }

  /**
   * @return la lista di tutti i codici di assenza che prevedono la riduzione dei giorni dell'anno
   *     su cui computare la maturazione delle ferie.
   */
  private List<AbsenceType> getCodesForVacations() {

    List<AbsenceType> codes = Lists.newArrayList();
    codes.add(getAbsenceType(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode()));
    codes.add(getAbsenceType(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode()));
    codes.add(getAbsenceType(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode()));
    codes.add(getAbsenceType(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode()));
    codes.addAll(postPartumCodes());

    return codes;

  }

}
