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

import com.google.common.collect.Lists;

import play.db.jpa.Model;

@Entity
@Audited
@Table(name="roles")
public class Role extends Model{

	public final static String PERSONNEL_ADMIN = "personnelAdmin";
	public final static String PERSONNEL_ADMIN_MINI = "personnelAdminMini";
	
	public String name;
	
	@ManyToMany
	public List<Permission> permissions = Lists.newArrayList();
	
    @NotAudited
    @OneToMany(mappedBy="role", cascade = {CascadeType.REMOVE}, orphanRemoval=true)
    public List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();
}
