package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.db.jpa.Model;
import controllers.Security;

@Entity
@Audited
@Table(name="users")
public class User extends Model{

	
	public String username;

	public String password;

	@NotAudited
	@OneToOne(mappedBy="user", fetch=FetchType.EAGER, cascade = {CascadeType.REMOVE}, orphanRemoval=true)
	public Person person;
	
//	@ManyToMany(cascade = {CascadeType.REFRESH, CascadeType.REMOVE}, fetch = FetchType.LAZY)
//	public List<Permission> permissions;
	@NotAudited
	@OneToMany(mappedBy="user", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<UsersPermissionsOffices> userPermissionOffices = new ArrayList<UsersPermissionsOffices>();
	
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
	
	public List<UsersPermissionsOffices> getAllPermissions(){
		List<UsersPermissionsOffices> permissions = null;
		if(this.person != null){
			permissions = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where " +
					"upo.user = ? and upo.office = ?", this, this.person.office).fetch();
		}
		else{
			Office office = Office.find("Select o from Office o where o.joiningDate is null").first();
			permissions = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where " +
					"upo.user = ? and upo.office = ?", this, office).fetch();
		}
		
		return permissions;
	}
	
//	public Set<UsersPermissionsOffices> getAllPermissions(){
//		Set<UsersPermissionsOffices> setPermissions = new HashSet<UsersPermissionsOffices>();
//		List<UsersPermissionsOffices> permissions = UsersPermissionsOffices.find("Select upo from UsersPermissionsOffices upo where " +
//				"upo.user = ? and upo.office = ?", this, this.person.office).fetch();
//		setPermissions.addAll(permissions);
//
//		return setPermissions;
//	}
	
	public boolean isViewPersonAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.VIEW_PERSON_LIST))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdatePersonAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.INSERT_AND_UPDATE_PERSON))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateAbsenceAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.INSERT_AND_UPDATE_ABSENCE))
				return true;
		}
		return false;
	}

	public boolean isDeletePersonAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.DELETE_PERSON))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateWorkinTimeAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.INSERT_AND_UPDATE_WORKINGTIME))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateStampingAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.INSERT_AND_UPDATE_STAMPING))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdatePasswordAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.INSERT_AND_UPDATE_PASSWORD))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateConfigurationAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.INSERT_AND_UPDATE_CONFIGURATION))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateAdministratorAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.INSERT_AND_UPDATE_ADMINISTRATOR))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateOfficesAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.INSERT_AND_UPDATE_OFFICES))
				return true;
		}
		return false;
	}
	
	public boolean isInsertAndUpdateCompetenceAndOvertimeAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.INSERT_AND_UPDATE_COMPETENCES))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateVacationsAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.INSERT_AND_UPDATE_VACATIONS))
				return true;
		}
		return false;
	}
	
	public boolean isUploadSituationAvailable(){
		for(UsersPermissionsOffices p : this.userPermissionOffices){
			if(p.permission.description.equals(Security.UPLOAD_SITUATION))
				return true;
		}
		return false;
	}

	
	
}
