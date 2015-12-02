package manager;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import dao.OfficeDao;
import dao.RoleDao;
import dao.wrapper.IWrapperFactory;

import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class SecureManager {

  //private final static Logger log = LoggerFactory.getLogger(SecurityManager.class);

  @Inject
  private IWrapperFactory wrapperFactory;

  @Inject
  private RoleDao roleDao;

  @Inject
  private OfficeDao officeDao;

  /**
   * @return la lista degli uffici permessi per l'utente user passato come parametro.
   */
  private Set<Office> getOfficeAllowed(User user, ImmutableList<String> rolesNames) {

    Preconditions.checkNotNull(user);
    Preconditions.checkState(user.isPersistent());

    final List<Role> roles = roleDao.getRolesByNames(rolesNames);
    //PROVVISORIO: per fare in modo che super utenti come developer e admin possano
    //vedere tutte le sedi presenti sul db in varie circostanze
    if (user.username.equals("developer") || user.username.equals("admin")) {
      Set<Office> officeSet = Sets.newHashSet();

      officeSet.addAll(officeDao.getAllOffices());

      return officeSet;
    }

    return FluentIterable.from(user.usersRolesOffices)
        .filter(new Predicate<UsersRolesOffices>() {
          @Override
          public boolean apply(UsersRolesOffices input) {
            if (!roles.contains(input.role)) {
              return false;
            }
            return true;
          }
        })
        .transform(new Function<UsersRolesOffices, Office>() {
          @Override
          public Office apply(UsersRolesOffices uro) {
            if (roles.contains(uro.role)) {
              return uro.office;
            }
            return null;
          }
        }).toSet();

  }

  /**
   * Le sedi per le quali l'utente dispone di almeno il ruolo di sola lettura.
   */
  public Set<Office> officesReadAllowed(User user) {

    ImmutableList<String> rolesNames = ImmutableList.of(
        Role.ADMIN,
        Role.DEVELOPER,
        Role.PERSONNEL_ADMIN,
        Role.PERSONNEL_ADMIN_MINI);

    return getOfficeAllowed(user, rolesNames);
  }

  /**
   * Le sedi per le quali l'utente dispone del ruolo di scrittura.
   */
  public Set<Office> officesWriteAllowed(User user) {

    ImmutableList<String> rolesNames = ImmutableList.of(
        Role.ADMIN,
        Role.DEVELOPER,
        Role.PERSONNEL_ADMIN);

    return getOfficeAllowed(user, rolesNames);
  }

  /**
   * Le sedi per le quali l'utente dispone del ruolo di badge reader.
   */
  public Set<Office> officesBadgeReaderAllowed(User user) {

    ImmutableList<String> roles = ImmutableList.of(Role.BADGE_READER);

    return getOfficeAllowed(user, roles);
  }

  /**
   * Le sedi per le quali l'utente dispone del ruolo di amministratore di sistema.
   */
  public Set<Office> officesSystemAdminAllowed(User user) {

    ImmutableList<String> roles = ImmutableList.of(Role.ADMIN, Role.DEVELOPER);

    return getOfficeAllowed(user, roles);

  }
}
