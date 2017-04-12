package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.OfficeDao;
import dao.UsersRolesOfficesDao;

import java.util.List;
import java.util.Set;

import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.configurations.EpasParam.EpasParamValueType;
import manager.configurations.EpasParam.EpasParamValueType.IpList;

import models.Configuration;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;

import play.Play;

public class OfficeManager {

  public static final String SKIP_IP_CHECK = "skip.ip.check";

  private final UsersRolesOfficesDao usersRolesOfficesDao;
  private final ConfigurationManager configurationManager;
  private final OfficeDao officeDao;

  @Inject
  public OfficeManager(
      UsersRolesOfficesDao usersRolesOfficesDao,
      ConfigurationManager configurationManager,
      OfficeDao officeDao) {
    this.usersRolesOfficesDao = usersRolesOfficesDao;
    this.configurationManager = configurationManager;
    this.officeDao = officeDao;
  }

  /**
   * @return true Se il permesso su quell'ufficio viene creato, false se è già esistente.
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
   * Le sedi che hanno almeno uno degli ip abilitato per timbrature.
   *
   * @param ipAddresses indirizzi ip da verificare
   * @return Set di uffici abilitati dagli indirizzi ip passati come parametro
   */
  public Set<Office> getOfficesWithAllowedIp(final List<String> ipAddresses) {

    Preconditions.checkNotNull(ipAddresses);
    Preconditions.checkState(!ipAddresses.isEmpty());

    if ("true".equals(Play.configuration.getProperty(SKIP_IP_CHECK))) {
      return FluentIterable.from(officeDao.getAllOffices()).toSet();
    }

    Set<Office> offices = Sets.newHashSet();
    List<Configuration> configurationWithType = configurationManager
        .configurationWithType(EpasParam.ADDRESSES_ALLOWED);

    for (Configuration configuration : configurationWithType) {
      IpList ipList = (IpList) EpasParamValueType.parseValue(EpasParamValueType.IP_LIST,
          (String) configuration.getValue());
      for (String ip : ipAddresses) {
        if (ipList != null && ipList.ipList.contains(ip)) {
          offices.add(configuration.office);
        }
      }
    }
    return offices;
  }

}
