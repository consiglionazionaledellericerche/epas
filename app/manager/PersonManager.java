package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.PersonChildren;
import models.PersonYear;
import models.User;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonChildrenDao;

public class PersonManager {

	private final static Logger log = LoggerFactory.getLogger(PersonManager.class);
	@Inject
	public OfficeDao officeDao;
	
	/**
	 * True se la persona ha almeno un contratto attivo in month
	 * @param month
	 * @param year
	 * @return
	 */
	public boolean hasMonthContracts(Person person, Integer month, Integer year)
	{
		//TODO usare getMonthContracts e ritornare size>0
		List<Contract> monthContracts = new ArrayList<Contract>();
		List<Contract> contractList = ContractDao.getPersonContractList(person);
		//List<Contract> contractList = Contract.find("Select con from Contract con where con.person = ?",this).fetch();
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
	public boolean hasYearContracts(Person person, Integer year)
	{
		for(int month=1; month<=12; month++)
		{
			if(hasMonthContracts(person,month, year))
				return true;
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param administrator
	 * @return true se la persona è visibile al parametro amministratore
	 */
	public boolean isAllowedBy(User admin, Person person)
	{

		Set<Office> adminOffices = officeDao.getOfficeAllowed(admin);
		Set<Office> officeAllowed = officeDao.getOfficeAllowed(person.user);
		
		return adminOffices.contains(officeAllowed.iterator().next());
		
		/*
		//List<Office> officeAllowed = administrator.getOfficeAllowed();
		Set<Office> officeAllowed = officeDao.getOfficeAllowed(Optional.of(person.user));
		for(Office office : officeAllowed)
		{
			if(office.id.equals(administrator.office.id))
				return true;
		}
		return false;
		*/

	}
	
	/**
	 * True se la persona alla data ha un contratto attivo, False altrimenti
	 * @param date
	 */
	public static boolean isActiveInDay(LocalDate date, Person person)
	{
		//Contract c = this.getContract(date);
		Contract c = ContractDao.getContract(date, person);
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
	public static boolean isActiveInMonth(Person person, int month, int year, boolean onCertificateFilter)
	{
		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();
		return isActiveInPeriod(person, monthBegin, monthEnd, onCertificateFilter);
	}
	
	
	/**
	 * true se la persona ha almeno un giorno lavorativo coperto da contratto in year
	 * @param year
	 * @param onCertificateFilter true se si vuole filtrare solo i dipendenti con certificati attivi 
	 * @return
	 */
	public static boolean isActiveInYear(Person person, int year, boolean onCertificateFilter)
	{
		LocalDate yearBegin = new LocalDate().withYear(year).withMonthOfYear(1).withDayOfMonth(1);
		LocalDate yearEnd = new LocalDate().withYear(year).withMonthOfYear(12).dayOfMonth().withMaximumValue();
		return isActiveInPeriod(person, yearBegin, yearEnd, onCertificateFilter);
	}
	
	
	
	/**
	 * 
	 * @param startPeriod
	 * @param endPeriod
	 * @param onCertificateFilter true se si vuole filtrare solo i dipendenti con certificati attivi 
	 * @return
	 */
	private static boolean isActiveInPeriod(Person person, LocalDate startPeriod, LocalDate endPeriod, boolean onCertificateFilter)
	{
		List<Contract> periodContracts = new ArrayList<Contract>();
		DateInterval periodInterval = new DateInterval(startPeriod, endPeriod);
		for(Contract contract : person.contracts)
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
	public static boolean isHoliday(Person person, LocalDate date)
	{
		if(DateUtility.isGeneralHoliday(person.office, date))
			return true;
		
		//Contract contract = this.getContract(date);
		Contract contract = ContractDao.getContract(date, person);
		if(contract == null)
		{
			//persona fuori contratto
			return false;
		}
			
		for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate)))
			{
				return cwtt.workingTimeType.workingTimeTypeDays.get(date.getDayOfWeek()-1).holiday;
			}
		}
		
		return false;	//se il db è consistente non si verifica mai
		
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
	 * 
	 * @param month
	 * @param year
	 * @return
	 */
	public List<Contract> getMonthContracts(Person person, Integer month, Integer year)
	{
		List<Contract> monthContracts = new ArrayList<Contract>();
		List<Contract> contractList = ContractDao.getPersonContractList(person);
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
	 * 
	 * @param month
	 * @param year
	 * @return
	 */
	@Deprecated
	public static List<Contract> getMonthContractsStatic(Person person, Integer month, Integer year)
	{
		List<Contract> monthContracts = new ArrayList<Contract>();
		List<Contract> contractList = ContractDao.getPersonContractList(person);
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
	
	/**
	 * utilizzata nel metodo delete del controller Persons per cancellare gli eventuali figli della persona passata come parametro
	 * @param person
	 */
	public static void deletePersonChildren(Person person){
		for(PersonChildren pc : person.personChildren){
			long id = pc.id;
			log.debug("Elimino figli di {}", person.getFullname());
			pc = PersonChildrenDao.getById(id);
			pc.delete();
		}
	}
	
	/**
	 * Utilizzato nel metodo delete del controller Persons per eleminare turni, reperibilità, ore di formazione e riepiloghi annuali
	 * per la persona person
	 * @param person
	 */
	public static void deleteShiftReperibilityTrainingHoursAndYearRecap(Person person){
		if(person.personHourForOvertime != null)
			person.personHourForOvertime.delete();
		if(person.personShift != null)
			person.personShift.delete();
		if(person.reperibility != null)
			person.reperibility.delete();
		for(PersonYear py : person.personYears){
			py.delete();
		}
	}
}
