package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import models.Institute;
import models.query.QInstitute;

/**
 * Dao per gli istituti.
 * 
 * @author cristian
 *
 */
public class InstituteDao extends DaoBase {

  @Inject
  InstituteDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }
  
  /**
   * Tutti gli istituti presenti.
   * 
   * @return la lista di tutti gli uffici presenti sul database.
   */
  public List<Institute> getAllInstitutes() {

    final QInstitute institute = QInstitute.institute;

    return getQueryFactory().selectFrom(institute).orderBy(institute.name.asc()).fetch();
  }

}