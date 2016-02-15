package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.RoleDao;
import dao.UsersRolesOfficesDao;

import models.Configuration;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.enumerate.EpasParam;
import models.enumerate.EpasParam.EpasParamValueType;
import models.enumerate.EpasParam.EpasParamValueType.IpList;

import java.util.List;
import java.util.Set;

public class OfficeManager {


  private final UsersRolesOfficesDao usersRolesOfficesDao;
  private final RoleDao roleDao;
  private final ConfigurationManager configurationManager;

  @Inject
  public OfficeManager(
      UsersRolesOfficesDao usersRolesOfficesDao,
      RoleDao roleDao,
      ConfigurationManager configurationManager) {
    this.usersRolesOfficesDao = usersRolesOfficesDao;
    this.roleDao = roleDao;
    this.configurationManager = configurationManager;
  }

  /**
   * Assegna i diritti agli amministratori. Da chiamare successivamente alla creazione.
   */
  public void setSystemUserPermission(Office office) {

    User admin = User.find("byUsername", Role.ADMIN).first();
    User developer = User.find("byUsername", Role.DEVELOPER).first();

    Role roleAdmin = roleDao.getRoleByName(Role.ADMIN);
    Role roleDeveloper = roleDao.getRoleByName(Role.DEVELOPER);

    setUro(admin, office, roleAdmin);
    setUro(developer, office, roleDeveloper);

  }

  /**
   * @return true Se il permesso su quell'ufficio viene creato, false se è già esistente
   */
  public boolean setUro(User user, Office office, Role role) {

    Optional<UsersRolesOffices> uro = usersRolesOfficesDao.getUsersRolesOffices(user, role, office);

    if (!uro.isPresent()) {

      UsersRolesOffices newUro = new UsersRolesOffices();
      newUro.user = user;
      newUro.office = office;
      newUro.role = role;
      newUro.save();
      return true;
    }

    return false;
  }

  
  /**
   * Le sedi che hanno tale ip abilitato per timbrature.
   * @param ip indirizzo da verificare
   * @return insieme sedi
   */
  public Set<Office> getOfficesWithAllowedIp(String ip) {

    Preconditions.checkNotNull(ip);
    Set<Office> offices = Sets.newHashSet();
    List<Configuration> configurationWithType = configurationManager
        .configurationWithType(EpasParam.ADDRESSES_ALLOWED);
    
    for (Configuration configuration : configurationWithType) {
      IpList ipList = (IpList)EpasParamValueType.parseValue(EpasParamValueType.IP_LIST, 
          (String)configuration.getValue());
      if (ipList.ipList.contains(ip)) {
        offices.add(configuration.office);
      }
    }
    return offices;
  }

}
