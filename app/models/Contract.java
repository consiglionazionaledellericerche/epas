package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import lombok.Data;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 * il contratto non è gestito direttamente da questa applicazione ma le sue informazioni
 * sono prelevate da un altro servizio
 */
@Data
@Audited
@Entity
@Table(name="contract")
public class Contract extends Model{
	
//	public Qualification qualification;
	
//	public ContractLevel contractLevel;
	
	@OneToOne
	@JoinColumn(name="person_id")
	public Person person;
	
	public Date beginContract;
	
	public Date endContract;
	
	public boolean isContinued;
	
	public boolean workSaturday;
	
	public boolean workSunday;
}
