/**
 * 
 */
package models;



import it.cnr.iit.epas.JsonReperibilityPeriodsBinder;
import it.cnr.iit.epas.JsonStampingBinder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
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
import models.Stamping.WayType;
import models.exports.ReperibilityPeriods;
import models.exports.StampingFromClient;

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
import controllers.Security;

import play.Logger;
import play.data.binding.As;
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
	 * relazione con la tabella delle assenze iniziali
	 */
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY)
	public List<InitializationAbsence> initializationAbsences = new ArrayList<InitializationAbsence>();



	/**
	 * relazione con la tabella delle info di contatto
	 */
	@OneToOne(mappedBy="person", fetch = FetchType.EAGER, cascade = {CascadeType.REMOVE})
	public ContactData contactData;
	
	/**
	 * relazione con la tabella delle info di contatto
	 */
	@OneToOne(mappedBy="person", fetch = FetchType.EAGER, cascade = {CascadeType.REMOVE})
	public PersonHourForOvertime personHourForOvertime;

	/**
	 * relazione con la tabella dei contratti
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.EAGER, cascade = {CascadeType.REMOVE})
	public List<Contract> contracts = new ArrayList<Contract>(); 

	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY)
	public List<StampProfile> stampProfiles = new ArrayList<StampProfile>();

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
	 * relazione con la tabella dei figli del personale
	 */
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY)
	public List<PersonChildren> personChildren;

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
	 * relazione con la tabella di storico YearRecap
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY)
	public List<YearRecap> yearRecaps;

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
	 * relazione con la tabella dei codici competenza per stabilire se una persona ha diritto o meno a una certa competenza
	 */
	@NotAudited
	@ManyToMany(cascade = {CascadeType.REFRESH}, fetch = FetchType.LAZY)
	public List<CompetenceCode> competenceCode;
	

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
	@OneToOne(mappedBy="person", fetch=FetchType.EAGER, cascade = {CascadeType.REMOVE})
	public Location location;


	@OneToOne(mappedBy="person", fetch=FetchType.LAZY)
	public PersonReperibility reperibility;

	@ManyToOne( fetch=FetchType.EAGER )
	@JoinColumn(name="qualification_id")
	public Qualification qualification;

	@OneToOne(mappedBy="person", fetch=FetchType.LAZY)
	public PersonShift personShift;
	
