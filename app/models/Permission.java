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

import com.google.common.collect.Lists;

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

	public String description;
    
    @ManyToMany
    public List <Group> groups = Lists.newArrayList();
    
	@ManyToMany(mappedBy="permissions")
	public List<Role> roles = Lists.newArrayList();
}
