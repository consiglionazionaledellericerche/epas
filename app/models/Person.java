/**
 * 
 */
package models;



import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
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

import manager.WorkingTimeTypeManager;
import manager.recaps.PersonResidualMonthRecap;
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

import controllers.Secure;
import controllers.Security;
import dao.ContractDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import dao.QualificationDao;

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
	 * Cerca nella variabile LAZY il contratto attuale
	 * @return il contratto attualmente attivo per quella persona, null se la persona non ha contratto attivo
	 */
	@Transient
	public Contract getCurrentContract(){
		if(this.currentContract!=null)
			return this.currentContract;
		
		//this.currentContract = getContract(LocalDate.now());
		this.currentContract = ContractDao.getContract(LocalDate.now(), this);
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
			//this.currentContract = getContract(LocalDate.now());
			this.currentContract = ContractDao.getContract(LocalDate.now(), this);
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
			//this.currentContract = getContract(LocalDate.now());
			this.currentContract = ContractDao.getContract(LocalDate.now(), this);
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
	 * @param month
	 * @param year
	 * @return
	 */
	public List<Contract> getMonthContracts(Integer month, Integer year)
	{
		List<Contract> monthContracts = new ArrayList<Contract>();
		List<Contract> contractList = ContractDao.getPersonContractList(this);
		//List<Contract> contractList = Contract.find("Select con from Contract con where con.person = ? order by con.beginContract",this).fetch();
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

	
	
	public boolean isIonicRadianceRiskCom3AvailableBis(){
		boolean flag = false;
		for(CompetenceCode code : this.competenceCode){
			if(code.code.equals("304"))
				flag = true;
		}
		return flag;
	}
	

	@Override
	public int compareTo(Person person) {
				
		int res = (this.surname.compareTo(person.surname) == 0) ?  this.name.compareTo(person.name) :  this.surname.compareTo(person.surname);
		return res;
	}

}
