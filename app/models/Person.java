/**
 *
 */
package models;



import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import manager.recaps.PersonResidualMonthRecap;
import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Email;
import play.data.validation.Required;
import play.mvc.With;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import controllers.Secure;
import dao.PersonMonthRecapDao;

/**
 * @author cristian
 *
 */
@Entity
@Audited
@Table(name = "persons")
@With(Secure.class)
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
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate birthday;

	@Column(name = "born_date")
	public Date bornDate;

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

	/**
	 * i prossimi tre campi sono stati inseriti con l'evoluzione 28 prendendoli da locations così da eliminare quella tabella
	 */
	public String department;

	@Column(name="head_office")
	public String headOffice;

	public String room;

	@Column(name="want_email")
	public boolean wantEmail;

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
	public List<ShiftType> shiftTypes = new ArrayList<ShiftType>();


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

	/**
	 * @param code
	 * @return la competenza di quella persona nell'anno year e nel mese month con il codice competenza code
	 */
	public Competence competence(final CompetenceCode code, final int year, final int month) {
		if (competenceCode.contains(code)) {
			Optional<Competence> o = FluentIterable.from(competences)
					.firstMatch(new Predicate<Competence>() {

				@Override
				public boolean apply(Competence input) {

					return input.competenceCode.equals(code) && input.year == year && input.month == month;
				}

			});
			return o.orNull();
		} else {
			return null;
		}
	}


	/**
	 *
	 * @param year
	 * @param month
	 * @return l'esito dell'invio attestati per la persona (null se non è ancora stato effettuato)
	 */
	public CertificatedData getCertificatedData(int year, int month)
	{

		CertificatedData cd = PersonMonthRecapDao.getCertificatedDataByPersonMonthAndYear(this, month, year);
//		CertificatedData cd = CertificatedData.find("Select cd from CertificatedData cd where cd.person = ? and cd.year = ? and cd.month = ?",
//				this, year, month).first();
		return cd;
	}



	/**
	 *
	 * @param year
	 * @param month
	 * @return le ore di residuo positivo fatte nel mese/anno da this. Metodo usato nel template showCompetences
	 */
	public Integer getPositiveResidualInMonth(int year, int month){

		return PersonResidualMonthRecap.positiveResidualInMonth(this, year, month)/60;
	}




	@Override
	public int compareTo(Person person) {

		int res = (this.surname.compareTo(person.surname) == 0) ?  this.name.compareTo(person.name) :  this.surname.compareTo(person.surname);
		return res;
	}

}