//	@NotAudited
//	@ManyToOne(fetch=FetchType.LAZY)
//	@JoinColumn(name="remote_office_id", nullable=true)
//	public RemoteOffice remoteOffice;

	public String fullName() {
		return String.format("%s %s", surname, name);
	}



	/**
	 * 
	 * @param person
	 * @return il piano ferie previsto per quella persona
	 */
	@SuppressWarnings("unused")

	public VacationCode getVacation(){

		VacationCode vacation = null;

		Contract contract = Contract.find("Select con from Contract con where con.person = ?", this).first();
		if(contract == null){
			Logger.warn("Siamo nel bottino che il contratto è nullo per %s", this);
			throw new IllegalStateException(String.format("Il contratto della persona %s è nullo", this));
		}
		LocalDate now = LocalDate.now();
		if(contract.expireContract == null && contract.beginContract != null){
			/**
			 * il contratto attuale è a tempo indeterminato, controllo che sia in vigore da più di 3 anni 
			 */
			int differenzaAnni = now.getYear() - contract.beginContract.getYear();
			int differenzaMesi = now.getMonthOfYear() - contract.beginContract.getMonthOfYear();
			int differenzaGiorni = now.getDayOfMonth() - contract.beginContract.getDayOfMonth();
			if(differenzaAnni > 3 ){
				vacation = VacationCode.find("Select vc from VacationCode vc, VacationPeriod vp " +
						"where vp.vacationCode = vc and vp.person = ?", this).first();
				if(vacation == null){
					VacationPeriod vacationPeriod = new VacationPeriod();
					vacationPeriod.person = this;
					vacationPeriod.beginFrom = contract.beginContract;
					vacationPeriod.endTo = null;
					vacationPeriod.vacationCode = VacationCode.find("Select vc from VacationCode vc where vc.description = ?", "28+4").first();
					vacationPeriod.save();
					vacation = VacationCode.find("Select vc from VacationCode vc, VacationPeriod vp " +
							"where vp.vacationCode = vc and vp.person = ?", this).first();
					return vacation;
				}

			}
			else{
				vacation = VacationCode.find("Select vac from VacationCode vac, VacationPeriod per where per.person = ?" +
						" and per.vacationCode = vac order by per.beginFrom", this).first();
				return vacation;
			}
		}
		if(contract.expireContract != null && contract.beginContract != null){

			int differenzaAnni = contract.expireContract.getYear() - contract.beginContract.getYear();

			if(this.getQualification() == null){
				vacation = null;
			}			
			else{
				if(differenzaAnni >= 3){

					vacation = VacationCode.find("Select vc from VacationCode vc where vc.description = ?", "28+4").first();
					VacationPeriod period = VacationPeriod.find("Select vp from VacationPeriod vp where vp.person = ?", this).first();
					if(period == null){
						period = new VacationPeriod();
						period.person = this;
						period.vacationCode = vacation;
						period.beginFrom = LocalDate.now();
						period.endTo = null;
						period.save();
					}

				}					
				else
					vacation = VacationCode.find("Select vc from VacationCode vc where vc.description = ?","26+4").first();				
			}		
		}
		return vacation;
	}

	/**
	 * 
	 * @return la locazione della persona
	 */
	public Location getLocation(){
		if(this.location != null)
			return this.location;
		else
			return null;
	}

	public Set<Permission> getAllPermissions(){
		Set<Permission> setPermissions = new HashSet<Permission>();
		setPermissions.addAll(permissions);

		return setPermissions;
	}

	/**
	 * 
	 * @return il contratto attivo per quella persona alla date date
	 */
	public Contract getContract(LocalDate date){

		Contract contract = Contract.find("Select con from Contract con where con.person = ? and (con.beginContract IS NULL or con.beginContract <= ?) and " +
				"(con.expireContract > ? or con.expireContract is null )",this, date, date).first();
		if(contract == null){
			return null;
		}
//		Logger.debug("La lista contratti è %s", contracts);
//		Logger.debug("Il primo contratto è: %s", contracts.get(0));
//		
//		if (contracts.size() > 1) {
//
//			throw new IllegalStateException(
//					String.format("Trovati più contratti in contemporanea per la persona %s nella data %s", this, date));
//			
//		}
//		else
		return contract;

	}
	/**
	 * 
	 * @return il contratto attualmente attivo per quella persona
	 */
	public Contract getCurrentContract(){
		return getContract(LocalDate.now());
	}

	/**
	 * 
	 * @return la qualifica della persona
	 */
	public Qualification getQualification(){
		if(this.qualification != null)
			return this.qualification;
		else
			return null;
	}


	@Override
	public String toString() {
		return String.format("Person[%d] - %s %s", id, name, surname);
	}

	/**
	 * Metodi di utilità per verificare se nella lista dei permessi c'è il permesso richiesto. Utile in visualizzazione
	 * 
	 */

	public boolean isViewPersonAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.VIEW_PERSON_LIST))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdatePersonAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_PERSON))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateAbsenceAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_ABSENCE))
				return true;
		}
		return false;
	}

	public boolean isDeletePersonAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.DELETE_PERSON))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateWorkinTimeAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_WORKINGTIME))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateStampingAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_STAMPING))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdatePasswordAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_PASSWORD))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateConfigurationAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_CONFIGURATION))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateAdministratorAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_ADMINISTRATOR))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateCompetenceAndOvertimeAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_COMPETENCES))
				return true;
		}
		return false;
	}

	public boolean isInsertAndUpdateVacationsAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_VACATIONS))
				return true;
		}
		return false;
	}
	
	public boolean isUploadSituationAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.UPLOAD_SITUATION))
				return true;
		}
		return false;
	}

	/**
	 * 
	 * @param date
	 * @return la lista di persone attive a quella data
	 */
	public static List<Person> getActivePersons(LocalDate date){
		List<Person> activePersons = Person.find("Select distinct (p) from Person p, Contract c where c.person = p and (c.endContract is null or c.endContract > ?)" +
				" and (c.expireContract > ? or c.expireContract is null) and (c.beginContract < ? or c.beginContract is null) order by p.surname, p.name", date, date, date).fetch();
		return activePersons;

	}
	
	/**
	 * 
	 * @param date
	 * @return la lista di tecnici che beneficiano di competenze (utilizzata nel controller competences, metodo showCompetences)
	 */
	public static List<Person> getTechnicianForCompetences(LocalDate date){
		List<Person> technicians = new ArrayList<Person>();
		List<Person> activePersons = getActivePersons(date);
		for(Person p : activePersons){
			if(p.qualification != null && p.qualification.qualification > 3)
				technicians.add(p);
		}
		return technicians;
	}

	/**
	 * metodo per la creazione di una timbratura a partire dall'oggetto stampModificationType che è stato costruito dal binder del Json
	 * passato dal client python
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static boolean createStamping(StampingFromClient stamping){

		if(stamping == null)
			return false;
		
		Long id = stamping.personId;
		
		if(id == null){
			Logger.warn("L'id della persona passata tramite json non ha trovato corrispondenza nell'anagrafica del personale. Controllare id = null");
			return false;
		}
			
		Person person = Person.findById(id);
		if(person == null){
			Logger.warn("L'id della persona passata tramite json non ha trovato corrispondenza nell'anagrafica del personale. Controllare id = %s", id);
			return false;
		}
		
		Logger.debug("Sto per segnare la timbratura di %s %s", person.name, person.surname);
		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", 
				person, stamping.dateTime.toLocalDate() ).first();
		if(pd == null){
			/**
			 * non esiste un personDay per quella data, va creato e quindi salvato
			 */
			//Logger.debug("Non esiste il personDay...è il primo personDay per il giorno %s per %s %s", pd.date, person.name, person.surname);
			pd = new PersonDay(person, stamping.dateTime.toLocalDate());
			pd.save();		
			Logger.debug("Salvato il nuovo personDay %s", pd);
			Stamping stamp = new Stamping();
			stamp.date = stamping.dateTime;
			stamp.markedByAdmin = false;
