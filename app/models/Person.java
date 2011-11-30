/**
 * 
 */
package models;



import java.util.Date;

import java.util.List;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.data.validation.Email;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Table(name = "persons")
public class Person extends Model {
	
	/**
	 * relazione con la tabella delle timbrature
	 */
	@OneToMany(mappedBy="person")
	public List<Stamping> stampings;
	
	/**
	 * relazione con la tabella dei contratti
	 */
	@OneToMany(mappedBy="person")
	public List<Contract> contract;
	
	/**
	 * relazione con la tabella delle absence
	 */
	@OneToMany(mappedBy="person")
	public List<Absences> absences;
	
	/**
	 * relazione con la tabella di person vacation
	 */
	@OneToMany(mappedBy="person")
	public List <PersonVacation> personVacation;
	
	/**
	 * relazione con la tabella delle tipologie di orario di lavoro
	 */
	@OneToOne
	@JoinColumn(name="workingTimeType_id")
	public WorkingTimeType workingTimeType;
	
	/**
	 * relazione con la tabella delle locazioni degli utenti
	 */
	@OneToOne
	@JoinColumn(name="location_id")
	public Location location;
	
	/**
	 * relazione con la tabella delle info di contatto
	 */
	@OneToOne
	@JoinColumn(name="contactData_id")
	public ContactData contact;
	
	@Required
	public String name;
	
	@Required
	public String surname;
	
	@Column
	public Date bornDate;
	
	@Email
	public String email;
	
	/**
	 * relazione con la tabella dei contratti
	 */
	@ManyToOne
	@JoinColumn(name = "contract_level_id")
	public ContractLevel contractLevel;
	
	/**
	 * Numero di matricola
	 */
	public Integer number;
	
}
