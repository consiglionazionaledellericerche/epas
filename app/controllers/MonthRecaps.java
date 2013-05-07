package controllers;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

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
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import play.Logger;
import play.data.binding.As;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class MonthRecaps extends Controller{

	private static Comparator<Person> PersonNameComparator = new Comparator<Person>() {

		public int compare(Person person1, Person person2) {

			String name1 = person1.surname.toUpperCase();
			String name2 = person2.surname.toUpperCase();

			return name1.compareTo(name2);

		}

	};	

	private static Comparator<String> AbsenceCodeComparator = new Comparator<String>(){

		public int compare(String absenceCode1, String absenceCode2){
			return absenceCode1.compareTo(absenceCode2);

		}		

	};

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void show(int year, int month) {
		LocalDate localDate = new LocalDate(year, month, 1);
		LocalDate today = new LocalDate();
		List<Person> persons = Person.getActivePersons(localDate);
		Table<Person, String, Integer> tableMonthRecap = TreeBasedTable.create(PersonNameComparator, AbsenceCodeComparator);
		/**
		 * bisogna considerare il caso in cui venga chiesto di vedere il riepilogo mensile per "tutti i mesi"...
		 */
		Logger.debug("Voglio il riepilogo di %d %d", month, year);		
		int numberOfWorkingDays = 0;
		if(today.getMonthOfYear() == localDate.getMonthOfYear()){
			while(localDate.isBefore(today)){
				//Logger.debug("Il giorno è: %s", localDate);
				if(DateUtility.isGeneralHoliday(localDate))
					numberOfWorkingDays = numberOfWorkingDays +1;
				localDate = localDate.plusDays(1);
			}
			Logger.debug("In questo mese ci sono fino ad oggi %d giorni lavorativi", numberOfWorkingDays);
		}
		else{
			while(localDate.isBefore(localDate.dayOfMonth().withMaximumValue())){
				//Logger.debug("Il giorno è: %s", localDate);
				if(DateUtility.isGeneralHoliday(localDate))
					numberOfWorkingDays = numberOfWorkingDays +1;
				localDate = localDate.plusDays(1);
			}
			Logger.debug("Nel mese di %d ci sono %d giorni lavorativi", localDate.getMonthOfYear(), numberOfWorkingDays);
		}

		for(Person p : persons){
			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
					p, localDate, localDate.dayOfMonth().withMaximumValue()).fetch();
			PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
					p, month, year).first();
			tableMonthRecap.put(p, "Giorni di presenza al lavoro nei giorni festivi", new Integer(PersonUtility.workDayInHoliday(p, localDate, localDate.dayOfMonth().withMaximumValue())));
			tableMonthRecap.put(p, "Giorni di presenza al lavoro nei giorni lavorativi", new Integer(PersonUtility.workDayInWorkingDay(p, localDate, localDate.dayOfMonth().withMaximumValue())));

			int timeAtWork = 0;
			int difference = 0;
			int justifiedAbsence = 0;
			int notJustifiedAbsence = 0;
			for(PersonDay pd : pdList){
				timeAtWork = timeAtWork + pd.timeAtWork;
				difference = difference + pd.difference;
				if(pd.absences.size() == 1){

					if(pd.absences.get(0).absenceType.justifiedTimeAtWork.minutesJustified == null)
						justifiedAbsence = justifiedAbsence +1;

				}
				if(pd.absences.size() == 0 && (pd.stampings.size() == 0 || pd.stampings.size() == 1))
					notJustifiedAbsence = notJustifiedAbsence + 1;

			}
			CompetenceCode code = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "S1").first();
			Competence comp = Competence.find("Select comp from Competence comp where comp.person = ? and comp.month = ? and comp.year = ? " +
					"and comp.competenceCode = ?", p, localDate.getMonthOfYear(), localDate.getYear(), code).first();

			int mealTicketToRender = 0;
			mealTicketToRender = mealTicketToRender + pm.numberOfMealTicketToRender();
			tableMonthRecap.put(p, "Ore di lavoro fatte", new Integer(timeAtWork));
			tableMonthRecap.put(p, "Differenza ore (Residuo a fine mese)", new Integer(difference));
			tableMonthRecap.put(p, "Assenze giustificate", new Integer(justifiedAbsence));
			tableMonthRecap.put(p, "Assenze non giustificate", new Integer(notJustifiedAbsence));
			tableMonthRecap.put(p, "Ore straord. pagate", new Integer(comp.valueApproved));
			tableMonthRecap.put(p, "Buoni mensa da restituire", new Integer(mealTicketToRender));

		}
		render(tableMonthRecap, numberOfWorkingDays, today);

	}

}
