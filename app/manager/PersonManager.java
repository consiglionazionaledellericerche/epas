package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.Query;

import models.AbsenceType;
import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.PersonChildren;
import models.PersonDay;
import models.PersonYear;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cnr.sync.dto.DepartmentDTO;
import cnr.sync.dto.PersonRest;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import play.Play;
import play.db.jpa.JPA;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import dao.AbsenceDao;
import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonChildrenDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;

public class PersonManager {

	@Inject
	public PersonManager(ContractDao contractDao, OfficeDao officeDao,
			PersonChildrenDao personChildrenDao, PersonDao personDao,
			PersonDayDao personDayDao, AbsenceDao absenceDao,
			PersonDayManager personDayManager,
			IWrapperFactory wrapperFactory,ConfGeneralManager confGeneralManager) {
		this.contractDao = contractDao;
		this.officeDao = officeDao;
		this.personChildrenDao = personChildrenDao;
		this.personDao = personDao;
		this.personDayDao = personDayDao;
		this.absenceDao = absenceDao;
		this.personDayManager = personDayManager;
		this.wrapperFactory = wrapperFactory;
	}

	private final static Logger log = LoggerFactory.getLogger(PersonManager.class);

	private final ContractDao contractDao;
	private final OfficeDao officeDao;
	private final PersonChildrenDao personChildrenDao;
	private final PersonDao personDao;
	private final PersonDayDao personDayDao;
	private final PersonDayManager personDayManager;
	private final IWrapperFactory wrapperFactory;
	private final AbsenceDao absenceDao;

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
		List<Contract> contractList = contractDao.getPersonContractList(person);
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
	}

	/**
	 * True se la persona alla data ha un contratto attivo, False altrimenti
	 * @param date
	 */
	public boolean isActiveInDay(LocalDate date, Person person)
	{
		//Contract c = this.getContract(date);
		Contract c = contractDao.getContract(date, person);
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
	public boolean isActiveInMonth(Person person, YearMonth yearMonth, 
			boolean onCertificateFilter) {
		
		LocalDate monthBegin = new LocalDate().withYear(yearMonth.getYear())
				.withMonthOfYear(yearMonth.getMonthOfYear()).withDayOfMonth(1);
		
		LocalDate monthEnd = new LocalDate().withYear(yearMonth.getYear())
				.withMonthOfYear(yearMonth.getMonthOfYear()).dayOfMonth().withMaximumValue();
		
		return isActiveInPeriod(person, monthBegin, monthEnd, onCertificateFilter);
	}


	/**
	 * true se la persona ha almeno un giorno lavorativo coperto da contratto in year
	 * @param year
	 * @param onCertificateFilter true se si vuole filtrare solo i dipendenti con certificati attivi 
	 * @return
	 */
	public boolean isActiveInYear(Person person, int year, boolean onCertificateFilter)
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
	 * 
	 * @return la lista delle persone che sono state selezionate per far parte della sperimentazione del nuovo sistema delle presenze
	 */
	public List<Person> getPeopleForTest(){
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
		List<Contract> contractList = contractDao.getPersonContractList(person);
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
			DateInterval contractInterval = wrapperFactory.create(contract).getContractDateInterval();
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
	public void deletePersonChildren(Person person){
		for(PersonChildren pc : person.personChildren){
			long id = pc.id;
			log.debug("Elimino figli di {}", person.getFullname());
			pc = personChildrenDao.getById(id);
			pc.delete();
		}
	}

	/**
	 * Utilizzato nel metodo delete del controller Persons per eleminare turni, reperibilità, ore di formazione e riepiloghi annuali
	 * per la persona person
	 * @param person
	 */
	public void deleteShiftReperibilityTrainingHoursAndYearRecap(Person person){
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

	/**
	 * 
	 * @return false se l'id passato alla funzione non trova tra le persone presenti in anagrafica, una che avesse nella vecchia applicazione un id
	 * uguale a quello che la sequence postgres genera automaticamente all'inserimento di una nuova persona in anagrafica.
	 * In particolare viene controllato il campo oldId presente per ciascuna persona e si verifica che non esista un valore uguale a quello che la 
	 * sequence postgres ha generato
	 */
	public boolean isIdPresentInOldSoftware(Long id){
		Person person = personDao.getPersonByOldID(id);
		//Person person = Person.find("Select p from Person p where p.oldId = ?", id).first();
		if(person == null)
			return false;
		else
			return true;

	}

	/**
	 * 
	 * @param person
	 * @param date
	 * @return true se in quel giorno quella persona non è in turno nè in reperibilità (metodo chiamato dal controller di inserimento assenza)
	 */
	public boolean canPersonTakeAbsenceInShiftOrReperibility(Person person, LocalDate date){
		Query queryReperibility = JPA.em().createQuery("Select count(*) from PersonReperibilityDay prd where prd.date = :date and prd.personReperibility.person = :person");
		queryReperibility.setParameter("date", date).setParameter("person", person);
		int prdCount = queryReperibility.getFirstResult();
		//	List<PersonReperibilityDay> prd =  queryReperibility.getResultList();
		if(prdCount != 0)
			return false;
		Query queryShift = JPA.em().createQuery("Select count(*) from PersonShiftDay psd where psd.date = :date and psd.personShift.person = :person");
		queryShift.setParameter("date", date).setParameter("person", person);
		int psdCount = queryShift.getFirstResult();
		if(psdCount != 0)
			return false;

		return true;
	}

	/**
	 * 
	 * @param date
	 * @return il personDay se la data passata è di un giorno feriale, null altrimenti
	 */
	public PersonDay createPersonDayFromDate(Person person, LocalDate date){
		//if(person.isHoliday(date))
		if(personDayManager.isHoliday(person, date)) {
			return null;
		}
		return new PersonDay(person, date);
	}

	/**
	 * 
	 * @param name
	 * @param surname
	 * @return una lista di stringhe ottenute concatenando nome e cognome in vari modi per proporre lo username per il 
	 * nuovo dipendente inserito 
	 */
	public List<String> composeUsername(String name, String surname){
		List<String> usernameList = new ArrayList<String>();
		usernameList.add(name.replace(' ', '_').toLowerCase()+'.'+surname.replace(' ','_').toLowerCase());
		usernameList.add(name.trim().toLowerCase().substring(0,1)+'.'+surname.replace(' ','_').toLowerCase());


		int blankNamePosition = whichBlankPosition(name);
		int blankSurnamePosition = whichBlankPosition(surname);
		if(blankSurnamePosition > 4 && blankNamePosition == 0){
			usernameList.add(name.toLowerCase().replace(' ','_')+'.'+surname.substring(0, blankSurnamePosition).toLowerCase());
			usernameList.add(name.toLowerCase().replace(' ','_')+'.'+surname.substring(blankSurnamePosition+1, surname.length()).toLowerCase());
		}
		if(blankNamePosition > 3 && blankSurnamePosition == 0){
			usernameList.add(name.substring(0, blankNamePosition).toLowerCase()+'.'+surname.toLowerCase().replace(' ','_'));
			usernameList.add(name.substring(blankNamePosition+1, name.length()).toLowerCase()+'.'+surname.toLowerCase());
			usernameList.add(name.toLowerCase().replace(' ','_')+'.'+surname.toLowerCase().replace(' ','_'));
		}
		if(blankSurnamePosition < 4 && blankNamePosition == 0){
			usernameList.add(name.toLowerCase()+'.'+surname.trim().toLowerCase());
		}
		if(blankSurnamePosition > 4 && blankNamePosition > 3){
			usernameList.add(name.toLowerCase().replace(' ','_')+'.'+surname.toLowerCase().replace(' ','_'));
			usernameList.add(name.toLowerCase().substring(0, blankNamePosition)+'.'+surname.replace(' ','_').toLowerCase());
			usernameList.add(name.substring(blankNamePosition+1, name.length()).toLowerCase()+'.'+surname.replace(' ','_').toLowerCase());
			usernameList.add(name.replace(' ','_').toLowerCase()+'.'+surname.substring(0, blankSurnamePosition).toLowerCase());
			usernameList.add(name.replace(' ','_').toLowerCase()+'.'+surname.substring(blankSurnamePosition+1, surname.length()).toLowerCase());
			usernameList.add(name.substring(0, blankNamePosition).toLowerCase()+'.'+surname.substring(0, blankSurnamePosition).toLowerCase());
			usernameList.add(name.substring(0, blankNamePosition).toLowerCase()+'.'+surname.substring(blankSurnamePosition+1, surname.length()).toLowerCase());
			usernameList.add(name.substring(blankNamePosition+1, name.length()).toLowerCase()+'.'+surname.substring(0, blankSurnamePosition).toLowerCase());
			usernameList.add(name.substring(blankNamePosition+1, name.length()).toLowerCase()+'.'+surname.substring(blankSurnamePosition+1, surname.length()).toLowerCase());
		}
		return usernameList;
	}

	/**
	 * 
	 * @param s
	 * @return la posizione in una stringa in cui si trova un eventuale spazio (più cognomi, più nomi...)
	 */
	private int whichBlankPosition(String s){
		int position = 0;
		for(int i = 0; i < s.length(); i++){
			if(s.charAt(i) == ' ')
				position = i;
		}
		return position;
	}

	/**
	 * //TODO utilizzare jpa per prendere direttamente i codici (e migrare ad una lista)
	 * @param days lista di PersonDay
	 * @return la lista contenente le assenze fatte nell'arco di tempo dalla persona
	 */
	public Map<AbsenceType,Integer> getAllAbsenceCodeInMonth(List<PersonDay> personDays){
		int month = personDays.get(0).date.getMonthOfYear();
		int year = personDays.get(0).date.getYear();
		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		Person person = personDays.get(0).person;

		//	List<AbsenceType> abtList = AbsenceTypeDao.getAbsenceTypeInPeriod(beginMonth, endMonth, person);
		List<AbsenceType> abtList = AbsenceType.find("Select abt from AbsenceType abt, Absence ab, PersonDay pd where ab.personDay = pd and ab.absenceType = abt and pd.person = ? and pd.date between ? and ?", person, beginMonth, endMonth ).fetch();
		Map<AbsenceType, Integer> absenceCodeMap = new HashMap<AbsenceType, Integer>();
		int i = 0;
		for(AbsenceType abt : abtList)
		{
			boolean stato = absenceCodeMap.containsKey(abt);
			if(stato==false){
				i=1;
				absenceCodeMap.put(abt,i);            	 
			} else{
				i = absenceCodeMap.get(abt);
				absenceCodeMap.remove(abt);
				absenceCodeMap.put(abt, i+1);
			}
		}
		return absenceCodeMap;
	}



	/**
	 * 
	 * @return il numero di giorni lavorati in sede. 
	 */
	public int basedWorkingDays(List<PersonDay> personDays){
 		
		int basedDays = 0;
		for (PersonDay pd : personDays) {	
 
			IWrapperPersonDay day = wrapperFactory.create(pd);
			boolean fixed = day.isFixedTimeAtWork();

			if(pd.isHoliday) {
				continue;
			}

			if (fixed && !personDayManager.isAllDayAbsences(pd) ){
				basedDays++;
			}
			else if( !fixed && pd.stampings.size() > 0 
					&& !personDayManager.isAllDayAbsences(pd) )	{
				basedDays++;
			}
		}
		return basedDays;
	}

	/**
	 * Il numero di riposi compensativi utilizzati nell'anno dalla persona
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public int numberOfCompensatoryRestUntilToday(Person person, int year, int month) {
		
		// TODO: andare a fare bound con sourceDate e considerare quelli da
		// inizializzazione
		
		LocalDate begin = new LocalDate(year,1,1);
		LocalDate end = new LocalDate(year, month, 1).dayOfMonth().withMaximumValue();
		return absenceDao.absenceInPeriod(person, begin, end, "91").size();
	}
	
	/**
	 * 
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public int holidayWorkingTimeNotAccepted(Person person, Optional<Integer> year, 
			Optional<Integer> month) {
		
		List<PersonDay> pdList = personDayDao
				.getHolidayWorkingTime(person, year, month);
		int value = 0;
		for(PersonDay pd : pdList) {
			if( !pd.acceptedHolidayWorkingTime ) {
				value+= pd.timeAtWork; 
			}
		}
		return value;
	}
	
	/**
	 * 
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public int holidayWorkingTimeAccepted(Person person, Optional<Integer> year, 
			Optional<Integer> month) {
		
		List<PersonDay> pdList = personDayDao
				.getHolidayWorkingTime(person, year, month);
		int value = 0;
		for(PersonDay pd : pdList) {
			if( pd.acceptedHolidayWorkingTime ) {
				value+= pd.timeAtWork; 
			}
		}
		return value;
	}
	
	/**
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public int holidayWorkingTimeTotal(Person person, Optional<Integer> year, 
			Optional<Integer> month) {
		List<PersonDay> pdList = personDayDao
				.getHolidayWorkingTime(person, year, month);
		int value = 0;
		for(PersonDay pd : pdList) {
			value+= pd.timeAtWork; 
		}
		return value;
	}
	
	
}
