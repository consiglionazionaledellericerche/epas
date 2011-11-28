package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 */
@Entity
public class Contract extends Model{
	
	
//	@OneToMany(mappedBy="contract")
//	@JoinColumn(name="qualification_id")
//	public Qualification qualification;
	
//	@ManyToOne
//	@JoinColumn(name= "person_id", nullable = false)
//	public Person person;
	
	@Column
	public Date beginContract;
	
	@Column
	public Date endContract;
	
	@Column
	public Date previousContract;
}
