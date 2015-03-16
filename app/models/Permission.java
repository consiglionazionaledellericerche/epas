package models;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import com.google.common.collect.Sets;


/**
 * 
 * @author dario
 * classe in cui memorizzare i permessi di accesso all'applicazione
 */
@Audited
@Entity
@Table(name="permissions")
public class Permission extends BaseModel{

	private static final long serialVersionUID = 7368960878704914559L;

	public String description;
     
	@ManyToMany(mappedBy="permissions")
	public Set<Role> roles = Sets.newHashSet();
}
