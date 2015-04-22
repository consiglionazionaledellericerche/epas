package manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import models.Competence;
import models.Person;
import models.PersonDay;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import dao.CompetenceDao;
import dao.PersonDao;
import dao.PersonDayDao;

public class MonthRecapManager {
	
	private final PersonDayDao personDayDao;
	private final CompetenceDao competenceDao;
	
	private final static Logger log = LoggerFactory.getLogger(MonthRecapManager.class);
	
	@Inject
	public MonthRecapManager(PersonDayDao personDayDao,
			CompetenceDao competenceDao) {

		this.personDayDao = personDayDao;
		this.competenceDao = competenceDao;
	}
	
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
	protected class PersonMonthRecapFieldSet
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
					if(!PersonDayManager.isAllDayAbsences(pd))
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
					if(PersonDayManager.isInTrouble(pd)) 
					{
						notJustifiedAbsences.add(pd);
					}
					else if(!PersonDayManager.isAllDayAbsences(pd))
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
			List<String> code = CompetenceManager.populateListWithOvertimeCodes();			
			List<Competence> competenceList = competenceDao.getCompetences(Optional.fromNullable(person),year, month, code, person.office, false);
			valueApproved = 0;
			for(Competence comp : competenceList)
			{
				valueApproved = valueApproved + comp.valueApproved;
			}			

		}
	}
	
	public static Comparator<Person> PersonNameComparator = new Comparator<Person>() {

		public int compare(Person person1, Person person2) {

			String name1 = person1.surname.toUpperCase() + " " + person1.name.toUpperCase();
			String name2 = person2.surname.toUpperCase() + " " + person2.name.toUpperCase();

			return name1.compareTo(name2);

		}

	};	

	public static Comparator<String> AbsenceCodeComparator = new Comparator<String>(){

		public int compare(String absenceCode1, String absenceCode2){
			return absenceCode1.compareTo(absenceCode2);

		}		

	};

	
	/**
	 * Calcola il riepilogo mensile per la persona con identificativo id e ritorna la lista specificamente richiesta 
	 * @param id
	 * @param year
	 * @param month
	 * @param listType enum: notJustifiedAbsences, justifiedAbsences, workingDayHoliday, workingDayNotHoliday
	 * @return
	 */
	public List<PersonDay> getPersonDayListRecap(Long id, int year, int month, String listType)
	{
		
		Person person = PersonDao.getPersonById(id);
		//Person person = Person.findById(id);
				
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
				return(notJustifiedAbsences);
			}
			
			//Considero il riepilogo fino a ieri
			monthEnd = today.minusDays(1);

		}
		
		List<PersonDay> pdList = personDayDao.getPersonDayInPeriod(person, monthBegin, Optional.fromNullable(monthEnd), false);
		
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
	/**
	 * 
	 * @param personList
	 * @return la tabella contenente il default di valori per i vari parametri presenti nel metodo per ciascuna persona
	 */
	public static Table<Person,String,Integer> populateDefaultTable(List<Person> personList){
		Table<Person, String, Integer> tableMonthRecap = TreeBasedTable.create(PersonNameComparator, AbsenceCodeComparator);
		for(Person person : personList)
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
		return tableMonthRecap;
	}
	
	/**
	 * 
	 * @param personList
	 * @param monthBegin
	 * @param monthEnd
	 * @param year
	 * @param month
	 * @return
	 */
	public Table<Person,String,Integer> populateRealValueTable(List<Person> personList, LocalDate monthBegin, LocalDate monthEnd, int year, int month){
		Table<Person, String, Integer> tableMonthRecap = TreeBasedTable.create(PersonNameComparator, AbsenceCodeComparator);
		for(Person person : personList)
		{
			//person day list
			List<PersonDay> pdList = personDayDao.getPersonDayInPeriod(person, monthBegin, Optional.fromNullable(monthEnd), false);
						
			log.debug("populateRealValueTable -> costruisco riepilogo mensile per {} {} {}",
					new Object[] { person.id, person.name, person.surname });
			
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
		return tableMonthRecap;
	}
}
