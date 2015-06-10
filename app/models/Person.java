/**
 *
 */
package models;



import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Email;
import play.data.validation.Required;

/**
 * @author cristian
 *
 */
@Entity
@Audited
@Table(name = "persons")
public class Person extends BaseModel implements Comparable<Person>{

	private static final long serialVersionUID = -2293369685203872207L;

	@Version
	public Integer version;

	@Required
	public String name;

	@Required
	public String surname;

	@Column(name = "other_surnames")
	public String othersSurnames;

	@Column(name = "birthday")
	public LocalDate birthday;

	@Email
	public String email;

	@OneToOne (optional = false)
	@JoinColumn(name = "user_id")
	public User user;

	/**
	 * Numero di matricola
	 */
	public Integer number;

	/**
	 * numero di matricola sul badge
	 */

	public String badgeNumber;

	/**
	 * id che questa persona aveva nel vecchio database
	 */
	public Long oldId;

	/**
	 * Internal ID: server per l'identificazione univoca della persona nella sincronizzazione con Perseo (Person.id di Perseo)
	 */
	public Integer iId;

	/**
	 * nuovo campo email del cnr da usarsi in caso di autenticazione via shibboleth inserito con l'evoluzione 28
	 */
	@Email
	public String cnr_email;

	/**
	 * i prossimi tre campi sono stati inseriti con l'evoluzione 28 prendendoli da contact_data così da eliminare quella tabella
	 */
	public String telephone;

	public String fax;

	public String mobile;

	@Column(name="want_email")
	public boolean wantEmail;
	
	/**
	 * i successivi due campi servono per la nuova relazione tra Person e Person 
	 * relativa ai responsabili
	 */
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<Person> people = new ArrayList<Person>();
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="person_id")
	@Nullable
    public Person person;
	
	/**
	 * questo campo booleano serve a stabilire se una persona è un responsabile o no
	 */
	@Column(name="is_person_in_charge")
	public boolean isPersonInCharge;

	/**
	 * relazione con la tabella delle assenze iniziali
	 */
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<InitializationAbsence> initializationAbsences = new ArrayList<InitializationAbsence>();

	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<InitializationTime> initializationTimes = new ArrayList<InitializationTime>();

	/**
	 *  relazione con i turni
	 */
	@OneToMany(mappedBy="supervisor", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<ShiftCategories> shiftCategories = new ArrayList<ShiftCategories>();

	@OneToOne(mappedBy="person", fetch = FetchType.EAGER)
	public PersonHourForOvertime personHourForOvertime;

	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<Contract> contracts = new ArrayList<Contract>();

	/**
	 * relazione con la tabella dei figli del personale
	 */
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<PersonChildren> personChildren;

	/**
	 * relazione con la nuova tabella dei person day
	 */
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<PersonDay> personDays;

	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<CertificatedData> certificatedData;

	@OneToMany(mappedBy="admin", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<MealTicket> mealTicketsAdmin;

	/**
	 * relazione con la nuova tabella dei person_month
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<PersonMonthRecap> personMonths = new ArrayList<PersonMonthRecap>();

	/**
	 * relazione con la nuova tabella dei person_year
	 */
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<PersonYear> personYears;


	/**
	 * relazione con la tabella Competence
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<Competence> competences;

	/**
	 * relazione con la tabella dei codici competenza per stabilire se una persona ha diritto o meno a una certa competenza
	 */
	@NotAudited
	@ManyToMany(cascade = {CascadeType.REFRESH}, fetch = FetchType.LAZY)
	public List<CompetenceCode> competenceCode;


	@OneToOne(mappedBy="person", fetch=FetchType.EAGER)
	public PersonReperibility reperibility;

	@ManyToOne
	@JoinColumn(name="qualification_id")
	public Qualification qualification;

	@OneToOne(mappedBy="person", fetch=FetchType.EAGER)
	public PersonShift personShift;

	@ManyToOne
	@JoinColumn(name="office_id")
	public Office office;


	public String getName(){
		return this.name;
	}

	public String getSurname(){
		return this.surname;
	}

	public void setCompetenceCodes(List<CompetenceCode> competenceCode)
	{
		this.competenceCode = competenceCode;
	}

	/**
	 * @return il nome completo.
	 */
	public String getFullname() {
		return String.format("%s %s", surname, name);
	}


	public String fullName() {
		return getFullname();
	}

	@Override
	public String toString() {
		return String.format("Person[%d] - %s %s", id, name, surname);
	}

	@Override
	public int compareTo(Person person) {

		int res = (this.surname.compareTo(person.surname) == 0) ?  this.name.compareTo(person.name) :  this.surname.compareTo(person.surname);
		return res;
	}

}
