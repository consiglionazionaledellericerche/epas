/**
 * 
 */
package models;



import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
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
import models.rendering.VacationsRecap;
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
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<InitializationAbsence> initializationAbsences = new ArrayList<InitializationAbsence>();

	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<InitializationTime> initializationTimes = new ArrayList<InitializationTime>();

	/**
	 * relazione con la tabella delle info di contatto
	 */
	@OneToOne(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE}, orphanRemoval=true)
	public ContactData contactData;
	
	/**
	 * relazione con la tabella delle info di contatto
	 */
	@OneToOne(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public PersonHourForOvertime personHourForOvertime;

	/**
	 * relazione con la tabella dei contratti
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.EAGER, cascade = {CascadeType.REMOVE})
	public List<Contract> contracts = new ArrayList<Contract>(); 

	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<StampProfile> stampProfiles = new ArrayList<StampProfile>();

	/**
	 * relazione con la tabella delle tipologie di orario di lavoro
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="working_time_type_id")
	public WorkingTimeType workingTimeType;
	
	/**
	 * relazione con la tabella delle eventuali sedi distaccate
	 */
//	@NotAudited
//	@ManyToOne(fetch=FetchType.LAZY)
//	@JoinColumn(name="remote_office_id", nullable=true)
//	public RemoteOffice remoteOffice;

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
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<PersonChildren> personChildren;

	/**
	 * relazione con la nuova tabella dei person day
	 */
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<PersonDay> personDays;

	/**
	 * relazione con la nuova tabella dei person_month
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<PersonMonth> personMonths = new ArrayList<PersonMonth>();

	/**
	 * relazione con la nuova tabella dei person_year
	 */
	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<PersonYear> personYears;

	/**
	 * relazione con la tabella di storico YearRecap
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<YearRecap> yearRecaps;



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
	

	/**
	 * relazione con la tabella delle competence valide
	 */
	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<ValuableCompetence> valuableCompetences;

	/**
	 * relazione con la tabella delle locazioni degli utenti
	 */
	@NotAudited
	@OneToOne(mappedBy="person", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE}, orphanRemoval=true)
	public Location location;


	@OneToOne(mappedBy="person", fetch=FetchType.LAZY,  cascade = {CascadeType.REMOVE} )
	public PersonReperibility reperibility;

	@ManyToOne( fetch=FetchType.EAGER )
	@JoinColumn(name="qualification_id")
	public Qualification qualification;

	@OneToOne(mappedBy="person", fetch=FetchType.LAZY,  cascade = {CascadeType.REMOVE})
	public PersonShift personShift;

	
	
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
	
	
	/*
	public WorkingTimeType getWorkingTimeType(){
		return this.workingTimeType;
	}
	*/
	
	
	
//	@NotAudited
//	@ManyToOne(fetch=FetchType.LAZY)
//	@JoinColumn(name="remote_office_id", nullable=true)
//	public RemoteOffice remoteOffice;

	public String fullName() {
		return String.format("%s %s", surname, name);
	}

	
	/**
	 * 
	 * @return il piano ferie associato al contratto a sua volta associato alla data di oggi
	 */
	public VacationCode getVacation(){
		
		Contract contract = this.getCurrentContract();
		if(contract==null)
			return null;
		
		VacationPeriod vp = contract.getCurrentVacationPeriod();
		if(vp==null)
			return null;
		
		return vp.vacationCode;
		
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

		Contract contract = Contract.find("Select con from Contract con where con.person = ? and (con.beginContract IS NULL or con.beginContract <= ?) and (con.expireContract > ? or con.expireContract is null )",
				this,
				date,
				date).first();
		if(contract == null){
			return null;
		}
		return contract;

	}
	/**
	 * 
	 * @return il contratto attualmente attivo per quella persona, null se la persona non ha contratto attivo
	 */
	public Contract getCurrentContract(){
		return getContract(LocalDate.now());
	}
	
	/**
	 * La lista dei contratti in essere per la persona in month
	 * @param month
	 * @param year
	 * @return
	 */
	public List<Contract> getMonthContracts(Integer month, Integer year)
	{
		List<Contract> monthContracts = new ArrayList<Contract>();
		List<Contract> contractList = Contract.find("Select con from Contract con where con.person = ?",this).fetch();
		if(contractList == null){
			return null;
		}
		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();
		DateInterval monthInterval = new DateInterval(monthBegin, monthEnd);
		for(Contract contract : contractList)
		{
			DateInterval contractInterval = new DateInterval(contract.beginContract, contract.expireContract);
			if(DateUtility.intervalIntersection(monthInterval, contractInterval)!=null)
			{
				monthContracts.add(contract);
			}
		}
		if(monthContracts.size()==0)
			return null;
		
		return monthContracts;
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
	 * True se la persona alla data ha un contratto attivo, False altrimenti
	 * @param date
	 */
	public boolean isActive(LocalDate date)
	{
		Contract c = this.getCurrentContract();
		if(c==null)
			return false;
		else
			return true;
	}
	
	/**
	 *  La lista delle persone che abbiano almeno un giorno lavorativo coperto da contratto nel mese month
	 *  ordinate per id
	 * @param month
	 * @param year
	 * @return
	 */
	public static List<Person> getActivePersonsInMonth(int month, int year)
	{
		/**
		 * FIXME: rivedere le select in modo da renderle più efficienti
		 */
		List<Person> persons = Person.find("SELECT p FROM Person p ORDER BY p.surname, p.othersSurnames, p.name").fetch();
		List<Person> activePersons = new ArrayList<Person>();
		for(Person person : persons)
		{
			List<Contract> monthContracts = person.getMonthContracts(month, year);
			if(monthContracts!=null)
				activePersons.add(person);
		}
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
	 * 
	 * @return la lista delle persone che sono state selezionate per far parte della sperimentazione del nuovo sistema delle presenze
	 */
	public static List<Person> getPeopleForTest(){
		List<Person> peopleForTest = Person.find("Select p from Person p where p.surname in (?,?,?,?,?,?) or (p.name = ? and p.surname = ?)", 
				"Vasarelli", "Lucchesi", "Vivaldi", "Del Soldato", "Sannicandro", "Ruberti", "Maurizio", "Martinelli").fetch();
		return peopleForTest;
		
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
			stamp.stampType = stamping.stampType;
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
				stamp.stampType = stamping.stampType;
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

	/**
	 * Cerca per numero di matricola
	 * @param number
	 * @return
	 */
	public static Person findByNumber(Integer number) {
		return Person.find("SELECT p FROM Person p WHERE number = ?", number).first();
	}

}
