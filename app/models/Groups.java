package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.JoinColumn;

import org.hibernate.envers.Audited;

import controllers.Check;
import controllers.Secure;

import play.db.jpa.Model;
import play.mvc.With;

/**
 * 
 * @author dario
 * classe in cui memorizzare i gruppi che possono accedere all'applicazione
 */
@Audited
@Entity
@Table(name = "groups")
@With(Secure.class)
public class Groups extends Model{

	@ManyToMany(mappedBy = "groups")
	public List<Person> persons;
	
    @ManyToMany(mappedBy = "groups")
    public List <Permissions> permissions;
	
	public int groupType;
	
	public String description;
	
	@Check("administrator")
	public void addPersonToGroup(Person person){
		
	}

	
}
