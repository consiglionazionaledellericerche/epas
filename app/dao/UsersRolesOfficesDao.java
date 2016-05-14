package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import helpers.ModelQuery;

import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.query.QBadgeReader;
import models.query.QPerson;
import models.query.QPersonHourForOvertime;
import models.query.QPersonReperibility;
import models.query.QPersonShift;
import models.query.QQualification;
import models.query.QRole;
import models.query.QUser;
import models.query.QUsersRolesOffices;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

public class UsersRolesOfficesDao extends DaoBase {

  @Inject
  UsersRolesOfficesDao(JPQLQueryFactory queryFactory,
                       Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  public UsersRolesOffices getById(Long id) {
    QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final JPQLQuery query = getQueryFactory().from(uro)
            .where(uro.id.eq(id));
    return query.singleResult(uro);
  }

  /**
   * @return l'usersRolesOffice associato ai parametri passati.
   */
  public Optional<UsersRolesOffices> getUsersRolesOffices(User user, Role role, Office office) {
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final JPQLQuery query = getQueryFactory().from(uro)
            .where(uro.user.eq(user)
                    .and(uro.role.eq(role)
                            .and(uro.office.eq(office))));

    return Optional.fromNullable(query.singleResult(uro));
  }


  /**
   * @return l'usersRolesOffice associato ai parametri passati.
   */
  public Optional<UsersRolesOffices> getUsersRolesOfficesByUserAndOffice(User user, Office office) {
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final JPQLQuery query = getQueryFactory().from(uro)
            .where(uro.user.eq(user)
                    .and(uro.office.eq(office)));

    return Optional.fromNullable(query.singleResult(uro));
  }
  
  /**
   * 
   * @param user
   * @return la lista di tutti gli usersRolesOffices associati al parametro passato.
   */
  public List<UsersRolesOffices> getUsersRolesOfficesByUser(User user) {
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final JPQLQuery query = getQueryFactory().from(uro).where(uro.user.eq(user));
    return query.list(uro);
  }

  /**
   * La lista di tutti i ruoli per l'user. Utilizzato per visualizzare gli elementi della navbar.
   */
  public List<Role> getUserRole(User user) {

    final QUsersRolesOffices quro = QUsersRolesOffices.usersRolesOffices;
    final QRole qr = QRole.role;

    final JPQLQuery query = getQueryFactory().from(qr)
            .leftJoin(qr.usersRolesOffices, quro).fetch()
            .distinct();

    final BooleanBuilder condition = new BooleanBuilder();
    condition.and(quro.user.eq(user));

    query.where(condition);

    return ModelQuery.simpleResults(query, qr).list();
  }

  /**
   * Tutti i ruoli assegnati per quella sede.
   * @param office
   * @return
   */
  public List<UsersRolesOffices> getUsersRolesOfficesByPersonInOffice(Office office) {
    
    final QUsersRolesOffices uro = QUsersRolesOffices.usersRolesOffices;
    final QUser user = QUser.user;
    final QPerson person = QPerson.person;
    final QBadgeReader badgeReader = QBadgeReader.badgeReader;
    
    final JPQLQuery query = getQueryFactory().from(uro)
        
        // Fetch necessarie per costruire con una query sola uro->user->person
        // TODO: metterle in un queryFactory e renderla disponibile ad altre chiamate
        
        .leftJoin(uro.user, user).fetch()
        .leftJoin(user.badgeReader, badgeReader).fetch()
        .leftJoin(user.badgeReader.user, user).fetch()
        .leftJoin(uro.user.person, person).fetch()
        .leftJoin(person.reperibility, QPersonReperibility.personReperibility).fetch()
        .leftJoin(person.personShift, QPersonShift.personShift).fetch()
        .leftJoin(person.personHourForOvertime, QPersonHourForOvertime.personHourForOvertime).fetch()
        .leftJoin(person.qualification, QQualification.qualification1).fetch();
//        .leftJoin(user.person, person).fetch()
//        .leftJoin(user.person.reperibility, QPersonReperibility.personReperibility).fetch()
//        .leftJoin(user.person.personHourForOvertime, QPersonHourForOvertime.personHourForOvertime).fetch()
//        .leftJoin(user.person.personShift, QPersonShift.personShift).fetch()
//        .leftJoin(user.person.qualification, QQualification.qualification1).fetch()
//
//        .where(person.office.eq(office));
    
    return query.list(uro);
        
  }
  
  public User fetchUser(Person person) {
    
    final QUser user = QUser.user;
    
    final JPQLQuery query = getQueryFactory()
        .from(user)
        .leftJoin(user.person, QPerson.person)
        .where(user.person.eq(person));
    
    return query.list(user).get(0);
    
  }
  
  
  

}
