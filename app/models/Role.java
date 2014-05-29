package models;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import com.google.common.collect.Lists;



@Entity
@Audited
@Table(name="roles")
public class Role extends BaseModel{

	public final static String PERSONNEL_ADMIN = "personnelAdmin";
	public final static String PERSONNEL_ADMIN_MINI = "personnelAdminMini";
	
	public String name;
	
	@ManyToMany
	public List<Permission> permissions = Lists.newArrayList();
	
    @NotAudited
    @OneToMany(mappedBy="role", cascade = {CascadeType.REMOVE}, orphanRemoval=true)
    public List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();
}
