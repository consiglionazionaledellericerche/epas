/**
 * 
 */
package models;



import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

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
import javax.persistence.Transient;
import javax.persistence.Version;

import manager.recaps.PersonResidualMonthRecap;
import models.MealTicket.BlockMealTicket;
import models.Stamping.WayType;
import models.base.BaseModel;
import models.exports.StampingFromClient;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.validation.Email;
import play.data.validation.Required;
import play.mvc.With;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import controllers.Secure;
import controllers.Security;

/**
 * @author cristian
 *
 */
@Entity
@Audited
@Table(name = "persons")
@With(Secure.class)
public class Person extends BaseModel implements Comparable<Person>{

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

	@Column(name = "birthday")
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate birthday;
	
	@Column(name = "born_date")
	public Date bornDate;

	@Email
	public String email;

	@OneToOne(fetch=FetchType.LAZY)
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

	@NotAudited
	@OneToMany(mappedBy="person", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<StampProfile> stampProfiles = new ArrayList<StampProfile>();




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

	@OneToMany(mappedBy="person", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<MealTicket> mealTickets;
	
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
	
	//@NotAudited
	@ManyToOne
	@JoinColumn(name="office_id")	
	public Office office;
	
	
	
	/**
	 * Variabili Transienti LAZY (caricate quando vengono acceduti tramite i getter Transienti definiti
	 */
	@Transient
	private Contract currentContract = null;
	
	@Transient
	private WorkingTimeType currentWorkingTimeType = null;
	
	@Transient
	private VacationCode currentVacationCode = null;
	
	
	
	
	
	
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
	
	
	public String fullName() {
		return String.format("%s %s", surname, name);
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
	 * @return il contratto attivo per quella persona alla date date
	 */
	public Contract getContract(LocalDate date){
		
		for(Contract c : this.contracts)
		{
			if(DateUtility.isDateIntoInterval(date, c.getContractDateInterval()))
				return c;
		}
		
		//FIXME sommani aprile 2014, lui ha due contratti ma nello heap ce ne sono due identici e manca quello nuovo.
		List<Contract> contractList = Contract.find("Select c from Contract c where c.person = ?", this).fetch();
		//this.contracts = contractList;
		for(Contract c : contractList)
		{
			if(DateUtility.isDateIntoInterval(date, c.getContractDateInterval()))
				return c;
		}
		//-----------------------
		
		
		return null;

	}
	
	/**
	 * Cerca nella variabile LAZY il contratto attuale
	 * @return il contratto attualmente attivo per quella persona, null se la persona non ha contratto attivo
	 */
	@Transient
	public Contract getCurrentContract(){
		if(this.currentContract!=null)
			return this.currentContract;
		
		this.currentContract = getContract(LocalDate.now()); 
		return this.currentContract;
	}
	
	
	/**
	 * Cerca nella variabile LAZY il tipo orario attuale.
	 * @return l'attuale orario di lavoro 
	 */
	@Transient
	public  WorkingTimeType getCurrentWorkingTimeType(){
		if(this.currentWorkingTimeType!=null) {
			return this.currentWorkingTimeType;
		}
		
		if(this.currentContract==null) {
			this.currentContract = getContract(LocalDate.now()); 
		}
		if(this.currentContract==null)
			return null;
		
		//ricerca
		for(ContractWorkingTimeType cwtt : this.currentContract.contractWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(LocalDate.now(), new DateInterval(cwtt.beginDate, cwtt.endDate)))
			{
				this.currentWorkingTimeType = cwtt.workingTimeType;
				return currentWorkingTimeType;
			}
		}
		return null;
		
	}
	
	/**
	 * Cerca nella variabile LAZY il piano ferie attuale.
	 * @return il piano ferie attivo per la persona
	 */
	@Transient
	public VacationCode getCurrentVacationCode() {
		
		if(this.currentVacationCode!=null)
			return this.currentVacationCode;
		
		if(this.currentContract==null) {
			this.currentContract = getContract(LocalDate.now()); 
		}
		if(this.currentContract==null)
			return null;
		
		//ricerca
		for(VacationPeriod vp : this.currentContract.vacationPeriods)
		{
			if(DateUtility.isDateIntoInterval(LocalDate.now(), new DateInterval(vp.beginFrom, vp.endTo)))
			{
				this.currentVacationCode = vp.vacationCode;
				return this.currentVacationCode;
			}
		}
		return null;
	}

	/**
	 * 
	 * @param date
	 * @return il tipo di orario di lavoro utilizzato in date
	 */
	public  WorkingTimeType getWorkingTimeType(LocalDate date) {
		Contract contract = this.getContract(date);
		if(contract==null)
			return null;
		for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate)))
			{
				return cwtt.workingTimeType;
			}
		}
		return null;
	}

	/**
	 * True se la persona ha almeno un contratto attivo in month
	 * @param month
	 * @param year
	 * @return
	 */
	public boolean hasMonthContracts(Integer month, Integer year)
	{
		//TODO usare getMonthContracts e ritornare size>0
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
	 * 
	 * @param month
	 * @param year
	 * @return
	 */
	public List<Contract> getMonthContracts(Integer month, Integer year)
	{
		List<Contract> monthContracts = new ArrayList<Contract>();
		List<Contract> contractList = Contract.find("Select con from Contract con where con.person = ? order by con.beginContract",this).fetch();
		if(contractList == null){
			return monthContracts;
		}
		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();
		DateInterval monthInterval = new DateInterval(monthBegin, monthEnd);
		for(Contract contract : contractList)
		{
			if(!contract.onCertificate)
				continue;
			DateInterval contractInterval = contract.getContractDateInterval();
			if(DateUtility.intervalIntersection(monthInterval, contractInterval)!=null)
			{
				monthContracts.add(contract);
			}
		}
		return monthContracts;
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
	 * 
	 * @return la lista delle sedi visibili alla persona che ha chiamato il metodo
	 */
	public List<Office> getOfficeAllowed(){
		
		List<Office> officeList = new ArrayList<Office>();
		officeList.add(this.office);
		if(!this.office.subOffices.isEmpty()){
			
			for(Office office : this.office.subOffices){
				officeList.add(office);
			}
		}
		
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
			if(office.id.equals(this.office.id))
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
		Contract c = this.getContract(date);
		if(c==null)
			return false;
		else
			return true;
	}

	/**
	 *  true se la persona ha almeno un giorno lavorativo coperto da contratto nel mese month
	 * @param month
	 * @param year
	 * @param onCertificateFilter true se si vuole filtrare solo i dipendenti con certificati attivi 
	 * @return 
	 */
	public boolean isActiveInMonth(int month, int year, boolean onCertificateFilter)
	{
		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();
		return this.isActiveInPeriod(monthBegin, monthEnd, onCertificateFilter);
	}

	/**
	 * true se la persona ha almeno un giorno lavorativo coperto da contratto in year
	 * @param year
	 * @param onCertificateFilter true se si vuole filtrare solo i dipendenti con certificati attivi 
	 * @return
	 */
	public boolean isActiveInYear(int year, boolean onCertificateFilter)
	{
		LocalDate yearBegin = new LocalDate().withYear(year).withMonthOfYear(1).withDayOfMonth(1);
		LocalDate yearEnd = new LocalDate().withYear(year).withMonthOfYear(12).dayOfMonth().withMaximumValue();
		return this.isActiveInPeriod(yearBegin, yearEnd, onCertificateFilter);
	}

	
	/**
	 * 
	 * @param startPeriod
	 * @param endPeriod
	 * @param officeAllowed
	 * @param onlyTechnician true se voglio solo i tecnici con qualifica <= 3
	 * @return
	 */
	public static List<Person> getActivePersonsSpeedyInPeriod(LocalDate startPeriod, LocalDate endPeriod, List<Office> officeAllowed, boolean onlyTechnician)
	{
		//Filtro sulla qualifica
		List<Qualification> qualificationRequested;
		if(onlyTechnician)
			qualificationRequested = Qualification.find("Select q from Qualification q where q.qualification >= ?", 4).fetch();
		else
			qualificationRequested = Qualification.findAll();
				
		//Query //TODO QueryDsl
		List<Person> personList = Person.find("Select distinct p from Person p "
//				+ "left outer join fetch p.contactData "				//OneToOne			//TODO ISSUE discutere dell'opzionalità di queste relazioni OneToOne
				+ "left outer join fetch p.personHourForOvertime "		//OneToOne
//				+ "left outer join fetch p.location "					//OneToOne
				+ "left outer join fetch p.reperibility "				//OneToOne
				+ "left outer join fetch p.personShift "				//OneToOne 
				+ "left outer join fetch p.user "						//OneToOne 
				+ "left outer join fetch p.contracts as c "
				+ "where "
				
				//utenti di sistema
				//+"p.username != ? "
				
				//contratto on certificate
				//+ "and c.onCertificate = true " tolto and perchè ho tolto username
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
				
								
				+ "order by p.surname, p.name", /*"epas.clocks",*/ endPeriod, endPeriod, startPeriod, endPeriod, startPeriod).bind("officeList", officeAllowed).bind("qualificationList", qualificationRequested).fetch();

		return personList;
	}
	
	/**
	 * La lista delle persone attive in uno specifico giorno
	 * @param day
	 * @param month
	 * @param year
	 * @param officeAllowed
	 * @param onlyTechnician true se si desiderano solo tecnici, false altrimenti
	 * @return
	 */
	public static List<Person> getActivePersonsInDay(int day, int month, int year, List<Office> officeAllowed, boolean onlyTechnician)
	{
		LocalDate date = new LocalDate(year, month, day);
		return Person.getActivePersonsSpeedyInPeriod(date, date, officeAllowed, onlyTechnician);
	}
	
	/**
	 * La lista delle persone attive in uno specifico giorno
	 * @param day
	 * @param officeAllowed
	 * @param onlyTechnician
	 * @return
	 */
	public static List<Person> getActivePersonsInDay(LocalDate day, List<Office> officeAllowed, boolean onlyTechnician)
	{	
		return Person.getActivePersonsSpeedyInPeriod(day, day, officeAllowed, onlyTechnician);
	}

	/**
	 * La lista delle persone che abbiano almeno un giorno lavorativo coperto da contratto nel mese month
	 * @param month
	 * @param year
	 * @param officeAllowed
	 * @param onlyTechnician true se si desiderano solo tecnici, false altrimenti
	 * @return
	 */
	public static List<Person> getActivePersonsInMonth(int month, int year, List<Office> officeAllowed, boolean onlyTechnician)
	{

		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();
		return Person.getActivePersonsSpeedyInPeriod(monthBegin, monthEnd, officeAllowed, onlyTechnician);
	}

	/**
	 * La lista delle persone che abbiano almeno un giorno lavorativo coperto da contratto nell'anno year
	 * @param officeAllowed
	 * @param year, onlyTechnician
	 * @return le persone attive in un anno se il booleano è true ritorna solo la lista dei tecnici (per competenze)
	 */
	public static List<Person> getActivePersonsinYear(int year, List<Office> officeAllowed, boolean onlyTechnician){

		LocalDate yearBegin = new LocalDate().withYear(year).withMonthOfYear(1).withDayOfMonth(1);
		LocalDate yearEnd = new LocalDate().withYear(year).withMonthOfYear(12).dayOfMonth().withMaximumValue();
		return Person.getActivePersonsSpeedyInPeriod(yearBegin, yearEnd, officeAllowed, onlyTechnician);
	
	}
	
	/**
	 * 
	 * @param startPeriod
	 * @param endPeriod
	 * @param onCertificateFilter true se si vuole filtrare solo i dipendenti con certificati attivi 
	 * @return
	 */
	private boolean isActiveInPeriod(LocalDate startPeriod, LocalDate endPeriod, boolean onCertificateFilter)
	{
		List<Contract> periodContracts = new ArrayList<Contract>();
		DateInterval periodInterval = new DateInterval(startPeriod, endPeriod);
		for(Contract contract : this.contracts)
		{
			if(onCertificateFilter && !contract.onCertificate)
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
	 * True se il giorno passato come argomento è festivo per la persona. False altrimenti.
	 * @param date
	 * @return
	 */
	public boolean isHoliday(LocalDate date)
	{
		if(DateUtility.isGeneralHoliday(this.office, date))
			return true;
		
		Contract contract = this.getContract(date);
		if(contract == null)
		{
			//persona fuori contratto
			return false;
		}
			
		for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate)))
			{
				return cwtt.workingTimeType.getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek()).holiday;
			}
		}
		
		return false;	//se il db è consistente non si verifica mai
		
	}
	
	@Override
	public String toString() {
		return String.format("Person[%d] - %s %s", id, name, surname);
	}

	/**
	 * Metodi di utilità per verificare se nella lista dei permessi c'è il permesso richiesto. Utile in visualizzazione
	 * 
	 */

	
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
	 * Cerca per numero di matricola
	 * @param number
	 * @return
	 */
	public static Person findByNumber(Integer number) {
		return Person.find("SELECT p FROM Person p WHERE number = ?", number).first();
	}
	
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @return l'esito dell'invio attestati per la persona (null se non è ancora stato effettuato)
	 */
	public CertificatedData getCertificatedData(int year, int month)
	{
		CertificatedData cd = CertificatedData.find("Select cd from CertificatedData cd where cd.person = ? and cd.year = ? and cd.month = ?",
				this, year, month).first();
		return cd;
	}
	
	public List<BlockMealTicket> getBlockMealTicket() {
		
		List<MealTicket> mealTicketList = MealTicket.find("Select mt from MealTicket mt "
				+ "where mt.person = ? order by mt.block",
				this).fetch();
		
		List<BlockMealTicket> blockList = Lists.newArrayList();
		
		if(mealTicketList.size() == 0)
			return blockList;
		
		BlockMealTicket currentBlock = null;
		
		for(MealTicket mealTicket : mealTicketList) {
			
			if(currentBlock == null) {
				currentBlock = new BlockMealTicket(mealTicket.block);
				currentBlock.mealTickets.add(mealTicket);
				continue;
			}	
				
			if( !currentBlock.codeBlock.equals(mealTicket.block) ) {
				blockList.add(currentBlock);
				currentBlock = new BlockMealTicket(mealTicket.block);
			}
			
			currentBlock.mealTickets.add(mealTicket);
		}
		
		blockList.add(currentBlock);
		
		return blockList;
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
	
	public boolean isIonicRadianceRiskCom3AvailableBis(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("304"))
				flag = true;
		}
		return flag;
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
		User user = Security.getUser().get();
					
		if(user.person==null)
		{
			return Person.findAll();
		}
		//tutte le persone (l'amministratore è amministratore di sede principale)
		if(user.person.office.subOffices.isEmpty())
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
					"order by p.surname, p.name", user.person.office, date, date, date, "epas.clocks").fetch();
		}
		
		return activePersons;
	
	}

	@Override
	public int compareTo(Person person) {
				
		int res = (this.surname.compareTo(person.surname) == 0) ?  this.name.compareTo(person.name) :  this.surname.compareTo(person.surname);
		return res;
	}

}
