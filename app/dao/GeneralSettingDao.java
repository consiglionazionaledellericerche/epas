package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.Optional;
import javax.persistence.EntityManager;
import models.GeneralSetting;
import models.query.QGeneralSetting;

/**
 * DAO per le impostazioni generali di ePAS.
 * 
 * @author cristian
 *
 */
public class GeneralSettingDao extends DaoBase {

  @Inject
  GeneralSettingDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * In caso non siano ancora mai state salvate, le restituisce nuove.
   *
   * @return le impostazioni generali.
   */
  public GeneralSetting generalSetting() {
    return Optional.ofNullable(queryFactory
        .selectFrom(QGeneralSetting.generalSetting).fetchOne())
        .orElseGet(GeneralSetting::new);
  }
}