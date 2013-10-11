package controllers;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.StampProfile;
import models.Stamping;
import models.WorkingTimeTypeDay;
import models.efficiency.EfficientPersonDay;
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import play.Logger;
import play.Play;
import play.data.binding.As;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class MonthRecaps extends Controller{

	private static Comparator<Person> PersonNameComparator = new Comparator<Person>() {

		public int compare(Person person1, Person person2) {

			String name1 = person1.surname.toUpperCase() + " " + person1.name.toUpperCase();
			String name2 = person2.surname.toUpperCase() + " " + person2.name.toUpperCase();

			return name1.compareTo(name2);

		}

	};	

	private static Comparator<String> AbsenceCodeComparator = new Comparator<String>(){

		public int compare(String absenceCode1, String absenceCode2){
			return absenceCode1.compareTo(absenceCode2);

		}		

	};

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void show(int year, int month) throws ClassNotFoundException, SQLException {

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
			if( ! DateUtility.isGeneralHoliday(lastDayOfMonth) )
			{
				generalWorkingDaysOfMonth++;
			}
			lastDayOfMonth = lastDayOfMonth.plusDays(1);
		}
		lastDayOfMonth = lastDayOfMonth.minusDays(1);
		
		Table<Person, String, Integer> tableMonthRecap = TreeBasedTable.create(PersonNameComparator, AbsenceCodeComparator);

		List<Person> activePersons = Person.getActivePersonsInMonth(month, year);
		
		//diagnosi
		//boolean dbConsistentControl = PersonDay.diagnosticPersonDay(year, month, activePersons);
		
		//Se mese attuale considero i person day fino a ieri
		if(today.getMonthOfYear()==month)
		{
			monthEnd = today.minusDays(2);
		}
		//Se oggi e' il primo giorno del mese stampo la tabella vuota 
		if(today.getDayOfMonth()==1)
		{
			for(Person person : activePersons)
			{
				tableMonthRecap.put(person, "Giorni di presenza al lavoro nei giorni festivi".intern(), 0);
				tableMonthRecap.put(person, "Giorni di presenza al lavoro nei giorni lavorativi".intern(), 0);
				tableMonthRecap.put(person, "Ore di lavoro fatte".intern(), 0);
				tableMonthRecap.put(person, "Differenza ore (Residuo a fine mese)".intern(), 0);
				tableMonthRecap.put(person, "Assenze giustificate".intern(), 0);
				tableMonthRecap.put(person, "Assenze non giustificate".intern(), 0);
				tableMonthRecap.put(person, "Ore straord. pagate".intern(), 0);
				tableMonthRecap.put(person, "Buoni mensa da restituire".intern(),0);
			}
			render(tableMonthRecap, generalWorkingDaysOfMonth, today, lastDayOfMonth);
		}
	
		for(Person person : activePersons){

			List<EfficientPersonDay> personDayRsList = EfficientPersonDay.getEfficientPersonDays(person, monthBegin, monthEnd);
			//personDayRsList di norma non e' vuoto, capita per esempio per Cresci/DiPietro/Conti etc che non dovrebbero esistere
			if(personDayRsList.size()!=0)
			{
				checkPersonMonthRecap(person, personDayRsList, tableMonthRecap);
			}

		}
		render(tableMonthRecap, generalWorkingDaysOfMonth, today, lastDayOfMonth);

	}
	
	/**
	 * Metodo privato che calcola il riepilogo mensile per la persona e lo inserisce nella ImmutableTable
	 * @param person
	 * @param personDayRsList
	 * @param tableMonthRecap
	 */
	private static void checkPersonMonthRecap(
			Person person, 
			List<EfficientPersonDay> personDayRsList, 
			Table<Person, String, Integer> tableMonthRecap)
	{
		
		Integer workingDayHoliday = 0;
		Integer workingDayNotHoliday = 0;
		int totalTimeAtWork = 0;
		int difference = 0;
		int justifiedAbsence = 0;
		int notJustifiedAbsence = 0;
		int mealTicketToRender = 0;
		int valueApproved = personDayRsList.get(0).valueApproved;
		
		for(EfficientPersonDay pdRecap : personDayRsList)
		{
			//total time at work -----------------------------------------------------------------------------------------------------
			totalTimeAtWork = totalTimeAtWork + pdRecap.timeAtWork;
			difference = difference + pdRecap.difference;
			
			//meal ticket to render --------------------------------------------------------------------------------------------------
			if(!pdRecap.isHoliday && pdRecap.timeAtWork - pdRecap.mealTicketTime < 0)
			{
				mealTicketToRender++;
			}
		
			//holiday at work ---------------------------------------------------------------------------------------------------------
			if(pdRecap.isHoliday)
			{
				if (!pdRecap.fixed && !pdRecap.justified.equals("AllDay") && pdRecap.isProperSequence)		
				{
					workingDayHoliday++;
				}
				continue;						
			}
			//not holiday at work -----------------------------------------------------------------------------------------------------
			//persone fixed
			if(pdRecap.fixed)
			{
				if(!pdRecap.justified.equals("AllDay"))
				{	
					workingDayNotHoliday++;
					continue;
				}
				if(pdRecap.justified.equals("AllDay"))
				{
					justifiedAbsence++;
					continue;
				}
				
			}
			//persone non fixed
			if(!pdRecap.fixed)
			{
				if(pdRecap.isProperSequence && !pdRecap.justified.equals("AllDay"))
				{
					workingDayNotHoliday++;
					continue;
				}
				if(pdRecap.justified.equals("AllDay"))
				{
					justifiedAbsence++;
					continue;
				}
				if(!pdRecap.isProperSequence && !pdRecap.justified.equals("AllDay"))
				{
					notJustifiedAbsence++;
					continue;
				}
					
			}
		}
		
		tableMonthRecap.put(person, "Giorni di presenza al lavoro nei giorni festivi".intern(), workingDayHoliday);
		tableMonthRecap.put(person, "Giorni di presenza al lavoro nei giorni lavorativi".intern(), workingDayNotHoliday);
		tableMonthRecap.put(person, "Ore di lavoro fatte".intern(), new Integer(totalTimeAtWork));
		tableMonthRecap.put(person, "Differenza ore (Residuo a fine mese)".intern(), new Integer(difference));
		tableMonthRecap.put(person, "Assenze giustificate".intern(), new Integer(justifiedAbsence));
		tableMonthRecap.put(person, "Assenze non giustificate".intern(), new Integer(notJustifiedAbsence));
		tableMonthRecap.put(person, "Ore straord. pagate".intern(), new Integer(valueApproved));
		tableMonthRecap.put(person, "Buoni mensa da restituire".intern(), mealTicketToRender);
		
	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void notJustifiedAbsences(Long id, int year, int month){
		List<PersonDay> notJustifiedAbsences = getPersonDayListRecap(id, year, month, "notJustifiedAbsences");

		render(notJustifiedAbsences);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void justifiedAbsences(Long id, int year, int month){
		List<PersonDay> justifiedAbsences = getPersonDayListRecap(id, year, month, "justifiedAbsences");

		render(justifiedAbsences);
	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void workingDayHoliday(Long id, int year, int month){
		List<PersonDay> workingDayHoliday = getPersonDayListRecap(id, year, month, "workingDayHoliday");

		render(workingDayHoliday);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void workingDayNotHoliday(Long id, int year, int month){
		List<PersonDay> workingDayNotHoliday = getPersonDayListRecap(id, year, month, "workingDayNotHoliday");

		render(workingDayNotHoliday);
	}

	private static List<PersonDay> getPersonDayListRecap(Long id, int year, int month, String listType)
	{
		List<PersonDay> notJustifiedAbsences = new ArrayList<PersonDay>();
		List<PersonDay> justifiedAbsences = new ArrayList<PersonDay>();
		List<PersonDay> workingDayHoliday = new ArrayList<PersonDay>();
		List<PersonDay> workingDayNotHoliday = new ArrayList<PersonDay>();
		Person person = Person.findById(id);
		List<StampProfile> personStampProfiles = person.stampProfiles;
		
		LocalDate today = new LocalDate();
		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();
		//Se mese attuale considero i person day fino a ieri
		if(today.getMonthOfYear()==month)
		{
			monthEnd = today.minusDays(2);
		}
		//Se oggi e' il primo giorno del mese stampo la tabella vuota 
		if(today.getDayOfMonth()==1)
		{
			render(notJustifiedAbsences);
		}

		List<PersonDay> pdList = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? AND pd.date between ? and ?", 
			person, 
			monthBegin, 
			monthEnd).fetch();
		
		for(PersonDay pd : pdList)
		{
			//holiday at work ---------------------------------------------------------------------------------------------------------
			if(pd.isHoliday())
			{
				if (!pd.isFixedTimeAtWork(personStampProfiles) && !pd.containsAllDayAbsence() && pd.containsProperStampingSequence())		
				{
					workingDayHoliday.add(pd);
					//workingDayHoliday++;
				}
				continue;						
			}
			//not holiday at work -----------------------------------------------------------------------------------------------------
			//persone fixed
			if(pd.isFixedTimeAtWork(personStampProfiles))
			{
				if(!pd.containsAllDayAbsence())
				{	
					workingDayNotHoliday.add(pd);
					//workingDayNotHoliday++;
					continue;
				}
				if(pd.containsAllDayAbsence())
				{
					justifiedAbsences.add(pd);
					//justifiedAbsence++;
					continue;
				}
				
			}
			//persone non fixed
			if(!pd.isFixedTimeAtWork(personStampProfiles))
			{
				if(pd.containsProperStampingSequence() && !pd.containsAllDayAbsence())
				{
					workingDayNotHoliday.add(pd);
					//workingDayNotHoliday++;
					continue;
				}
				if(pd.containsAllDayAbsence())
				{
					justifiedAbsences.add(pd);
					//justifiedAbsence++;
					continue;
				}
				if(!pd.containsProperStampingSequence() && !pd.containsAllDayAbsence())
				{
					notJustifiedAbsences.add(pd);
					//notJustifiedAbsence++;
					continue;
				}
					
			}
			
		}
		
		if(listType.equals("notJustifiedAbsences"))
			return notJustifiedAbsences;
		if(listType.equals("justifiedAbsences"))
			return justifiedAbsences;
		if(listType.equals("workingDayHoliday"))
			return workingDayHoliday;
		if(listType.equals("workingDayNotHoliday"))
			return workingDayNotHoliday;
		
		return null;
	}

	
	

}
