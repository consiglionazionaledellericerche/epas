package manager;

import java.util.List;

import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import controllers.Security;
import dao.OfficeDao;
import dao.RoleDao;
import dao.UserDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

public class OfficeManager {

	@Inject
	public OfficeDao officeDao;
	
	@Inject
	public UsersRolesOfficesDao usersRolesOfficesDao;
	
	@Inject
	public UserDao userDao;
	
	@Inject
	public IWrapperFactory wrapperFactory;
	
	/**
	 * 
	 * @param area
	 * @param name
	 * @param contraction
	 */
//	public void saveInstitute(Office office, Office area, String name, String contraction){
//
//		office.name = name;
//		office.contraction = contraction;
//		office.office = area;
//		office.save();
//	}	

	/**
	 * 
	 * @param name
	 * @param address
	 * @param code
	 * @param date
	 * @param institute
	 */
//	public void saveSeat(Office office, String name, String address, String code, String date, Office institute){
//
//		office.name = name;
//		office.address = address;
//		office.code = getInteger(code);
//		office.joiningDate = getLocalDate(date);
//		office.office = institute;
//		office.save();
//	}

	/**
	 * 
	 * @param office
	 * @param name
	 * @param address
	 * @param code
	 * @param date
	 */
//	public void updateSeat(Office office, String name, String address, String code, String date){
//		office.name = name;
//		office.address = address;
//		office.code = getInteger(code);
//		office.joiningDate = getLocalDate(date);
//
//		office.save();
//
//	}
	
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
	public void setPermissionAfterCreation(Office office) {

		User userLogged = Security.getUser().get();
		User admin = UserDao.getUserByUsernameAndPassword("admin", Optional.<String>absent());

		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		Role roleAdminMini = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);

		setUro(admin, office, roleAdmin);
		setUro(userLogged, office, roleAdmin);

		List<Office> officeList = Lists.newArrayList();
		
		IWrapperOffice wOffice = wrapperFactory.create(office);
		
		if(wOffice.isInstitute()) {
			officeList.add(officeDao.getSuperArea(office));
		}
		if(wOffice.isSeat()) {
			officeList.add(officeDao.getSuperArea(office));
			officeList.add(officeDao.getSuperInstitute(office));
		}

		for(Office superOffice : officeList) {

			//Attribuire roleAdminMini a coloro che hanno roleAdminMini su il super office
			for(User user : userDao.getUserByOfficeAndRole(superOffice, roleAdminMini)) {

				setUroIfImprove(user, office, roleAdminMini, true);
			}

			//Attribuire roleAdmin a coloro che hanno roleAdmin su area il super office
			for(User user : userDao.getUserByOfficeAndRole(superOffice, roleAdmin)) {

				setUroIfImprove(user, office, roleAdmin, true);
			}

		}

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

		UsersRolesOffices newUro = null;
		Optional<UsersRolesOffices> uro = usersRolesOfficesDao.getUsersRolesOfficesByUserAndOffice(user, office);

		if(!uro.isPresent()) {

			newUro = new UsersRolesOffices();
			newUro.user = user;
			newUro.office = office;
			newUro.role = role;
			newUro.save();
		}
		else{
			newUro = uro.get();
			newUro.role = role;
			newUro.save();
		}		

	}
	
//	/**
//	 * @param office
//	 * @return Restituisce true se uno dei parametri nome,sigla o codice sono già assegnati ad un altro ufficio
//	 */
//	public boolean isDuplicated(Office office){
//		
//		return officeDao.getOfficeByName(office.name).isPresent() ||  
//				officeDao.getOfficeByContraction(office.contraction).isPresent() ||
//				 office.code!=0 && officeDao.getOfficeByCode(office.code).isPresent();
//	}

}
