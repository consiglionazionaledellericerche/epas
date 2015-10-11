/**
 *
 */
package models;



import it.cnr.iit.epas.NullStringBinder;

import java.util.List;
import java.util.Set;

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
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import models.base.MutableModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.binding.As;
import play.data.validation.Email;
import play.data.validation.Required;
import play.data.validation.Unique;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author cristian
 *
 */

/**
 * IMPORTANTE: relazione con user impostata a LAZY per non scaricare tutte le 
 * informazioni della persona in fase di personDao.list.
 * Necessaria comunque la join con le relazioni OneToOne. 
 */
@Entity
@Audited
@Table(name = "persons", uniqueConstraints={@UniqueConstraint(columnNames={"badgenumber", "office_id"})})

public class Person extends MutableModel implements Comparable<Person>{

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
	@Unique @As(binder=NullStringBinder.class)
	@Required
	public String email;

	@OneToOne (optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	public User user;

	/**
	 * Numero di matricola
	 */
	@Unique
	public Integer number;



	/**
	 * id che questa persona aveva nel vecchio database
	 */
	public Long oldId;

	/**
	 * Internal ID: server per l'identificazione univoca della persona nella 
	 * sincronizzazione con Perseo (Person.id di Perseo)
	 */
	@Unique
	public Integer iId;

	/**
	 * Campo da usarsi in caso di autenticazione via shibboleth.
	 */
	@Unique
	@As(binder=NullStringBinder.class)
	public String eppn;

	public String telephone;

	public String fax;

	public String mobile;

	@Column(name="want_email")
	public boolean wantEmail;
	
	/**
	 * i successivi due campi servono per la nuova relazione tra Person e Person 
	 * relativa ai responsabili
	 */
	@OneToMany(mappedBy="personInCharge")
	@OrderBy("surname")
    public List<Person> people = Lists.newArrayList();;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="person_in_charge")
	@Nullable
    public Person personInCharge;
	
	/**
	 * questo campo booleano serve a stabilire se una persona è un responsabile o no
	 */
	@Column(name="is_person_in_charge")
	public boolean isPersonInCharge;

	/**
	 *  relazione con i turni
	 */
	@OneToMany(mappedBy="supervisor")
	public List<ShiftCategories> shiftCategories = Lists.newArrayList();

	@NotAudited
	@OneToMany(mappedBy="person", cascade = {CascadeType.REMOVE})
	public List<Contract> contracts = Lists.newArrayList();

	/**
	 * relazione con la tabella dei figli del personale
	 */
	@OneToMany(mappedBy="person", cascade = {CascadeType.REMOVE})
	public Set<PersonChildren> personChildren = Sets.newHashSet();

	/**
	 * relazione con la nuova tabella dei person day
	 */
	@OneToMany(mappedBy="person", cascade = {CascadeType.REMOVE})
	public List<PersonDay> personDays = Lists.newArrayList();;

	@OneToMany(mappedBy="person", cascade = {CascadeType.REMOVE})
	public List<CertificatedData> certificatedData = Lists.newArrayList();;

	@OneToMany(mappedBy="admin")
	public List<MealTicket> mealTicketsAdmin = Lists.newArrayList();;

	/**
	 * relazione con la nuova tabella dei person_month
	 */
	@NotAudited
	@OneToMany(mappedBy="person", cascade = {CascadeType.REMOVE})
	public List<PersonMonthRecap> personMonths = Lists.newArrayList();;

	/**
	 * relazione con la nuova tabella dei person_year
	 */
	@OneToMany(mappedBy="person", cascade = {CascadeType.REMOVE})
	public List<PersonYear> personYears = Lists.newArrayList();;


	/**
	 * relazione con la tabella Competence
	 */
	@NotAudited
	@OneToMany(mappedBy="person", cascade = {CascadeType.REMOVE})
	public List<Competence> competences = Lists.newArrayList();;

	/**
	 * relazione con la tabella dei codici competenza per stabilire se una persona ha diritto o meno a una certa competenza
	 */
	@NotAudited
	@ManyToMany(cascade = {CascadeType.REFRESH})
	public List<CompetenceCode> competenceCode = Lists.newArrayList();;

	@OneToOne(mappedBy="person")
	public PersonHourForOvertime personHourForOvertime;

	@OneToOne(mappedBy="person", cascade = {CascadeType.REMOVE})
	public PersonReperibility reperibility;

	@OneToOne(mappedBy="person")
	public PersonShift personShift;
	
	@ManyToOne
	@JoinColumn(name="qualification_id")
	@Required
	public Qualification qualification;

	@ManyToOne
	@JoinColumn(name="office_id")
	@Required
	public Office office;

	/** 
	 * Rimuoverlo quando sarà stata effettuata la migrazione di tutti i badge
	 * alla tabella badges.
	 */
	@Deprecated
	@As(binder=NullStringBinder.class)
	public String badgeNumber;
	
	@OneToMany(mappedBy="person", cascade = {CascadeType.REMOVE})
	public Set<Badge> badges = Sets.newHashSet();


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
		return getFullname();
	}

	@Override
	public int compareTo(Person person) {

		int res = (this.surname.compareTo(person.surname) == 0) ?  this.name.compareTo(person.name) :  this.surname.compareTo(person.surname);
		return res;
	}

}
