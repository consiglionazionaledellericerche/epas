package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import play.data.validation.Unique;
import play.db.jpa.Model;

/**
 * 
 * @author dario
 * classe in cui memorizzare i permessi di accesso all'applicazione
 */
@Audited
@Entity
@Table(name="permissions")
public class Permission extends Model{

	@Unique
	public String description;
	
//	@ManyToMany(mappedBy = "permissions", cascade = { CascadeType.ALL })
//    public List<User> users;
    
    @ManyToMany
    public List <Group> groups;
    
    @NotAudited
    @OneToMany(mappedBy="permission", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<UsersPermissionsOffices> userPermissionOffices = new ArrayList<UsersPermissionsOffices>();
}
