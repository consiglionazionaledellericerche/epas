package models;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
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
	
	@ManyToMany(cascade = {CascadeType.REFRESH, CascadeType.REMOVE}, fetch = FetchType.LAZY)
	public List<Permission> permissions;
	
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
	
	
	public Set<Permission> getAllPermissions(){
		Set<Permission> setPermissions = new HashSet<Permission>();
		setPermissions.addAll(permissions);

		return setPermissions;
	}
	
	public boolean isViewPersonAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.VIEW_PERSON_LIST))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdatePersonAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_PERSON))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateAbsenceAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_ABSENCE))
				return true;
		}
		return false;
	}

	public boolean isDeletePersonAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.DELETE_PERSON))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateWorkinTimeAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_WORKINGTIME))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateStampingAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_STAMPING))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdatePasswordAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_PASSWORD))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateConfigurationAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_CONFIGURATION))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateAdministratorAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_ADMINISTRATOR))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateOfficesAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_OFFICES))
				return true;
		}
		return false;
	}
	
	public boolean isInsertAndUpdateCompetenceAndOvertimeAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_COMPETENCES))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateVacationsAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_VACATIONS))
				return true;
		}
		return false;
	}
	
	public boolean isUploadSituationAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.UPLOAD_SITUATION))
				return true;
		}
		return false;
	}

	
	
}
