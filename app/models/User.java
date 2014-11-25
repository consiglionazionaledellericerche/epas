package models;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

@Entity
@Audited
@Table(name="users")
public class User extends BaseModel{

	private static final long serialVersionUID = -6039180733038072891L;

	public String username;

	public String password;

	@NotAudited
	@OneToOne(mappedBy="user", fetch=FetchType.EAGER, cascade = {CascadeType.REMOVE}, orphanRemoval=true)
	public Person person;
	
	@NotAudited
	@OneToMany(mappedBy="user", fetch=FetchType.EAGER, cascade = {CascadeType.REMOVE})
	public List<UsersRolesOffices> usersRolesOffices = new ArrayList<UsersRolesOffices>();
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="expire_recovery_token")
	public LocalDate expireRecoveryToken;
	
	@Column(name="recovery_token")
	public String recoveryToken;
	
	public boolean isAdmin()
	{
		if(this.username.equals("admin"))
			return true;
		else
			return false;
	}
	
	public List<Permission> getAllPermissions() {
		List<Permission> permissions = new ArrayList<Permission>();
		
		if(this.person != null){
			UsersRolesOffices uro = UsersRolesOffices.find("Select upo from UsersRolesOffices uro where " +
					"uro.user = ? and uro.office = ?", this, this.person.office).first();
			for(Permission p : uro.role.permissions){
				permissions.add(p);
			}
			
		}
		
		//TODO admin 
		/*
		else{
			Office office = Office.find("Select off from Office off where off.joiningDate is null").first();
			List<UsersPermissionsOffices> upoList = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where " +
					"upo.user = ? and upo.office = ?", this, office).fetch();
			for(UsersPermissionsOffices upo : upoList){
				permissions.add(upo.permission);
			}
		}
		*/
		return permissions;
	}
	
	public List<Office> getOfficeAllowed() {
		
		List<Office> officeList = new ArrayList<Office>();
		for(UsersRolesOffices uro : this.usersRolesOffices){
			if(uro.office.isSeat())
				officeList.add(uro.office);
		}
		//TODO riscrivere col nuovo concetto di ruoli e permessi e funzionale al tipo di ruolo che si cerca
//		if (this.person != null) {
//			officeList.add(this.person.office);
//		}
//		else {
//			
//			officeList = Office.findAll(); 
//		}
		return officeList;
			
		//return Office.find("select distinct o from Office o join "
		//		+ "o.userPermissionOffices as upo where upo.user = ?",this).fetch();
		
	}
	
	
}
