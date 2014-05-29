package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.NotAudited;



@Entity
@Table(name="users_roles_offices")
public class UsersRolesOffices extends BaseModel{

	@NotAudited
	@ManyToOne(fetch=FetchType.EAGER)
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
}
