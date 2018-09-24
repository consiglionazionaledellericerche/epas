package dao;

import java.util.List;
import javax.persistence.EntityManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.flows.Group;
import models.flows.query.QGroup;

@Slf4j
public class GroupDao extends DaoBase {

  @Inject
  GroupDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory wrapperFactory) {
    super(queryFactory, emp);
  }
  
  /**
   * 
   * @param office la sede di cui si richiedono i gruppi
   * @return la lista dei gruppi i cui responsabili appartengono alla sede passata come parametro.
   */
  public List<Group> groupsByOffice(Office office) {
    final QGroup group = QGroup.group;
    final JPQLQuery query = getQueryFactory().from(group).where(group.manager.office.eq(office));
    return query.list(group);
  }
}
