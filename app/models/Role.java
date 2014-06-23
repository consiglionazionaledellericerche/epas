package models;

import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;



@Entity
@Audited
@Table(name="roles")
public class Role extends BaseModel{

	public final static String PERSONNEL_ADMIN = "personnelAdmin";
	public final static String PERSONNEL_ADMIN_MINI = "personnelAdminMini";
	
	public String name;
	
	@ManyToMany(fetch = FetchType.EAGER)
	public Set<Permission> permissions = Sets.newHashSet();
	
    @NotAudited
    @OneToMany(mappedBy="role", cascade = {CascadeType.REMOVE}, orphanRemoval=true)
    public List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();
    
    public Set<Permission> prendiPermissions(String where) {
    	
    	Set<Permission> permissionTemp = this.permissions;
    	return permissionTemp;
    }
}
