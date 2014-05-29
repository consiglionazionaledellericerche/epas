package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import play.mvc.With;
import controllers.Secure;

/**
 * 
 * @author dario
 * classe in cui memorizzare i gruppi che possono accedere all'applicazione
 */
@Audited
@Entity
@Table(name = "groups")
@With(Secure.class)
public class Group extends BaseModel{

	@ManyToMany(mappedBy = "groups")
	public List<Person> persons;
	
    @ManyToMany(mappedBy = "groups")
    public List <Permission> permissions;
		
	public String description;
	

	
}
