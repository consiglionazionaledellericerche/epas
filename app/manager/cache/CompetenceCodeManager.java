package manager.cache;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

import models.CompetenceCode;
import models.query.QCompetenceCode;

import org.apache.commons.lang.NotImplementedException;

import play.cache.Cache;

import javax.persistence.EntityManager;

public class CompetenceCodeManager {

  protected final JPQLQueryFactory queryFactory;
  private static final String COMPETENCE_PREFIX = "comp";

  @Inject
  CompetenceCodeManager(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    this.queryFactory = new JPAQueryFactory(emp);
  }

  /**
   * Costruisce o se presente aggiorna il competence code In caso di aggiornamento invalida la
   * cache.
   */
  public void saveCompetenceCode(String code, String value)
          throws NotImplementedException {
    throw new NotImplementedException();
  }

  /**
   * Preleva dalla cache il competence code.
   */
  public CompetenceCode getCompetenceCode(
          String code) {

    Preconditions.checkNotNull(code);

    String key = COMPETENCE_PREFIX + code;

    CompetenceCode value = (CompetenceCode) Cache.get(key);

    if (value == null) {

      value = getCompetenceCodeByCode(code);

      Preconditions.checkNotNull(value);

      Cache.set(key, value);
    }
    value.merge();
    return value;

  }

  /**
   * @return il CompetenceCode relativo al codice code passato come parametro.
   */
  private CompetenceCode getCompetenceCodeByCode(
          String code) {

    Preconditions.checkNotNull(code);

    final QCompetenceCode compCode = QCompetenceCode.competenceCode;

    JPQLQuery query = queryFactory.from(compCode)
            .where(compCode.code.eq(code));

    return query.singleResult(compCode);
  }

}
