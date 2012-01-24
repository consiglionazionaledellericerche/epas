/**
 * 
 */
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
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import play.data.validation.Email;
import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Audited
@Table(name = "persons")
public class Person extends Model {

	private static final long serialVersionUID = -2293369685203872207L;

	/**
	 * relazione con la tabella delle timbrature
	 */
	@NotAudited
	@OneToMany(mappedBy="person")
	public List<Stamping> stampings;
	
	/**
	 * relazione con la tabella di storico YearRecap
	 */
	@NotAudited
	@OneToMany(mappedBy="person")
	public List<YearRecap> yearRecap;
	
	/**
	 * relazione con la tabella di storico MonthRecap
	 */
	@NotAudited
	@OneToMany(mappedBy="person")
	public List<MonthRecap> monthRecap;
	
	/**
	 * relazione con la tabella dei contratti
	 */
	@Transient
	@OneToOne(mappedBy="person")
	@JoinColumn(name="contract_id")
	public Contract contract;
	
	/**
	 * relazione con la tabella delle absence
	 */
	@NotAudited
	@OneToMany(mappedBy="person")
	public List<Absence> absence;
	
	/**
	 * relazione con la tabella di person vacation
	 */
	@OneToMany(mappedBy="person")
	public List <PersonVacation> personVacation;
	
	/**
	 * relazione con la tabella di vacation_code
	 */
	@OneToOne
	@JoinColumn(name="vacation_period_id")
	public VacationPeriod vacationPeriod;
	
	
	/**
	 * relazione con la tabella Competence
	 */
	@NotAudited
	@OneToMany(mappedBy="person")
	public List<Competence> competence;
	
	/**
	 * relazione con la tabella delle tipologie di orario di lavoro
	 */
	@OneToMany(mappedBy="person")
	public List<WorkingTimeType> workingTimeType;
	
	/**
	 * relazione con la tabella delle locazioni degli utenti
	 */
	@NotAudited
	@OneToOne
	@JoinColumn(name="location_id")
	public Location location;
	
	/**
	 * relazione con la tabella delle info di contatto
	 */
	@NotAudited
	@OneToOne
	@JoinColumn(name="contact_data_id")
	public ContactData contactData;
	
	@Required
	public String name;
	
	@Required
	public String surname;
	
	public String othersSurname;
	
	@Column(name = "born_date")
	public Date bornDate;
	
	@Email
	public String email;
		
	public String username;
	
	public String password;
	
	/**
	 * Numero di matricola
	 */
	public Integer number;
	
	public void createPerson(){
		
	}
	
	public void deletePerson(long id){
		/**
		 * a cascata vanno elimate tutte le occorrenze nelle tabelle correlate con person che hanno l'id dell persona
		 * da cancellare
		 */
		int numberOfPersonDeleted = Person.delete("", id);
	}
	
}
