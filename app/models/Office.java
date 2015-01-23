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

import manager.OfficeManager;
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
    @Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
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
    
       	LocalDate date = new LocalDate();
   
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
		return areaList;
		
	}
	
	/**
	 * Se this è una Area ritorna la lista degli istituti definiti per quell'area
	 * @return la lista degli istituti associati all'area, null nel caso this non 
	 * sia un'area
	 */
	public List<Office> getInstitutes() {
		
		if(!OfficeManager.isArea(this))
			return null;
		
		return this.subOffices;
	}
	
	/**
	 * Se this è un Istituto ritorna la lista delle sedi definite per quell'istituto
	 * @return la lista delle sedi associate all'istituto, null nel caso this non 
	 * sia un istituto
	 */
	public List<Office> getSeats() {
		
		if(!OfficeManager.isInstitute(this))
			return null;
		
		return this.subOffices;
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
		Role roleAdminMini = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);
				
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
		this.isEditable = isRightPermittedOnOfficeTree(roleAdmin);
		
		return this.isEditable;
	}
	
	
	public List<Person> getPersonnelAdmin() {
		
		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
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
		
		Optional<UsersRolesOffices> uro = UsersRolesOfficesDao.getUsersRolesOffices(user, role, office);
				
		if(!uro.isPresent())
			return false;
		else
			return true;		
		
	}
	
	/**
	 * Ritorna il ruolo attualmente attivo per <user,office>
	 * @param user
	 * @param office
	 */
	public static UsersRolesOffices getUro(User user, Office office) {
		
		Optional<UsersRolesOffices> uro = UsersRolesOfficesDao.getUsersRolesOfficesByUserAndOffice(user, office);
		if(uro.isPresent())
	
			return uro.get();
		else
			return null;
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
