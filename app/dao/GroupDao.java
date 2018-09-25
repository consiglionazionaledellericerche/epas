package dao;

import java.util.List;
import javax.persistence.EntityManager;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
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
  
  /**
   * 
   * @param person la persona di cui cerco i gruppi in cui è responsabile
   * @return la lista dei gruppi di cui Person è responsabile.
   */
  public List<Group> groupsByManager(Optional<Person> person) {
    final QGroup group = QGroup.group;
    BooleanBuilder builder = new BooleanBuilder();
    if (person.isPresent()) {
      builder.and(group.manager.eq(person.get()));
    }    
    final JPQLQuery query = getQueryFactory().from(group).where(builder);
    return query.list(group);
  }
}
