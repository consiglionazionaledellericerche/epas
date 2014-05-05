package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.NotAudited;

import play.db.jpa.Model;

@Entity
@Table(name="users_permissions_offices")
public class UsersPermissionsOffices extends Model{

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
	@JoinColumn(name="permission_id")
	public Permission permission;
}