//			stamp.considerForCounting = true;
			if(stamping.inOut == 0)
				stamp.way = WayType.in;
			else
				stamp.way = WayType.out;
			stamp.badgeReader = stamping.badgeReader;
			stamp.personDay = pd;
			stamp.save();
			pd.stampings.add(stamp);
			pd.save();

		}
		else{
			if(checkDuplicateStamping(pd, stamping) == false){
				Stamping stamp = new Stamping();
				stamp.date = stamping.dateTime;
				stamp.markedByAdmin = false;
//				stamp.considerForCounting = true;
				if(stamping.inOut == 0)
					stamp.way = WayType.in;
				else
					stamp.way = WayType.out;
				stamp.badgeReader = stamping.badgeReader;
				stamp.personDay = pd;
				stamp.save();
				pd.stampings.add(stamp);
				pd.save();
			}
			else{
				Logger.info("All'interno della lista di timbrature di %s %s nel giorno %s c'è una timbratura uguale a quella passata dallo" +
						"stampingsFromClient: %s", person.name, person.surname, pd.date, stamping.dateTime);
			}
			//0113 00004000000000000086063304051407
//			for(Stamping s : pd.stampings){
//				if(!s.date.isEqual(stamping.dateTime)){
//					Stamping stamp = new Stamping();
//					stamp.date = stamping.dateTime;
//					stamp.markedByAdmin = false;
//					if(stamping.inOut == 0)
//						stamp.way = WayType.in;
//					else
//						stamp.way = WayType.out;
//					stamp.badgeReader = stamping.badgeReader;
//					stamp.personDay = pd;
//					stamp.save();
//				}
//			}
			
		}
		Logger.debug("Chiamo la populatePersonDay per fare i calcoli sulla nuova timbratura inserita per il personDay %s", pd);
		pd.populatePersonDay();
		/**
		 * controllare se può essere una save qui al posto della merge il problema al fatto che le timbrature prese dal client non permettano
		 * i calcoli
		 */
		pd.save();
		return true;
	}

	/**
	 * 
	 * @param pd
	 * @param stamping
	 * @return true se all'interno della lista delle timbrature per quel personDay c'è una timbratura uguale a quella passata come parametro
	 * false altrimenti
	 */
	private static boolean checkDuplicateStamping(PersonDay pd, StampingFromClient stamping){
		for(Stamping s : pd.stampings){
			if(s.date.isEqual(stamping.dateTime)){
				return true;
			}
		}return false;
	}
	
	
	/**
	 * 
	 * @return se è attiva la reperibilità nei giorni lavorativi
	 */
	public boolean isWorkDayReperibilityAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("207"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attiva la reperibilità festiva
	 */
	public boolean isHolidayReperibilityAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("208"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attivo lo straordinario nei giorni lavorativi
	 */
	public boolean isOvertimeInWorkDayAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("S1"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attivo il turno ordinario
	 */
	public boolean isOrdinaryShiftAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("T1"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attivo il turno notturno
	 */
	public boolean isNightlyShiftAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("T2"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attivo il turno festivo
	 */
	public boolean isHolidayShiftAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("T3"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attivo lo straordinario notturno nei giorni lavorativi o diurno nei festivi
	 */
	public boolean isOvertimeInHolidayOrNightlyInWorkDayAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("S2"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attivo lo straordinario notturno nei giorni festivi
	 */
	public boolean isOvertimeInNightlyHolidayAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("S3"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attiva l'indennità meccanografica
	 */
	public boolean isMechanographicalAllowanceAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("050"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attiva per la persona l'indennità di sede disagiata
	 */
	public boolean isHardshipAllowance(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("042"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attiva l'indennità per maneggio valori
	 */
	public boolean isHandleValuesAllowanceAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("203"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attiva l'indennità natanti
	 */
	public boolean isBoatsAllowanceAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("367"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attiva l'indennità di rischio subacquei
	 */
	public boolean isRiskDivingAllowanceAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("356"))
				flag = true;
		}
		return flag;
	}
	
	public boolean isRiskDegreeOneAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("351"))
				flag = true;
		}
		return flag;
	}
	
	public boolean isRiskDegreeTwoAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("352"))
				flag = true;
		}
		return flag;
	}

	public boolean isRiskDegreeThreeAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("353"))
				flag = true;
		}
		return flag;
	}

	public boolean isRiskDegreeFourAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("354"))
				flag = true;
		}
		return flag;
	}

	public boolean isRiskDegreeFiveAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("355"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attiva l'indennità mansione
	 */
	public boolean isTaskAllowanceAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("106"))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * 
	 * @return se è attiva l'indennità mansione maggiorata
	 */
	public boolean isTaskAllowanceIncreasedAvailable(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("107"))
				flag = true;
		}
		return flag;
	}
	
	public boolean isIonicRadianceRiskCom1Available(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("205"))
				flag = true;
		}
		return flag;
	}
	
	public boolean isIonicRadianceRiskCom3Available(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("206"))
				flag = true;
		}
		return flag;
	}
	
	public boolean isIonicRadianceRiskCom1AvailableBis(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("303"))
				flag = true;
		}
		return flag;
	}
	
	public boolean isIonicRadianceRiskCom3AvailableBis(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("304"))
				flag = true;
		}
		return flag;
	}

}
