/**
 * 
 */
package models;



import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.ToString;

import org.eclipse.jdt.internal.core.BecomeWorkingCopyOperation;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import controllers.Check;
import controllers.Secure;

import play.data.validation.Email;
import play.data.validation.Required;
import play.db.jpa.Model;
import play.mvc.Http.Request;
import play.mvc.With;

/**
 * @author cristian
 *
 */
@Entity
@Audited
@Table(name = "persons")
@With(Secure.class)
public class Person extends Model {

	/**
	 * relazione con la tabella dei permessi
	 */
	private static final long serialVersionUID = -2293369685203872207L;
	
	@ManyToMany(cascade = {CascadeType.REFRESH}, fetch = FetchType.LAZY)
	public List<Permission> permissions;

	/**
	 * relazione con la tabella dei gruppi
	 */
	@ManyToMany(cascade = {CascadeType.REFRESH}, fetch = FetchType.LAZY)
	public List<Group> groups;
	
	
	/**
	 * relazione con la nuova tabella dei person day
	 */
	@OneToMany(mappedBy="person")
	public List<PersonDay> personDays;
	
	/**
	 * relazione con la nuova tabella dei person_month
	 */
	@OneToMany(mappedBy="person")
	public List<PersonMonth> personMonths;
	
	/**
	 * relazione con la nuova tabella dei person_year
	 */
	@OneToMany(mappedBy="person")
	public List<PersonYear> personYears;

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
	public List<YearRecap> yearRecaps;
	
	/**
	 * relazione con la tabella di storico MonthRecap
	 */
	@NotAudited
	@OneToMany(mappedBy="person")
	public List<MonthRecap> monthRecaps;
	
		
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
	public List<Absence> absences;
	
	/**
	 * relazione con la tabella di person vacation
	 */
	@OneToMany(mappedBy="person")
	public List <PersonVacation> personVacations;
	
	/**
	 * relazione con la tabella di vacation_code
	 */
	@OneToOne(mappedBy="person")
	public VacationPeriod vacationPeriod;
	
	
	/**
	 * relazione con la tabella Competence
	 */
	@NotAudited
	@OneToMany(mappedBy="person")
	public List<Competence> competences;
	
	/**
	 * relazione con la tabella delle competence valide
	 */
	@NotAudited
	@OneToMany(mappedBy="person")
	public List<ValuableCompetence> valuableCompetences;
	
	/**
	 * relazione con la tabella delle tipologie di orario di lavoro
	 */
	@ManyToOne
	@JoinColumn(name="workingTimeType_id")
	public WorkingTimeType workingTimeType;
	
	
	/**
	 * relazione con la tabella delle locazioni degli utenti
	 */
	@NotAudited
	@OneToOne(mappedBy="person")
	public Location location;
	
	/**
	 * relazione con la tabella delle info di contatto
	 */
	@NotAudited
	@OneToOne
	@JoinColumn(name="contact_data_id")
	public ContactData contactData;
	
	@OneToOne(mappedBy="person")
	public PersonReperibility reperibility;
	
	@Required
	public String name;
	
	@Required
	public String surname;
	
	public String othersSurnames;
	
	@Column(name = "born_date")
	public Date bornDate;
	
	@Email
	public String email;
		
	public String username;
	
	public String password;
	
	/**
	 * livello di contratto
	 */
	public int qualification;
	
	/**
	 * Numero di matricola
	 */
	public Integer number;
	
	public String fullName() {
		return String.format("%s %s", surname, name);
	}
	
	/**
	 * TODO: importare anche il li campo qualifica dalla tabella Persone perchè identifica il livello di contratto e mi serve per fare
	 * tutti i calcoli relativi ai contratti.
	 */
	
	/**
	 * 
	 * @return l'ultimo contratto in essere per questa persona
	 */
	public Contract getLastContract(){
		
		
		Contract contract = Contract.find("Select con from Contract con where con.person = ? order by con.beginContract desc", this).first();
		return contract;
	}
	
	/**
	 * 
	 * @param person
	 * @return il piano ferie previsto per quella persona
	 */
	@SuppressWarnings("unused")
	
