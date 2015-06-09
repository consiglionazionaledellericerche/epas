package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.NotAudited;

import com.google.common.base.MoreObjects;



@Entity
@Table(name="users_roles_offices")
public class UsersRolesOffices extends BaseModel{

	private static final long serialVersionUID = -1403683534643592790L;

	@NotAudited
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="user_id")
	public User user;

	@NotAudited
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="office_id")
	public Office office;

	@NotAudited
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="role_id")
	public Role role;

	@Override
	public String toString() {

		return MoreObjects.toStringHelper(this)
				.add("id", id)
				.add("user", user.username)
				.add("role", role.name)
				.add("office", office.name)
				.toString();
	}
}
