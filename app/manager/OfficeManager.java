package manager;

import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Inject;

import controllers.Security;
import dao.RoleDao;
import dao.UserDao;
import dao.UsersRolesOfficesDao;

public class OfficeManager {

	@Inject
	public OfficeManager(UsersRolesOfficesDao usersRolesOfficesDao,
			RoleDao roleDao,UserDao userDao,ConfYearManager confYearManager
			,ConfGeneralManager confGeneralManager) {
		this.usersRolesOfficesDao = usersRolesOfficesDao;
		this.roleDao = roleDao;
		this.userDao = userDao;
		this.confGeneralManager = confGeneralManager;
		this.confYearManager = confYearManager;
	}
	
	private final UserDao userDao;
	private final UsersRolesOfficesDao usersRolesOfficesDao;
	private final RoleDao roleDao;
	private final ConfGeneralManager confGeneralManager;
	private final ConfYearManager confYearManager;

	/**
	 * 
	 * @param permission
	 * @return true se permission è presente in almeno un office del sottoalbero, radice compresa, 
	 * false altrimenti
	 */
	public boolean isRightPermittedOnOfficeTree(Office office, Role role) {

		if(usersRolesOfficesDao.getUsersRolesOffices(Security.getUser().get(), role, office).isPresent())
			return true;

		for(Office subOff : office.subOffices) {

			if(isRightPermittedOnOfficeTree(subOff, role))
				return true;
		}

		return false;
	}

	/**
	 * Assegna i diritti agli amministratori. Da chiamare successivamente alla creazione.
	 * @param office
	 */
	public void setSystemUserPermission(Office office) {

		User admin = userDao.getUserByUsernameAndPassword( Role.ADMIN, Optional.<String>absent());
		User developer = userDao.getUserByUsernameAndPassword( Role.DEVELOPER, Optional.<String>absent());

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
	
	public void saveOffice(Office office){
		
		Preconditions.checkNotNull(office);

//		Verifico se è un nuovo inserimento o è un aggiornamento di uno esistente
		final boolean newOffice = !office.isPersistent();

		office.save();

//		Verifico se si tratta dell'inserimento di una nuova sede
		if(newOffice && office.office != null && office.office.office != null){
			confGeneralManager.buildOfficeConfGeneral(office, false);

			confYearManager.buildOfficeConfYear(office, LocalDate.now().getYear() - 1, false);
			confYearManager.buildOfficeConfYear(office, LocalDate.now().getYear(), false);
		}

		setSystemUserPermission(office);
	}
}
