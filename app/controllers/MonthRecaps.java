package controllers;

import helpers.ModelQuery.SimpleResults;
import it.cnr.iit.epas.DateUtility;

import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;

import manager.MonthRecapManager;
import models.ContractMonthRecap;
import models.Person;
import models.PersonDay;

import org.joda.time.LocalDate;
import org.joda.time.MonthDay;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

@With( {Resecure.class, RequestInit.class} )
public class MonthRecaps extends Controller{

	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static WrapperModelFunctionFactory wrapperFunctionFactory;
	@Inject
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static MonthRecapManager monthRecapManager;
	@Inject
	private static SecurityRules rules;
	
	/**
	 * Controller che gescisce il calcolo del riepilogo annuale residuale delle persone.
	 * 
	 * @param year
	 */
	public static void showRecaps(int year, int month) {

		//FIXME per adesso senza paginazione

		//Prendo la lista delle persone attive in questo momento. 
		//Secondo me si deve mettere le persone non attive in un elenco da poter
		//Analizzare singolarmente.

		List<Person> simplePersonList = personDao.list(Optional.<String>absent(),
				officeDao.getOfficeAllowed(Security.getUser().get()),
				false, LocalDate.now(), LocalDate.now(), false).list();

		List<IWrapperPerson> personList = FluentIterable
				.from(simplePersonList)
				.transform(wrapperFunctionFactory.person()).toList();

		List<ContractMonthRecap> recaps = Lists.newArrayList();
		
		for(IWrapperPerson person : personList) {
			
			IWrapperContract contract = wrapperFactory.create(person.getCurrentContract().get());
			
			YearMonth yearMonth = new YearMonth(year, month); 
			
			Optional<ContractMonthRecap> recap = contract.getContractMonthRecap( yearMonth );
			if (recap.isPresent()) {
				recaps.add(recap.get());
			} else { 
				System.out.println(person.getValue().fullName());
			}
		}

		render(recaps);
	}

	/**
	 * Controller che gestisce il calcolo del Riepilogo Mensile.
	 * @param year
	 * @param month
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void show(int year, int month, String name, Integer page) {

		if(page == null)
			page = 0;

		LocalDate today = new LocalDate();
		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();
		LocalDate lastDayOfMonth = monthBegin;

		//numero di giorni lavorativi in month
		int generalWorkingDaysOfMonth = 0;
		while(!lastDayOfMonth.isAfter(monthEnd))
		{
			if(!lastDayOfMonth.isBefore(today))
				break;
			if(lastDayOfMonth.getDayOfWeek()==6 || lastDayOfMonth.getDayOfWeek()==7)
			{
				lastDayOfMonth = lastDayOfMonth.plusDays(1);
				continue;
			}
			if(!DateUtility.isGeneralHoliday(Optional.<MonthDay>absent(), lastDayOfMonth) )
			{
				generalWorkingDaysOfMonth++;
			}
			lastDayOfMonth = lastDayOfMonth.plusDays(1);
		}
		lastDayOfMonth = lastDayOfMonth.minusDays(1);

		Table<Person, String, Integer> tableMonthRecap = TreeBasedTable.create(MonthRecapManager.PersonNameComparator, MonthRecapManager.AbsenceCodeComparator);

		SimpleResults<Person> simpleResults = personDao.list(Optional.fromNullable(name), 
				officeDao.getOfficeAllowed(Security.getUser().get()), false, monthBegin, monthEnd, true);

		List<Person> activePersons = simpleResults.paginated(page).getResults();

		//logica mese attuale
		if(today.getYear()==year && today.getMonthOfYear()==month)
		{
			//Se oggi e' il primo giorno del mese stampo la tabella vuota 
			if(today.getDayOfMonth()==1)
			{
				tableMonthRecap = MonthRecapManager.populateDefaultTable(activePersons);
				render(tableMonthRecap, generalWorkingDaysOfMonth, today, lastDayOfMonth, year, month, simpleResults, name);
				return;
			}
			//Considero il riepilogo fino a ieri
			monthEnd = today.minusDays(1);			
		}	

		tableMonthRecap = monthRecapManager.populateRealValueTable(activePersons, monthBegin, monthEnd, year, generalWorkingDaysOfMonth);

		render(tableMonthRecap, generalWorkingDaysOfMonth, today, lastDayOfMonth, year, month, simpleResults, name);

	}

	/**
	 * Controller che ritorna la lista dei Giorni di assenza non giustificati.
	 * @param id
	 * @param year
	 * @param month
	 */	
	public static void notJustifiedAbsences(Long personId, int year, int month){

		Person person = personDao.getPersonById(personId);
		if(person == null){
			flash.error("Persona non presente in anagrafica");
			MonthRecaps.show(year, month, null, null);
		}
		rules.checkIfPermitted(person.office);
		List<PersonDay> notJustifiedAbsences = monthRecapManager
				.getPersonDayListRecap(personId, year, month, "notJustifiedAbsences");

		render(notJustifiedAbsences, person);
	}

	/**
	 * Controller che ritorna la lista dei Giorni di assenza giustificati.
	 * @param id
	 * @param year
	 * @param month
	 */	
	public static void justifiedAbsences(Long personId, int year, int month){

		Person person = personDao.getPersonById(personId);
		if(person == null){
			flash.error("Persona non presente in anagrafica");
			MonthRecaps.show(year, month, null, null);
		}
		rules.checkIfPermitted(person.office);
		List<PersonDay> justifiedAbsences = monthRecapManager
				.getPersonDayListRecap(personId, year, month, "justifiedAbsences");

		render(justifiedAbsences, person);
	}

	/**
	 * Controller che ritorna la lista dei Giorni di presenza al lavoro nei giorni festivi.
	 * @param id
	 * @param year
	 * @param month
	 */	
	public static void workingDayHoliday(Long personId, int year, int month){

		Person person = personDao.getPersonById(personId);
		if(person == null){
			flash.error("Persona non presente in anagrafica");
			MonthRecaps.show(year, month, null, null);
		}
		rules.checkIfPermitted(person.office);
		List<PersonDay> workingDayHoliday = monthRecapManager
				.getPersonDayListRecap(personId, year, month, "workingDayHoliday");

		render(workingDayHoliday, person);
	}

	/**
	 * Controller che ritorna la lista dei Giorni di presenza al lavoro nei giorni lavorativi.
	 * @param id
	 * @param year
	 * @param month
	 */	
	public static void workingDayNotHoliday(Long personId, int year, int month){

		Person person = personDao.getPersonById(personId);
		if(person == null){
			flash.error("Persona non presente in anagrafica");
			MonthRecaps.show(year, month, null, null);
		}
		rules.checkIfPermitted(person.office);
		List<PersonDay> workingDayNotHoliday = monthRecapManager
				.getPersonDayListRecap(personId, year, month, "workingDayNotHoliday");

		render(workingDayNotHoliday, person);
	}

}
