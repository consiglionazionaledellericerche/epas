package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name = "contracts")
public class Contract extends Model{
	
	
	@OneToMany(mappedBy="contract")
	public List<Qualification> qualification;
	
	@ManyToOne
	@JoinColumn(name="contractLevel_id")
	public ContractLevel contractLevel;
	
	@ManyToOne
	@JoinColumn(name= "person_id")
	public Person person;
	
	@Column
	public Date beginContract;
	
	@Column
	public Date endContract;
	
	@Column
	public Date previousContract;
}
