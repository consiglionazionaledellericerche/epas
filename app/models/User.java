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

import com.google.common.base.Objects;

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
	
	//  @Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="expire_recovery_token")
	public LocalDate expireRecoveryToken;
	
	@Column(name="recovery_token")
	public String recoveryToken;
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("id", this.id)
				.add("user", this.username)
				.toString();
	}	
	
}