	public VacationCode getVacation(Person person){
		/**
		 * TODO: controllare nella documentazione dell'envers il modo di interrogare lo storico (e amodificare la proceduira di importazione
		 * sui contratti per riempire lo storico con i vecchi contratti del personale
		 */
		VacationCode vacation = null;
		/**
		 * TODO: per adesso sul db (nemmeno in quello della vecchia applicazione) non si tiene traccia della entry con il piano ferie 
		 * ultimo previsto per coloro i quali sono a tempo indeterminato da più di tre anni (28+4) o sono a tempo determinato e la 
		 * somma dei loro anni di contratto qui dentro è superiore a 3 o ancora hanno un contratto a tempo determinato della durata di 
		 * più di 3 anni e si trovano attualmente nel terzo o successivo anno di contratto.
		 * In tutti questi casi, per adesso, bisogna quindi fare in modo che venga ritornato come piano ferie il 28+4. 
		 * Successivamente bisogna implementare nella tabella dei piani ferie, una entry che contenga questa informazione
		 */
		List<Contract> listaContratti = Contract.find("Select con from Contract con where con.person = ? " +
				"order by con.beginContract desc", person).fetch();
		
		//XXX: dovrebbe esistere questo caso?? ESISTE ESISTE
		if (listaContratti.size() == 0) {
			return null;
		}
		
		/*
		 * prendo il primo elemento della lista che ho ordinato nella query che contiene il contratto più recente. Controllo che sia
		 * diverso da null e, in tal caso, guardo la durata: se è maggiore di 3 anni rispetto alla data odierna ritorno un nuovo 
		 * VacationCode con descrizione "28+4"
		 */
		Contract con = listaContratti.get(0);
		if(con != null){
			LocalDate now = new LocalDate();
			LocalDate beginContract = new LocalDate(con.beginContract);
			LocalDate endContract = new LocalDate(con.endContract);
			if(endContract == null && beginContract != null){
				/*
				 * il contratto è a tempo indeterminato, controllo quindi se la data odierna è maggiore o no di 3 anni rispetto 
				 * all'inizio di questo contratto
				 */
				int differenzaAnni = now.getYear() - beginContract.getYear();
				int differenzaMesi = now.getMonthOfYear() - beginContract.getMonthOfYear();
				int differenzaGiorni = now.getDayOfMonth() - beginContract.getDayOfMonth();
				if(differenzaAnni >= 3 && differenzaMesi > 11 && differenzaGiorni >=0){
					vacation = new VacationCode();
					vacation.description = "28+4";
					
				}
				else{
					vacation = VacationCode.find("Select vac from VacationCode vac, VacationPeriod per where per.person = ?" +
							" and per.vacationCode = vac order by per.beginFrom", this).first();
				}
			}
			if(endContract == null && beginContract == null){
				/*
				 * è il caso di persone come Laura Abba e Mauro Balestri
				 * 
				 */
				/**
				 * TODO: fare una find su una classe di enum che associa un valore alla riga della tabella (in questo caso del 
				 * vacation code)
				 */
				vacation = new VacationCode();
				vacation.description = "28+4";
			}
			if(endContract != null && beginContract != null){
				/*
				 * bisogna controllare se nella lista dei contratti ce n'è più di uno e quanti anni questa persona ha accumulato.
				 * nel caso la durata complessiva dei contratti accumulati sia superiore a 3 anni bisogna ritornare il piano ferie 
				 * "28+4"
				 */
				if(listaContratti.size()>1){
					int diffYear = 0;
					int diffMonth = 0;
					int diffDay = 0;
					for(Contract c : listaContratti){
						if(c != null || (c.beginContract!=null && c.endContract!=null)){
							/**
							 * TODO: cambiare la tipologia di data di inizio e fine contratto da date a localdate
							 */
							diffYear = diffYear + (c.endContract.getYear()-c.beginContract.getYear());
							diffMonth = diffMonth + (c.endContract.getMonthOfYear()-c.beginContract.getMonthOfYear());
							diffDay = diffDay + (c.endContract.getDayOfMonth()-c.beginContract.getDayOfMonth());
						}
												
					}
					if(diffYear > 2 || (diffYear == 2 && diffMonth > 12)){
						vacation = new VacationCode();
						vacation.description = "28+4";
					}
				}
				else{
					vacation = VacationCode.find("Select vac from VacationCode vac, VacationPeriod per where per.person = ?" +
							" and per.vacationCode = vac order by per.beginFrom", this).first();
				}
			}
			
		}
//		VacationCode vacation = VacationCode.find("Select vac from VacationCode vac, VacationPeriod per where per.person = ?" +
//				" and per.vacationCode = vac order by per.beginFrom", person).first();		
		return vacation;
	}
	
	/**
	 * 
	 * @param person
	 * @return le info sulla locazione della persona in istituto (stanza, dipartimento ecc...)
	 */
	public Location getLocation(Person person){
		Location loc = Location.find("Select loc from Location loc where loc.person = ?", person).first();		
		return loc;
	}
	
	/**
	 * 
	 * @param person
	 * @return le informazioni di contatto ovvero mail e telefono
	 */
	public ContactData getContact(Person person){
		ContactData con = ContactData.find("Select con from ContactData con where con.person = ?", person).first();
		return con;
	}
	
	public Set<Permission> getAllPermissions(){
		Set<Permission> setPermissions = new HashSet<Permission>();
		setPermissions.addAll(permissions);
		for(Group g : groups){
			setPermissions.addAll(g.permissions);
		}
		return setPermissions;
	}
	
	
}
