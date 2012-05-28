package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Data;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 * il contratto non è gestito direttamente da questa applicazione ma le sue informazioni
 * sono prelevate da un altro servizio
 */
@Audited
@Entity
@Table(name="contract")
public class Contract extends Model {
	
	private static final long serialVersionUID = -4472102414284745470L;
	
//	public Qualification qualification;
	
//	public ContractLevel contractLevel;
	
	@OneToOne
	@JoinColumn(name="person_id")
	public Person person;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate beginContract;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate endContract;
	
	public boolean isContinued;
	
	public boolean workSaturday;
	
	public boolean workSunday;
}
