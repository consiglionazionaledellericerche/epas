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
import javax.persistence.JoinColumns;
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
import models.personalMonthSituation.Mese;
import models.rendering.VacationsRecap;
import net.spy.memcached.FailureMode;

import org.apache.commons.lang.time.DateUtils;
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
import org.joda.time.LocalDateTime;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import controllers.Check;
import controllers.Secure;
import controllers.Security;
import play.Logger;
import play.cache.Cache;
import play.data.binding.As;
import play.data.validation.Email;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Scope.Params;
import play.mvc.Scope.Session;
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
	@OneToOne(mappedBy="person", fetch = FetchType.EAGER, cascade = {CascadeType.REMOVE}, orphanRemoval=true, optional=true)
	public ContactData contactData;
	
	
	@OneToOne(mappedBy="person", fetch = FetchType.EAGER, cascade = {CascadeType.REMOVE})
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
//	@ManyToOne(fetch=FetchType.LAZY)
//	@JoinColumn(name="working_time_type_id")
//	public WorkingTimeType workingTimeType;
	
	@NotAudited
	@OneToMany(mappedBy = "person", fetch=FetchType.LAZY)
	public List<PersonWorkingTimeType> personWorkingTimeType = new ArrayList<PersonWorkingTimeType>();
	
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
	@OneToOne(mappedBy="person", fetch=FetchType.EAGER, cascade = {CascadeType.REMOVE}, orphanRemoval=true)
	public Location location;


	@OneToOne(mappedBy="person", fetch=FetchType.EAGER,  cascade = {CascadeType.REMOVE} )
	public PersonReperibility reperibility;

	@ManyToOne( fetch=FetchType.LAZY )
	@JoinColumn(name="qualification_id")
	public Qualification qualification;

	@OneToOne(mappedBy="person", fetch=FetchType.EAGER,  cascade = {CascadeType.REMOVE})
	public PersonShift personShift;
	
	//@NotAudited
	@ManyToOne( fetch=FetchType.LAZY )
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
	 * @return la qualifica della persona
	 */
	public Qualification getQualification(){
		if(this.qualification != null)
			return this.qualification;
		else
			return null;
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
		return contract;

	}
	
	/**
	 * Il contratto attivo alla data, se esiste. Ciclando su tutti i contratti della persona (no query sql)
	 * Da utilizzare se si intende ripetere la query per la persona su più giorni
	 * @param date
	 * @return
	 */
	public Contract getContractFromHeap(LocalDate date)
	{
		for(Contract c : this.contracts)
		{
			if(DateUtility.isDateIntoInterval(date, new DateInterval(c.beginContract, c.expireContract)))
				return c;
		}
		return null;
	}
	/**
	 * 
	 * @return il contratto attualmente attivo per quella persona, null se la persona non ha contratto attivo
	 */
	public Contract getCurrentContract(){
		return getContract(LocalDate.now());
	}
	
	
	/**
	 * True se la persona ha almeno un contratto attivo in month
	 * @param month
	 * @param year
	 * @return
	 */
	public boolean hasMonthContracts(Integer month, Integer year)
	{
		List<Contract> monthContracts = new ArrayList<Contract>();
		List<Contract> contractList = Contract.find("Select con from Contract con where con.person = ?",this).fetch();
		if(contractList == null){
			return false;
		}
		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();
		DateInterval monthInterval = new DateInterval(monthBegin, monthEnd);
		for(Contract contract : contractList)
		{
			if(!contract.onCertificate)
				continue;
			DateInterval contractInterval = new DateInterval(contract.beginContract, contract.expireContract);
			if(DateUtility.intervalIntersection(monthInterval, contractInterval)!=null)
			{
				monthContracts.add(contract);
			}
		}
		if(monthContracts.size()==0)
			return false;
		
		return true;
	}
	
	/**
	 * True se la persona ha almeno un contratto attivo in year
	 * @param year
	 * @return
	 */
	public boolean hasYearContracts(Integer year)
	{
		for(int month=1; month<=12; month++)
		{
			if(this.hasMonthContracts(month, year))
				return true;
		}
		return false;
	}
	
	

	/**
	 * Ritorna la lista delle persone visibili dall'amministratore attive nel mese richiesto.
	 * Questa lista viene salvata in cache e ricalcolata solo se la copia non esiste o è scaduta.
	 * Il nome della variabile in cache è persons-year-month-personLogged.id (esempio 'persons-2014-01-146')
	 * @param year
	 * @return
	 
	public static List getCachedActivePersonInMonth(Integer year, Integer month, Person personLogged)
	{
		return null;
	}
	*/
	
	/**
	 * Metodo deprecato, usare getActivePersonsInDay
	 * @param date
	 * @return la lista di persone attive a quella data
	 */
	@Deprecated 
	public static List<Person> getActivePersons(LocalDate date){
		List<Person> activePersons = null;
		Person person = Security.getPerson();
		
		//tutte le persone (l'amministratore è amministratore di sede principale)
		if(person.office.remoteOffices.isEmpty())
		{
			//List<Person> personOffice = new ArrayList<Person>();
			
			activePersons = Person.find(
					"Select distinct (p) " +
					"from Person p, Contract c " +
					"where c.person = p "
					+ "and (c.endContract is null or c.endContract > ?) "
					+ "and (c.expireContract > ? or c.expireContract is null) "
					+ "and (c.beginContract < ? or c.beginContract is null) "
					+ "and p.username <> ? " + 
					"order by p.surname, p.name", date, date, date, "epas.clocks").fetch();
			
		}
		//le persone aderenti all'ufficio dell'amministratore, che è amministratore di sede distaccata
		else
		{
			activePersons =Person.find(
					"Select distinct (p) " +
					"from Person p, Contract c " +
					"where c.person = p " +
					"and p.office = ?" 
					+ "and (c.endContract is null or c.endContract > ?) "
					+ "and (c.expireContract > ? or c.expireContract is null) "
					+ "and (c.beginContract < ? or c.beginContract is null) "
					+ "and p.username <> ? " + 
					"order by p.surname, p.name", person.office, date, date, date, "epas.clocks").fetch();
		}
		
		return activePersons;
	
	}

	/**
	 * 
	 * @return la lista delle sedi visibili alla persona che ha chiamato il metodo
	 */
	public List<Office> getOfficeAllowed(){
		
		if(this.username.equals("admin"))
			return Office.findAll();
		
		List<Office> officeList = new ArrayList<Office>();
		if(!this.office.remoteOffices.isEmpty()){
			
			for(Office office : this.office.remoteOffices){
				officeList.add(office);
			}
		}
		officeList.add(this.office);
		return officeList;
	}
	
	/**
	 * 
	 * @param administrator
	 * @return true se la persona è visibile al parametro amministratore
	 */
	public boolean isAllowedBy(Person administrator)
	{
		List<Office> officeAllowed = administrator.getOfficeAllowed();
		for(Office office : officeAllowed)
		{
			if(office.id == this.office.id)
				return true;
		}
		return false;
	}
	

	/**
	 * True se la persona alla data ha un contratto attivo, False altrimenti
	 * @param date
	 */
	public boolean isActiveInDay(LocalDate date)
	{
		Contract c = this.getCurrentContract();
		if(c==null)
			return false;
		else
			return true;
	}

	/**
	 *  true se la persona ha almeno un giorno lavorativo coperto da contratto nel mese month
	 * @param month
	 * @param year
	 * @return 
	 */
	public boolean isActiveInMonth(int month, int year)
	{
		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();
		return this.isActiveInPeriod(monthBegin, monthEnd);
	}

	/**
	 * true se la persona ha almeno un giorno lavorativo coperto da contratto in year
	 * @param year
	 * @return
	 */
	public boolean isActiveInYear(int year)
	{
		LocalDate yearBegin = new LocalDate().withYear(year).withMonthOfYear(1).withDayOfMonth(1);
		LocalDate yearEnd = new LocalDate().withYear(year).withMonthOfYear(12).dayOfMonth().withMaximumValue();
		return this.isActiveInPeriod(yearBegin, yearEnd);
	}

	
	/**
	 * 
	 * @param startPeriod
	 * @param endPeriod
	 * @param personLogged la persona loggata, cannot be null
	 * @param onlyTechnician true se voglio solo i tecnici con qualifica <= 3
	 * @return
	 */
	public static List<Person> getActivePersonsSpeedyInPeriod(LocalDate startPeriod, LocalDate endPeriod, Person personLogged, boolean onlyTechnician)
	{
		if(personLogged==null)
		{
			Logger.info("La lista delle persone attive visibili dall'amministratore e' vuota perchè personLogged e' null.");
			return new ArrayList<Person>();
		}
		if(personLogged.name.equals("Admin"))
		{
			return Person.find("Select p from Person p order by p.surname, p.name").fetch();
		}

		//Filtro sulla sede
		List<Office> officeAllowed = personLogged.getOfficeAllowed();
				
		//Filtro sulla qualifica
		List<Qualification> qualificationRequested;
		if(onlyTechnician)
			qualificationRequested = Qualification.find("Select q from Qualification q where q.qualification >= ?", 4).fetch();
		else
			qualificationRequested = Qualification.findAll();
				
		//Query //TODO QueryDsl
		List<Person> personList = Person.find("Select distinct p from Person p "
				+ "left outer join fetch p.contactData "				//OneToOne			//TODO ISSUE discutere dell'opzionalità di queste relazioni OneToOne
				+ "left outer join fetch p.personHourForOvertime "		//OneToOne
				+ "left outer join fetch p.location "					//OneToOne
				+ "left outer join fetch p.reperibility "				//OneToOne
				+ "left outer join fetch p.personShift "				//OneToOne
				+ "left outer join fetch p.contracts as c "
				+ "where "
				
				//contratto on certificate
				+ "c.onCertificate = true "
				
				+ "and "
				
				
				//contratto attivo nel periodo
				+ "( "
				//caso contratto non terminato
				+ "c.endContract is null and "
					//contratto a tempo indeterminato che si interseca col periodo 
					+ "( (c.expireContract is null and c.beginContract <= ? )"
					+ "or "
					//contratto a tempo determinato che si interseca col periodo (comanda il campo endContract)
					+ "(c.expireContract is not null and c.beginContract <= ? and c.expireContract >= ? ) ) "
				+ "or "
				//caso contratto terminato che si interseca col periodo		
				+ "c.endContract is not null and c.beginContract <= ? and c.endContract >= ? "
				+ ") "
				
				//persona allowed
				+"and p.office in :officeList "
				
				//only technician
				+"and p.qualification in :qualificationList "
								
				+ "order by p.surname, p.name", endPeriod, endPeriod, startPeriod, endPeriod, startPeriod).bind("officeList", officeAllowed).bind("qualificationList", qualificationRequested).fetch();

		return personList;
	}
	
	/**
	 * La lista delle persone attive in uno specifico giorno
	 *  sulle quali l'amministratore loggato detiene i diritti di amministrazione.
	 * @param day
	 * @param month
	 * @param year
	 * @param onlyTechnician true se si desiderano solo tecnici, false altrimenti
	 * @return
	 */
	public static List<Person> getActivePersonsInDay(int day, int month, int year, boolean onlyTechnician)
	{
		LocalDate date = new LocalDate(year, month, day);
		Person personLogged = Security.getPerson();
		return Person.getActivePersonsSpeedyInPeriod(date, date, personLogged, onlyTechnician);
	}
	
	/**
	 * La lista delle persone attive in uno specifico giorno
	 *  sulle quali l'amministratore loggato detiene i diritti di amministrazione.
	 *   Se l'amministratore loggato è null (job automatico) ritorna l'elenco completo delle persone attive senza restrizioni sulla sede.
	 * @param day
	 * @param onlyTechnician
	 * @return
	 */
	public static List<Person> getActivePersonsInDay(LocalDate day, boolean onlyTechnician)
	{	
		Person personLogged = Security.getPerson();
		return Person.getActivePersonsSpeedyInPeriod(day, day, personLogged, onlyTechnician);
	}

	/**
	 * La lista delle persone che abbiano almeno un giorno lavorativo coperto da contratto nel mese month
	 *  sulle quali l'amministratore loggato detiene i diritti di amministrazione.
	 *   Se l'amministratore loggato è null (job automatico) ritorna l'elenco completo delle persone attive senza restrizioni sulla sede.
	 * @param month
	 * @param year
	 * @param onlyTechnician true se si desiderano solo tecnici, false altrimenti
	 * @return
	 */
	public static List<Person> getActivePersonsInMonth(int month, int year, boolean onlyTechnician)
	{

		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();
		Person personLogged = Security.getPerson();
		return Person.getActivePersonsSpeedyInPeriod(monthBegin, monthEnd, personLogged, onlyTechnician);
	}

	/**
	 * La lista delle persone che abbiano almeno un giorno lavorativo coperto da contratto nell'anno year
	 *  sulle quali l'amministratore loggato detiene i diritti di amministrazione.
	 *   Se l'amministratore loggato è null (job automatico) ritorna l'elenco completo delle persone attive senza restrizioni sulla sede.
	 * @param year, onlyTechnician
	 * @return le persone attive in un anno se il booleano è true ritorna solo la lista dei tecnici (per competenze)
	 */
	public static List<Person> getActivePersonsinYear(int year, boolean onlyTechnician){

		LocalDate yearBegin = new LocalDate().withYear(year).withMonthOfYear(1).withDayOfMonth(1);
		LocalDate yearEnd = new LocalDate().withYear(year).withMonthOfYear(12).dayOfMonth().withMaximumValue();
		Person personLogged = Security.getPerson();
		return Person.getActivePersonsSpeedyInPeriod(yearBegin, yearEnd, personLogged, onlyTechnician);
	
	}
	
	/**
	 * 
	 * @param startPeriod
	 * @param endPeriod
	 * @return
	 */
	private boolean isActiveInPeriod(LocalDate startPeriod, LocalDate endPeriod)
	{
		List<Contract> periodContracts = new ArrayList<Contract>();
		DateInterval periodInterval = new DateInterval(startPeriod, endPeriod);
		for(Contract contract : this.contracts)
		{
			if(!contract.onCertificate)
				continue;
			DateInterval contractInterval = new DateInterval(contract.beginContract, contract.expireContract); //TODO è sbagliato bisogna considerare anche endContract
			if(DateUtility.intervalIntersection(periodInterval, contractInterval)!=null)
			{
				periodContracts.add(contract);
			}
		}
		if(periodContracts.size()==0)
			return false;
		
		return true;
	}


	/**
	 * Ritorna la lista dei tecnici che beneficiano di competenze, attive alla data passata come argomento,
	 *  sulle quali l'amministratore detiene i diritti di amministrazione.
	 * @param date
	 * @return
	 */
	public static List<Person> getTechnicianForCompetences(LocalDate date){
		return getActivePersonsInDay(date.getDayOfMonth(), date.getMonthOfYear(), date.getYear(), true);
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

	public boolean isInsertAndUpdateOfficesAvailable(){
		for(Permission p : this.permissions){
			if(p.description.equals(Security.INSERT_AND_UPDATE_OFFICES))
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
		
		if(stamping.dateTime.isBefore(new LocalDateTime().minusMonths(1))){
			Logger.warn("La timbratura che si cerca di inserire è troppo precedente rispetto alla data odierna. Controllare il server!");
			return false;
		}
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
	 * @param code
	 * @param month
	 * @param year
	 * @return il totale per quel mese e quell'anno di ore/giorni relativi a quel codice competenza
	 */
	public int totalFromCompetenceCode(CompetenceCode code, int month, int year){
		int totale = 0;
		List<Competence> compList = Competence.find("Select comp from Competence comp where comp.competenceCode = ? " +
				"and comp.month = ? and comp.year = ?", code, month, year).fetch();
		for(Competence comp : compList){
			totale = totale+comp.valueApproved;
		}
		return totale;
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
	
	
	/**
	 * 
	 * @return l'attuale orario di lavoro
	 */
	public  WorkingTimeType getCurrentWorkingTimeType(){
		return getWorkingTimeType(LocalDate.now());
	}

	/**
	 * 
	 * @param date
	 * @return il tipo di orario di lavoro utilizzato in date
	 */
	public  WorkingTimeType getWorkingTimeType(LocalDate date) {
		for(PersonWorkingTimeType personWtt : this.personWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(date, new DateInterval(personWtt.beginDate, personWtt.endDate)))
			{
				return personWtt.workingTimeType;
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @return le ore di residuo positivo fatte nel mese/anno da this. Metodo usato nel template showCompetences
	 */
	public Integer getPositiveResidualInMonth(int year, int month){
		
		return Mese.positiveResidualInMonth(this, year, month)/60; 
	}

}
