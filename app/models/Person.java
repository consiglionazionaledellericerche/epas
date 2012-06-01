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
import javax.persistence.Version;

import lombok.ToString;

import net.spy.memcached.FailureMode;

import org.eclipse.jdt.internal.core.BecomeWorkingCopyOperation;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditQueryCreator;
import org.hibernate.envers.query.criteria.AuditConjunction;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.joda.time.LocalDate;

import controllers.Check;
import controllers.Secure;

import play.data.validation.Email;
import play.data.validation.Required;
import play.db.jpa.JPA;
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

	
	/**
	 * Used for optimisti locking
	 */
	@Version
	public Integer version;
	
	@Required
	public String name;
	
	@Required
	public String surname;
	
	@Column(name = "other_surnames")
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
	public Integer qualification;
	
	/**
	 * Numero di matricola
	 */
	public Integer number;

	/**
	 * relazione con la tabella delle info di contatto
	 */
	@OneToOne(mappedBy="person", fetch = FetchType.LAZY)
	public ContactData contactData;
	
	/**
	 * relazione con la tabella dei contratti
	 */
	@Transient
	@OneToOne(mappedBy="person", fetch=FetchType.LAZY)
	@JoinColumn(name="contract_id")
	public Contract contract;
	
	/**
	 * relazione con la tabella delle tipologie di orario di lavoro
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="working_time_type_id")
	public WorkingTimeType workingTimeType;
	
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
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY)
	public List<PersonDay> personDays;
	
	/**
	 * relazione con la nuova tabella dei person_month
	 */
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY)
	public List<PersonMonth> personMonths;
	
	/**
	 * relazione con la nuova tabella dei person_year
	 */
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY)
	public List<PersonYear> personYears;

	/**
	 * relazione con la tabella delle timbrature
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY)
	public List<Stamping> stampings;
	
	/**
	 * relazione con la tabella di storico YearRecap
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY)
	public List<YearRecap> yearRecaps;
	
	/**
	 * relazione con la tabella di storico MonthRecap
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY)
	public List<MonthRecap> monthRecaps;
	
	
	/**
	 * relazione con la tabella delle absence
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY)
	public List<Absence> absences;
	
	/**
	 * relazione con la tabella di person vacation
	 */
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY)
	public List <PersonVacation> personVacations;
	
	/**
	 * relazione con la tabella di vacation_code
	 */
	@OneToOne(mappedBy="person", fetch=FetchType.LAZY)
	public VacationPeriod vacationPeriod;
	
	/**
	 * relazione con la tabella Competence
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY)
	public List<Competence> competences;
	
	/**
	 * relazione con la tabella delle competence valide
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY)
	public List<ValuableCompetence> valuableCompetences;
	
	/**
	 * relazione con la tabella delle locazioni degli utenti
	 */
	@NotAudited
	@OneToOne(mappedBy="person", fetch=FetchType.LAZY)
	public Location location;
	
	
	@OneToOne(mappedBy="person", fetch=FetchType.LAZY)
	public PersonReperibility reperibility;
	
	public String fullName() {
		return String.format("%s %s", surname, name);
	}
	

	
	/**
	 * 
	 * @param person
	 * @return il piano ferie previsto per quella persona
	 */
	@SuppressWarnings("unused")
	
	public VacationCode getVacation(Person person){
	
		VacationCode vacation = null;

		Contract contract = person.contract;
		LocalDate now = new LocalDate();
		if(contract.endContract == null && contract.beginContract != null){
			/**
			 * il contratto attuale è a tempo indeterminato, controllo che sia in vigore da più di 3 anni 
			 */
			int differenzaAnni = now.getYear() - contract.beginContract.getYear();
			int differenzaMesi = now.getMonthOfYear() - contract.beginContract.getMonthOfYear();
			int differenzaGiorni = now.getDayOfMonth() - contract.beginContract.getDayOfMonth();
			if(differenzaAnni >= 3 && differenzaMesi > 11 && differenzaGiorni >=0){
				vacation = new VacationCode();
				vacation.description = "28+4";
				
			}
			else{
				vacation = VacationCode.find("Select vac from VacationCode vac, VacationPeriod per where per.person = ?" +
						" and per.vacationCode = vac order by per.beginFrom", this).first();
			}
		}
		if(contract.endContract != null && contract.beginContract != null){
			/*
			 * bisogna controllare se nella lista dei contratti ce n'è più di uno e quanti anni questa persona ha accumulato.
			 * nel caso la durata complessiva dei contratti accumulati sia superiore a 3 anni bisogna ritornare il piano ferie 
			 * "28+4"
			 * Si fa la query sullo storico:
			 */
			AuditReader reader = AuditReaderFactory.get(JPA.em());
			List<Contract> listaContratti = (List<Contract>) reader.createQuery().forRevisionsOfEntity(Contract.class, true, false)
					.addOrder(AuditEntity.property("endContract").asc())
					;
			
			if (listaContratti.size() == 0) {
				return null;
			}
			
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
			
			
		}
//		VacationCode vacation = VacationCode.find("Select vac from VacationCode vac, VacationPeriod per where per.person = ?" +
//				" and per.vacationCode = vac order by per.beginFrom", person).first();		
		return vacation;
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
