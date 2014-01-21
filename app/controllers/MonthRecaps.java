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
import models.StampProfile;
import models.Stamping;
import models.WorkingTimeTypeDay;
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
	
	/**
	 * Riepilogo mensile per la Persona. Contiene le seguenti informazioni
	 * Assenze non giustificate
	 * Assenze giustificate
	 * Giorni di presenza al lavoro nei giorni festivi
	 * Giorni di presenza al lavoro nei giorni lavorativi
	 * Buoni mensa da restituire
	 * Residuo a fine mese (differenza)
	 * Ore di lavoro fatte
	 * Ore straord. pagate
	 * @author alessandro
	 *
	 */
	protected static class PersonMonthRecapFieldSet
	{
		protected List<PersonDay> notJustifiedAbsences = new ArrayList<PersonDay>();
		protected List<PersonDay> justifiedAbsences = new ArrayList<PersonDay>();
		protected List<PersonDay> workingDayHoliday = new ArrayList<PersonDay>();
		protected List<PersonDay> workingDayNotHoliday = new ArrayList<PersonDay>();
		protected int totalTimeAtWork = 0;
		protected int difference = 0;
		protected int mealTicketToRender = 0;
		protected int mealTicketToUse = 0;
		protected int valueApproved = 0;
		
		/**
		 * Algoritmo per il calcolo del riepilogo mensile per la persona.
		 * @param person
		 * @param pdList
		 * @param year
		 * @param month
		 */
		private void populatePersonMonthRecap(Person person, List<PersonDay> pdList, int year, int month)
		{
			for(PersonDay pd : pdList)
			{
				//pd.getStampProfile();
				totalTimeAtWork = totalTimeAtWork + pd.timeAtWork;
				difference = difference + pd.difference;
				
				//meal ticket to render --------------------------------------------------------------------------------------------------
				if(pd.isTicketAvailable)
					mealTicketToUse++;
				
				if(!pd.isTicketAvailable && !pd.isHoliday())
					mealTicketToRender++;
				
				//holiday at work ---------------------------------------------------------------------------------------------------------
				if(pd.isHoliday())
				{
					if (pd.timeAtWork>0)		
					{
						workingDayHoliday.add(pd);
					}						
				}
				//not holiday at work -----------------------------------------------------------------------------------------------------
				//persone fixed
				else if(pd.isFixedTimeAtWork())
				{
					if(!pd.isAllDayAbsences())
					{	
						workingDayNotHoliday.add(pd);
					}
					else
					{
						justifiedAbsences.add(pd);
					}
				}
				//persone non fixed
				else if(!pd.isFixedTimeAtWork())
				{
					if(pd.isInTrouble()) 
					{
						notJustifiedAbsences.add(pd);
					}
					else if(!pd.isAllDayAbsences())
					{
						workingDayNotHoliday.add(pd);
					}
					else
					{
						justifiedAbsences.add(pd);
					}
				}
			}

			//straordinari s1/s2/s3
			List<Competence> competenceList = 
					Competence.find("Select comp from Competence comp, CompetenceCode compCode where comp.competenceCode = compCode and comp.person = ?"
					+ "and comp.year = ? and comp.month = ? and (compCode.code = ? or compCode.code = ? or compCode.code = ?)",
					person, year, month, "S1", "S2", "S3").fetch();
			valueApproved = 0;
			for(Competence comp : competenceList)
			{
				valueApproved = valueApproved + comp.valueApproved;
			}
			

		}
	}

	/**
	 * Controller che gestisce il calcolo del Riepilogo Mensile.
	 * @param year
	 * @param month
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
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

		List<Person> activePersons = Person.getActivePersonsInMonth(month, year, false);

		//logica mese attuale
		if(today.getYear()==year && today.getMonthOfYear()==month)
		{
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
				return;
			}
			
			//Considero il riepilogo fino a ieri
			monthEnd = today.minusDays(1);
			
		}
			
		

		for(Person person : activePersons)
		{
			//person day list
			List<PersonDay> pdList = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? AND pd.date between ? and ?", 
					person, 
					monthBegin, 
					monthEnd).fetch();
			
			
			Logger.info("Costruisco riepilogo mensile per %s %s %s",person.id, person.name, person.surname);
			PersonMonthRecapFieldSet mr = new PersonMonthRecapFieldSet();
			mr.populatePersonMonthRecap(person, pdList, year, month);

			tableMonthRecap.put(person, "Giorni di presenza al lavoro nei giorni festivi".intern(), mr.workingDayHoliday.size());
			tableMonthRecap.put(person, "Giorni di presenza al lavoro nei giorni lavorativi".intern(), mr.workingDayNotHoliday.size());
			tableMonthRecap.put(person, "Ore di lavoro fatte".intern(), new Integer(mr.totalTimeAtWork));
			tableMonthRecap.put(person, "Differenza ore (Residuo a fine mese)".intern(), new Integer(mr.difference));
			tableMonthRecap.put(person, "Assenze giustificate".intern(), new Integer(mr.justifiedAbsences.size()));
			tableMonthRecap.put(person, "Assenze non giustificate".intern(), new Integer(mr.notJustifiedAbsences.size()));
			tableMonthRecap.put(person, "Ore straord. pagate".intern(), new Integer(mr.valueApproved));
			tableMonthRecap.put(person, "Buoni mensa da restituire".intern(), mr.mealTicketToRender);
		}
		
		String month_capitalized  = null;
		if(month>0)
			month_capitalized = DateUtility.fromIntToStringMonth(month);
		render(tableMonthRecap, generalWorkingDaysOfMonth, today, lastDayOfMonth, month_capitalized, year);

	}
	
	/**
	 * Controller che ritorna la lista dei Giorni di assenza non giustificati.
	 * @param id
	 * @param year
	 * @param month
	 */
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void notJustifiedAbsences(Long id, int year, int month){
		
		List<PersonDay> notJustifiedAbsences = getPersonDayListRecap(id, year, month, "notJustifiedAbsences");

		render(notJustifiedAbsences);
	}
	
	/**
	 * Controller che ritorna la lista dei Giorni di assenza giustificati.
	 * @param id
	 * @param year
	 * @param month
	 */
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void justifiedAbsences(Long id, int year, int month){
		List<PersonDay> justifiedAbsences = getPersonDayListRecap(id, year, month, "justifiedAbsences");

		render(justifiedAbsences);
	}

	/**
	 * Controller che ritorna la lista dei Giorni di presenza al lavoro nei giorni festivi.
	 * @param id
	 * @param year
	 * @param month
	 */
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void workingDayHoliday(Long id, int year, int month){
		List<PersonDay> workingDayHoliday = getPersonDayListRecap(id, year, month, "workingDayHoliday");

		render(workingDayHoliday);
	}
	
	/**
	 * Controller che ritorna la lista dei Giorni di presenza al lavoro nei giorni lavorativi.
	 * @param id
	 * @param year
	 * @param month
	 */
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void workingDayNotHoliday(Long id, int year, int month){
		List<PersonDay> workingDayNotHoliday = getPersonDayListRecap(id, year, month, "workingDayNotHoliday");

		render(workingDayNotHoliday);
	}

	/**
	 * Calcola il riepilogo mensile per la persona con identificativo id e ritorna la lista specificamente richiesta 
	 * @param id
	 * @param year
	 * @param month
	 * @param listType enum: notJustifiedAbsences, justifiedAbsences, workingDayHoliday, workingDayNotHoliday
	 * @return
	 */
	private static List<PersonDay> getPersonDayListRecap(Long id, int year, int month, String listType)
	{
		
		Person person = Person.findById(id);
				
		LocalDate today = new LocalDate();
		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();

		//logica mese attuale
		if(today.getYear()==year && today.getMonthOfYear()==month)
		{
			//Se oggi e' il primo giorno del mese stampo la tabella vuota 
			if(today.getDayOfMonth()==1)
			{
				List<PersonDay> notJustifiedAbsences = new ArrayList<PersonDay>();
				render(notJustifiedAbsences);
			}
			
			//Considero il riepilogo fino a ieri
			monthEnd = today.minusDays(1);

		}
		
		List<PersonDay> pdList = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? AND pd.date between ? and ?", 
			person, 
			monthBegin, 
			monthEnd).fetch();
		
		PersonMonthRecapFieldSet mr = new PersonMonthRecapFieldSet();
		mr.populatePersonMonthRecap(person, pdList, year, month);
	
		if(listType.equals("notJustifiedAbsences"))
			return mr.notJustifiedAbsences;
		if(listType.equals("justifiedAbsences"))
			return mr.justifiedAbsences;
		if(listType.equals("workingDayHoliday"))
			return mr.workingDayHoliday;
		if(listType.equals("workingDayNotHoliday"))
			return mr.workingDayNotHoliday;
		
		return null;
	}

	
	

}
