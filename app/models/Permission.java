package models;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

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
	
	@ManyToMany(mappedBy = "permissions", cascade = { CascadeType.ALL })
    public List<User> users;
    
    @ManyToMany
    public List <Group> groups;
}
