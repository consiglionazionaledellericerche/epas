package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

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

@Entity
@Table(name="contracts")
public class Contract extends Model {
	
	private static final long serialVersionUID = -4472102414284745470L;
	
//	public Qualification qualification;
	
//	public ContractLevel contractLevel;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_id")
	public Person person;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="begin_contract")
	public LocalDate beginContract;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="expire_contract")
	public LocalDate expireContract;

//	@Column(name="is_continued")
//	public boolean isContinued;
	/**
	 * data di termine contratto in casi di licenziamento, pensione, morte, ecc ecc...
	 */
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="end_contract")
	public LocalDate endContract;
	
	public boolean workSaturday;
	
	public boolean workSunday;
	
	@Transient
	public boolean isValidContract(){
		LocalDate date = new LocalDate();
		return endContract==null && beginContract.isBefore(date) && expireContract.isAfter(date);
					 
	}
	
}
