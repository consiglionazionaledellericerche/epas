package manager;

import java.util.Set;

import models.ConfGeneral;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.enumerate.Parameter;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;

import controllers.Security;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import injection.StaticInject;

public class OfficeManager {

	@Inject
	public OfficeManager(UsersRolesOfficesDao usersRolesOfficesDao,
			RoleDao roleDao,ConfGeneralManager confGeneralManager) {
		this.usersRolesOfficesDao = usersRolesOfficesDao;
		this.roleDao = roleDao;
		this.confGeneralManager = confGeneralManager;
	}

	private final UsersRolesOfficesDao usersRolesOfficesDao;
	private final RoleDao roleDao;
	private final ConfGeneralManager confGeneralManager;

	/**
	 * Assegna i diritti agli amministratori. Da chiamare successivamente alla creazione.
	 * @param office
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
	 * 
	 * @param user
	 * @param office
	 * @param role
	 * 
	 * @return true Se il permesso su quell'ufficio viene creato, false se è già esistente
	 */
	public boolean setUro(User user, Office office, Role role){

		Optional<UsersRolesOffices> uro = usersRolesOfficesDao.getUsersRolesOffices(user,role, office);

		if(!uro.isPresent()) {

			UsersRolesOffices newUro = new UsersRolesOffices();
			newUro.user = user;
			newUro.office = office;
			newUro.role = role;
			newUro.save();
			return true;
		}

		return false;
	}
	
	public Set<Office> getOfficesWithAllowedIp(String ip){

		Preconditions.checkNotNull(ip);

		return FluentIterable.from(confGeneralManager.containsValue(
				Parameter.ADDRESSES_ALLOWED.description, ip)).transform(
						new Function<ConfGeneral, Office>() {

							@Override
							public Office apply(ConfGeneral input) {
								return input.office;
							}
						}).toSet();
	}
}
