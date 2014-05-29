package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import com.google.common.collect.Lists;


/**
 * 
 * @author dario
 * classe in cui memorizzare i permessi di accesso all'applicazione
 */
@Audited
@Entity
@Table(name="permissions")
public class Permission extends BaseModel{

	public String description;
    
    @ManyToMany
    public List <Group> groups = Lists.newArrayList();
    
	@ManyToMany(mappedBy="permissions")
	public List<Role> roles = Lists.newArrayList();
}
