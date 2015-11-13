package models;

import com.google.common.collect.Lists;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@Entity
@Audited
@Table(name="roles")
public class Role extends BaseModel{

	private static final long serialVersionUID = 6717202212924325368L;

	public final static String DEVELOPER = "developer";
	public final static String ADMIN = "admin";
	public final static String PERSONNEL_ADMIN = "personnelAdmin";
	public final static String PERSONNEL_ADMIN_MINI = "personnelAdminMini";
	public final static String EMPLOYEE = "employee";
	public final static String BADGE_READER = "badgeReader";
	public final static String REST_CLIENT = "restClient";
	public final static String TECNICAL_ADMIN = "tecnicalAdmin";
	public final static String SHIFT_MANAGER = "shiftManager";
	public final static String REPERIBILITY_MANAGER = "reperibilityManager";

	public String name;

	@NotAudited
	@OneToMany(mappedBy="role", cascade = {CascadeType.REMOVE}, orphanRemoval=true)
	public List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();
	
	@Override
	public String toString() {
		if( name.equals(Role.PERSONNEL_ADMIN))
			return "Amministratore Personale";
		if( name.equals(Role.PERSONNEL_ADMIN_MINI))
			return "Amministratore Personale Sola lettura";
		if( name.equals(Role.TECNICAL_ADMIN))
			return "Amministratore Tecnico";
		return this.name;
	}

}
