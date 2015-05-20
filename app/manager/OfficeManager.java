package manager;

import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import controllers.Security;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;

public class OfficeManager {

	@Inject
	public OfficeManager(UsersRolesOfficesDao usersRolesOfficesDao,
			RoleDao roleDao) {
		this.usersRolesOfficesDao = usersRolesOfficesDao;
		this.roleDao = roleDao;
	}

	private final UsersRolesOfficesDao usersRolesOfficesDao;
	private final RoleDao roleDao;

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
		
		User admin = User.find("byUsername", Role.ADMIN).first();
		User developer = User.find("byUsername", Role.DEVELOPER).first();

		Role roleAdmin = roleDao.getRoleByName(Role.ADMIN);
		Role roleDeveloper = roleDao.getRoleByName(Role.DEVELOPER);

		setUro(admin, office, roleAdmin);
		setUro(developer, office, roleDeveloper);

	}

	/**
	 * Setta il ruolo per la tripla <user,office,role>. Se non esiste viene creato.
	 * Se ifImprove è false il precedente ruolo viene sovrascritto. Se ifImprove è true 
	 * il ruolo viene sovrascritto solo se assegna maggiori diritti rispetto al precedente. 
	 * @param user
	 * @param office
	 * @param role
	 * @param ifImprove
	 * @return true se il ruolo è stato assegnato, false se il ruolo non è stato assegnato (perchè peggiorativo)
	 */
	public Boolean setUroIfImprove(User user, Office office, Role role, boolean ifImprove) {

		Optional<UsersRolesOffices> uro = usersRolesOfficesDao.getUsersRolesOfficesByUserAndOffice(user, office);

		if( !uro.isPresent() || !ifImprove ) {

			setUro(user, office, role);
			return true;
		}

		if(ifImprove) {

			/* implementare la logica di confronto fra ruolo */
			Role previous = uro.get().role;

			if(previous.name.equals(Role.PERSONNEL_ADMIN_MINI)) {

				setUro(user, office, role);
				return true;
			}

		}		
		return false;		 
	}

	/**
	 * 
	 * @param user
	 * @param office
	 * @param role
	 */
	public void setUro(User user, Office office, Role role){

		Optional<UsersRolesOffices> uro = usersRolesOfficesDao.getUsersRolesOffices(user,role, office);

		if(!uro.isPresent()) {

			UsersRolesOffices newUro = new UsersRolesOffices();
			newUro.user = user;
			newUro.office = office;
			newUro.role = role;
			newUro.save();
		}

	}
}
