package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 * classe in cui memorizzare i permessi di accesso all'applicazione
 */
@Audited
@Entity
@Table(name="permissions")
public class Permissions extends Model{

	public String description;
	
	/**
	 * da verificare se vadano bene tre campi booleani per le possibilità di intervento sulle classi 
	 */
	public boolean canRead;
	
	public boolean canWrite;
	
	public boolean canModify;
	
	@ManyToMany(mappedBy = "permissions")
    public List<Person> users;
    
    @ManyToMany
    public List <Groups> groups;
}
