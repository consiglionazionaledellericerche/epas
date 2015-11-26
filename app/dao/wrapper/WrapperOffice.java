package dao.wrapper;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.RoleDao;

import manager.ConfGeneralManager;

import models.Office;
import models.Role;
import models.UsersRolesOffices;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * @author alessandro
 */
public class WrapperOffice implements IWrapperOffice {

  private final Office value;
  private final RoleDao roleDao;
  private final ConfGeneralManager confGeneralManager;

  /**
   *
   * @param office
   * @param roleDao
   * @param confGeneralManager
   */
  @Inject
  WrapperOffice(@Assisted Office office, RoleDao roleDao, ConfGeneralManager confGeneralManager) {
    value = office;
    this.roleDao = roleDao;
    this.confGeneralManager = confGeneralManager;
  }

  @Override
  public final Office getValue() {
    return value;
  }

  @Override
  public final LocalDate initDate() {
    return confGeneralManager
            .getLocalDateFieldValue(Parameter.INIT_USE_PROGRAM, this.value).get();
  }

  /**
   * Gli amministratori tecnici dell'office.
   */
  public List<UsersRolesOffices> getTecnicalAdmins() {

    Role roleAdmin = roleDao.getRoleByName(Role.TECNICAL_ADMIN);
    List<UsersRolesOffices> uroList = Lists.newArrayList();
    for (UsersRolesOffices uro : this.value.usersRolesOffices) {

      if (uro.office.id.equals(this.value.id) && uro.role.id.equals(roleAdmin.id)
              && uro.user.person != null)
        uroList.add(uro);
    }
    return uroList;
  }


  /**
   * Gli amministratori dell'office.
   */
  public List<UsersRolesOffices> getPersonnelAdmins() {

    Role roleAdmin = roleDao.getRoleByName(Role.PERSONNEL_ADMIN);
    List<UsersRolesOffices> uroList = Lists.newArrayList();
    for (UsersRolesOffices uro : this.value.usersRolesOffices) {

      if (uro.office.id.equals(this.value.id) && uro.role.id.equals(roleAdmin.id)
              && uro.user.person != null)
        uroList.add(uro);
    }
    return uroList;
  }

  /**
   * I mini amministratori dell'office.
   */
  public List<UsersRolesOffices> getMiniAdmins() {

    Role roleAdminMini = roleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);
    List<UsersRolesOffices> uroList = Lists.newArrayList();
    for (UsersRolesOffices uro : this.value.usersRolesOffices) {

      if (uro.office.id.equals(this.value.id) && uro.role.id.equals(roleAdminMini.id)
              && uro.user.person != null)
        uroList.add(uro);
    }
    return uroList;
  }

}
