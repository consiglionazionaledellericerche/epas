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
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.data.validation.Unique;

import com.google.common.base.MoreObjects;

@Entity
@Audited
@Table(name="users", uniqueConstraints={@UniqueConstraint(columnNames={"username"})})
public class User extends BaseModel{

	private static final long serialVersionUID = -6039180733038072891L;
	
	@Unique
	@NotNull
	@Column(nullable=false)
	@Required
	public String username;

	public String password;

	@NotAudited
	@OneToOne(mappedBy="user", fetch=FetchType.LAZY)
	public Person person;
	
	@NotAudited
	@OneToOne(mappedBy="user", fetch=FetchType.LAZY)
	public BadgeReader badgeReader;
	
	@NotAudited
	@OneToMany(mappedBy="user", cascade = {CascadeType.REMOVE})
	public List<UsersRolesOffices> usersRolesOffices = new ArrayList<UsersRolesOffices>();


	@Column(name="expire_recovery_token")
	public LocalDate expireRecoveryToken;

	@Column(name="recovery_token")
	public String recoveryToken;

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id", this.id)
				.add("user", this.username)
				.toString();
	}
	
	/**
	 * Se l'user è un account di sistema. 
	 * TODO: definire la logica più dettagliata se necessario.
	 * @return
	 */
	public boolean isSystemUser() {
		if (person == null) {
			return true;
		}
		return false;
	}

}
