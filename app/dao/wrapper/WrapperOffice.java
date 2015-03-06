package dao.wrapper;

import java.util.List;

import manager.OfficeManager;
import models.Office;
import models.Person;
import models.Role;
import models.UsersRolesOffices;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.OfficeDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.wrapper.function.WrapperModelFunctionFactory;

/**
 * @author alessandro
 *
 */
public class WrapperOffice implements IWrapperOffice {

	private final Office value;
	private final OfficeManager officeManager;
	private final OfficeDao officeDao;
	private final WrapperModelFunctionFactory wrapperFunctionFactory;
	
	private Boolean isEditable = null;
		
	@Inject
	WrapperOffice(@Assisted Office office, OfficeManager officeManager,
			WrapperModelFunctionFactory wrapperFuncionFactory, OfficeDao officeDao) {
		value = office;
		this.officeManager = officeManager;
		this.wrapperFunctionFactory = wrapperFuncionFactory;
		this.officeDao = officeDao;
	}

	@Override
	public Office getValue() {
		return value;
	}
	
	/**
	 * Area livello 0
	 * @return true se this è una Area, false altrimenti
	 */
	public boolean isArea() {

		if(this.value.office != null) 
			return false;

		return true;
	}

	/**
	 * Istituto livello 1
	 * @return true se this è un Istituto, false altrimenti
	 */
	public boolean isInstitute() {

		if(this.isArea())
			return false;

		if(this.value.office.office != null)
			return false;

		return true;
	}

	/**
	 * Sede livello 2
	 * @return
	 */
	public boolean isSeat() {

		if(this.isArea())
			return false;

		if(this.isInstitute())
			return false;

		return true;

	}
	
  	/**
	 * Verifica la visibilità dell'office rispetto ai ruoli dell'amministratore loggato.
	 * (Almeno un office del sottoalbero radice compresa deve essere amministrato dall'user
	 * loggato)
	 * @return
	 */
	public boolean isPrintable() {
		
		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		Role roleAdminMini = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);
				
		return officeManager.isRightPermittedOnOfficeTree(value, roleAdmin) 
				|| officeManager.isRightPermittedOnOfficeTree(value, roleAdminMini);

	}
	
	/**
	 *	Verifica il ruolo di personnel_Admin dell'user loggato. 
	 *  Gestisce una variabile LAZY.
	 * @return
	 */
	public boolean isEditable() {
		
		if(isEditable != null)
			return this.isEditable;
		
		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		this.isEditable = officeManager.isRightPermittedOnOfficeTree(this.value, roleAdmin);
		
		return this.isEditable;
	}
	
	/**
     * Ritorna il numero di dipendenti attivi registrati nella sede e nelle sottosedi
     * @return
     */
    public List<Person> activePersonsInOffice() {
    
       	LocalDate date = new LocalDate();
   
    	List<Person> activePerson = PersonDao.list(Optional.<String>absent(), 
    			Sets.newHashSet(officeDao.getSubOfficeTree(value)), false, date, date, true).list();
    	    			
    	return activePerson;
    }
    
    /**
  	 * Se this è una Area ritorna la lista degli istituti definiti per quell'area
  	 * @return la lista degli istituti associati all'area, null nel caso this non 
  	 * sia un'area
  	 */
  	public List<IWrapperOffice> getWrapperInstitutes() {
  		
  		if(!this.isArea())
  			return null;
  		
  		List<IWrapperOffice> wrapperSubOffice = FluentIterable
  				.from(this.value.subOffices).transform(wrapperFunctionFactory.office()).toList();
  		return wrapperSubOffice;
  	}
  	
  	
  	/**
  	 * Se this è un Istituto ritorna la lista delle sedi definite per quell'istituto
  	 * @return la lista delle sedi associate all'istituto, null nel caso this non 
  	 * sia un istituto
  	 */
  	public List<IWrapperOffice> getWrapperSeats() {
  		
  		if(!this.isInstitute())
  			return null;
  		
  		List<IWrapperOffice> wrapperSubOffice = FluentIterable
  				.from(this.value.subOffices).transform(wrapperFunctionFactory.office()).toList();
  		return wrapperSubOffice;
  	}
  	
  	/**
     * Ritorna i nomi (user.username) dei lettori badge abilitati all'ufficio.
     * @return
     */
    public List<String> getActiveBadgeReaders() {
    	
    	List<String> bgList = Lists.newArrayList();
    	
    	List<Office> officeList = officeDao.getSubOfficeTree(this.value);
    	
    	for(Office office : officeList) {
    		
    		for(UsersRolesOffices uro : office.usersRolesOffices) {
    			
    			if( uro.role.name.equals(Role.BADGE_READER) && !bgList.contains(uro.user.username) ) {
    				
    				bgList.add(uro.user.username);
    			}
    		}
    	}
    	
    	return bgList;
    }
    
    /**
     * Gli amministratori dell'office.
     * @return
     */
    public List<Person> getPersonnelAdmin() {
		
		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		List<Person> personList = Lists.newArrayList();
		for(UsersRolesOffices uro : this.value.usersRolesOffices) {
			
			if(uro.office.id.equals(this.value.id) && uro.role.id.equals(roleAdmin.id) 
					&& uro.user.person != null)
				personList.add(uro.user.person);
		}
		return personList;
	}
	
    /**
     * I mini amministratori dell'office.
     * @return
     */
	public List<Person> getPersonnelAdminMini() {
		
		Role roleAdminMini = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);
		List<Person> personList = Lists.newArrayList();
		for(UsersRolesOffices uro : this.value.usersRolesOffices) {
			
			if(uro.office.id.equals(this.value.id) && uro.role.id.equals(roleAdminMini.id) 
					&& uro.user.person != null)
				personList.add(uro.user.person);
		}
		return personList;
	}
  	

    

}
