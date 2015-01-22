package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import controllers.Security;
import dao.OfficeDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.UserDao;
import dao.UsersRolesOfficesDao;

 
 
@Entity
@Audited
@Table(name = "office")
public class Office extends BaseModel{
 
	private static final long serialVersionUID = -8689432709728656660L;

	@Required
	@Column(name = "name")
    public String name;
    
    @Column(name = "contraction")
    public String contraction;
    
    @Column(name = "address")
    public String address = "";
    
    @Required
    @Column(name = "code")
    public Integer code = 0;
    
    @Column(name="joining_date")
    //  @Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
    public LocalDate joiningDate;
    
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<Office> subOffices = new ArrayList<Office>();
    
    @ManyToOne
    @JoinColumn(name="office_id")
    public Office office;
     
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<Person> persons = new ArrayList<Person>();
    
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<ConfGeneral> confGeneral = new ArrayList<ConfGeneral>();
    
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<ConfYear> confYear = new ArrayList<ConfYear>();
    
    @NotAudited
    @OneToMany(mappedBy="office", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();
    
    @NotAudited
	@OneToMany(mappedBy="office", fetch=FetchType.LAZY)
	public List<WorkingTimeType> workingTimeType = new ArrayList<WorkingTimeType>();
    
    @NotAudited
	@OneToMany(mappedBy="office", fetch=FetchType.LAZY)
	public List<TotalOvertime> totalOvertimes = new ArrayList<TotalOvertime>();
    
    
    @Transient
    private Boolean isEditable = null;
    
    public String getName() {
    	return this.name;
    }
    
    @Override
    public String getLabel() {
    	return this.name;
    }
    
    /**
     * Ritorna il numero di dipendenti attivi registrati nella sede e nelle sottosedi
     * @return
     */
    public List<Person> getActivePersons() {
    
    	//List<Office> officeList = this.getSubOfficeTree();
    	LocalDate date = new LocalDate();
    	
    	
    	
    	//List<Person> activePerson = Person.getActivePersonsSpeedyInPeriod(date, date,
    	//		officeList, false);
    	
    	List<Person> activePerson = PersonDao.list(Optional.<String>absent(), 
    			Sets.newHashSet(this.getSubOfficeTree()), false, date, date, true).list();
    	    			
    	return activePerson;
    	
    }
    
    /**
     * Ritorna i nomi (user.username) dei lettori badge abilitati all'ufficio.
     * @return
     */
    public List<String> getActiveBadgeReaders() {
    	
    	List<String> bgList = Lists.newArrayList();
    	
    	List<Office> officeList = this.getSubOfficeTree();
    	
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
     * Ritorna la lista di tutte le sedi gerarchicamente sotto a Office
     * @return
     */
    private List<Office> getSubOfficeTree() {
    	
    	List<Office> officeToCompute = new ArrayList<Office>();
    	List<Office> officeComputed = new ArrayList<Office>();
    	
    	officeToCompute.add(this);
    	while(officeToCompute.size() != 0) {
    		
    		Office office = officeToCompute.get(0);
    		officeToCompute.remove(office);
    		
    		for(Office remoteOffice : office.subOffices) {
    			
    			//Office temp = Office.find("byId", remoteOffice.id).first();
    			//officeToCompute.add(temp);
    			officeToCompute.add((Office)remoteOffice);
    		}
    		
    		officeComputed.add(office);
    	}
    	return officeComputed;
    }

    
    
    /**
     * La lista di tutte le Aree definite nel db ePAS
     * @return
     */
	public static List<Office> getAllAreas() {
		
		List<Office> areaList = OfficeDao.getAreas();
		//List<Office> areaList = Office.find("select o from Office o where o.office is null").fetch();
		return areaList;
		
	}
	
	/**
	 * Se this è una Area ritorna la lista degli istituti definiti per quell'area
	 * @return la lista degli istituti associati all'area, null nel caso this non 
	 * sia un'area
	 */
	public List<Office> getInstitutes() {
		
		if(!this.isArea())
			return null;
		
		return this.subOffices;
	}
	
	/**
	 * Se this è un Istituto ritorna la lista delle sedi definite per quell'istituto
	 * @return la lista delle sedi associate all'istituto, null nel caso this non 
	 * sia un istituto
	 */
	public List<Office> getSeats() {
		
		if(!this.isInstitute())
			return null;
		
		return this.subOffices;
	}
	
	/**
	 * Area livello 0
	 * @return true se this è una Area, false altrimenti
	 */
	public boolean isArea() {
		
		if(this.office != null) 
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
    	
    	if(this.office.office != null)
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
	 * Ritorna l'istituto padre se this è una sede
	 * @return 
	 */
	public Office getSuperInstitute() {
		
		if(!isSeat())
			return null;
		return this.office;
	}
	
	/**
	 * Ritorna l'area padre se thi è un istituto o una sede
	 * @return
	 */
	public Office getSuperArea() {
		
		if(isSeat())
			return this.office.office;
		
		if(isInstitute())
			return this.office;
		
		return null;
	}
	
	/***
	 * 
	 * 
	 * PARTE SULLA SICUREZZA - METODI DA SPOSTARE
	 * 
	 * 
	 * 
	 */
	
	/**
	 * 
	 * @return
	 */
	public boolean isPrintable() {
		
		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		//Role roleAdmin = Role.find("byName", Role.PERSONNEL_ADMIN).first();
		Role roleAdminMini = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);
		//Role roleAdminMini = Role.find("byName", Role.PERSONNEL_ADMIN_MINI).first();
		
		return isRightPermittedOnOfficeTree(roleAdmin) || isRightPermittedOnOfficeTree(roleAdminMini);

	}
	
	/**
	 * @return
	 */
	@Transient
	public boolean getIsEditable() {
		
		if(isEditable != null)
			return this.isEditable;
		
		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		//Role roleAdmin = Role.find("byName", Role.PERSONNEL_ADMIN).first();
		this.isEditable = isRightPermittedOnOfficeTree(roleAdmin);
		
		return this.isEditable;
	}
	
	
	public List<Person> getPersonnelAdmin() {
		
		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		//Role roleAdmin = Role.find("byName", Role.PERSONNEL_ADMIN).first();
		List<Person> personList = Lists.newArrayList();
		for(UsersRolesOffices uro : this.usersRolesOffices) {
			
			if(uro.office.id.equals(this.id) && uro.role.id.equals(roleAdmin.id) 
					&& uro.user.person != null)
				personList.add(uro.user.person);
		}
		return personList;
	}
	
	public List<Person> getPersonnelAdminMini() {
		
		Role roleAdminMini = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);
		//Role roleAdminMini = Role.find("byName", Role.PERSONNEL_ADMIN_MINI).first();
		List<Person> personList = Lists.newArrayList();
		for(UsersRolesOffices uro : this.usersRolesOffices) {
			
			if(uro.office.id.equals(this.id) && uro.role.id.equals(roleAdminMini.id) 
					&& uro.user.person != null)
				personList.add(uro.user.person);
		}
		return personList;
	}

	
	/**
	 * 
	 * @param permission
	 * @return true se permission è presente in almeno un office del sottoalbero, radice compresa, 
	 * false altrimenti
	 */
	private boolean isRightPermittedOnOfficeTree(Role role) {
		
		if(checkUserRoleOffice(Security.getUser().get(), role, this))
			return true;
		
		for(Office subOff : this.subOffices) {
			
			if(subOff.isRightPermittedOnOfficeTree(role))
				return true;
		}
		
		return false;
	}
		
	/**
	 * 	Check del Permesso
	 */
	private static boolean checkUserRoleOffice(User user, Role role, Office office) {
		
		/*
		UsersRolesOffices uro1 = UsersRolesOffices.find(
				"Select uro from UsersRolesOffices uro, Role role, Permission permission where ? in uro.role.permissions and uro.office = ? and uro.user = ?",
				permission, office, user).first();
		*/
		Optional<UsersRolesOffices> uro = UsersRolesOfficesDao.getUsersRolesOffices(user, role, office);
//		UsersRolesOffices uro = UsersRolesOffices.find(
//				"Select uro from UsersRolesOffices uro where uro.office = ? and uro.user = ? and uro.role = ?",
//				office, user, role).first();
		
		/*
		for(UsersRolesOffices uro : uroList){
			for(Permission p : uro.role.permissions) {
				if(p.id.equals(permission.id))
					return true;
			}
		}
		return false;*/
				
		if(!uro.isPresent())
			return false;
		else
			return true;
		
		
	}
	
	/**
	 * 
	 */
	public void setPermissionAfterCreation() {
		
		User userLogged = Security.getUser().get();
		User admin = UserDao.getUserByUsernameAndPassword("admin", Optional.<String>absent());
		//User admin = User.find("byUsername", "admin").first();
		
		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		//Role roleAdmin = Role.find("byName", Role.PERSONNEL_ADMIN).first();
		Role roleAdminMini = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);
		//Role roleAdminMini = Role.find("byName", Role.PERSONNEL_ADMIN_MINI).first();
		
		Office.setUro(admin, this, roleAdmin);
		Office.setUro(userLogged, this, roleAdmin);
		
		List<Office> officeList = Lists.newArrayList();
		if(isInstitute()) {
			officeList.add(getSuperArea());
		}
		if(isSeat()) {
			officeList.add(getSuperArea());
			officeList.add(getSuperInstitute());
		}
			
		for(Office superOffice : officeList) {
			
			//Attribuire roleAdminMini a coloro che hanno roleAdminMini su il super office
			for(User user : Office.getUserByOfficeAndRole(superOffice, roleAdminMini)) {
				
				Office.setUroIfImprove(user, this, roleAdminMini, true);
			}


			//Attribuire roleAdmin a coloro che hanno roleAdmin su area il super office
			for(User user : Office.getUserByOfficeAndRole(superOffice, roleAdmin)) {
				
				Office.setUroIfImprove(user, this, roleAdmin, true);
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
	public static Boolean setUroIfImprove(User user, Office office, Role role, boolean ifImprove) {
		
		UsersRolesOffices uro = Office.getUro(user, office);
		
		if(uro == null || !ifImprove) {
			
			Office.setUro(user, office, role);
			return true;
		}
		
		if(ifImprove) {
			
			/* implementare la logica di confronto fra ruolo */
			Role previous = uro.role;
			
			if(previous.name.equals(Role.PERSONNEL_ADMIN_MINI)) {
				
				Office.setUro(user, office, role);
				return true;
			}
			
		}
		
		return false;
		 
	}
	
	
	
	/**
	 * Ritorna il ruolo attualmente attivo per <user,office>
	 * @param user
	 * @param office
	 */
	public static UsersRolesOffices getUro(User user, Office office) {
		
		Optional<UsersRolesOffices> uro = UsersRolesOfficesDao.getUsersRolesOfficesByUserAndOffice(user, office);
		if(uro.isPresent())
//		UsersRolesOffices uro = UsersRolesOffices.find("select uro from UsersRolesOffices uro "
//				+ "where uro.user = ? and uro.office = ? ", user, office).first();
		
			return uro.get();
		else
			return null;
	}
	
	/**
	 * 
	 * @param user
	 * @param office
	 * @param role
	 */
	public static void setUro(User user, Office office, Role role){
		
		UsersRolesOffices newUro = null;
		Optional<UsersRolesOffices> uro = UsersRolesOfficesDao.getUsersRolesOfficesByUserAndOffice(user, office);
//		UsersRolesOffices uro = UsersRolesOffices.find("select uro from UsersRolesOffices uro "
//				+ "where uro.user = ? and uro.office = ? ", user, office).first();
		
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
	
	/**
	 * Ritorna la lista degli utenti che hanno ruolo role nell'ufficio office
	 * @param office
	 * @param role
	 * @return
	 */
	public static List<User> getUserByOfficeAndRole(Office office, Role role) {
		
		List<User> userList = Lists.newArrayList();
		
		for(UsersRolesOffices uro : office.usersRolesOffices) {
			
			if(uro.role.id.equals(role.id)) {
				
				userList.add(uro.user);
			}
		}
		return userList;
	}
	
	@Transient
	public List<WorkingTimeType> getEnabledWorkingTimeType() {
		
		List<WorkingTimeType> enabledWttList = new ArrayList<WorkingTimeType>();
		for(WorkingTimeType wtt: this.workingTimeType) {
			
			if(wtt.disabled == false)
				enabledWttList.add(wtt);
		}
		return enabledWttList;
	}
	


}
