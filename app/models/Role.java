package models;

import java.security.Permissions;
import java.util.ArrayList;
import java.util.List;

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

@Entity
@Audited
@Table(name="roles")
public class Role extends Model{

	public final static String PERSONNEL_ADMIN = "personnelAdmin";
	public final static String PERSONNEL_ADMIN_MINI = "personnelAdminMini";
	
	
	public String name;
	
	@ManyToMany(cascade = {CascadeType.REFRESH, CascadeType.REMOVE}, fetch = FetchType.LAZY)
	public List<Permission> permissions;
	
    @NotAudited
    @OneToMany(mappedBy="role", fetch = FetchType.EAGER, cascade = {CascadeType.REMOVE})
    public List<UsersRolesOffices> usersRolesOffices = new ArrayList<UsersRolesOffices>();
	
		
}
