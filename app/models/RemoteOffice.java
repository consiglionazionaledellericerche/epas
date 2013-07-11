package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.NotAudited;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 */

@Entity
@Table(name = "remote_office")
public class RemoteOffice extends Model{

	@Column
	public String description;
	
	@Column
	public String address;
	
//	@NotAudited
//	@OneToMany(mappedBy="remoteOffice", fetch=FetchType.LAZY)
//	public List<Person> persons;
}
