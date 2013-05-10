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
		
		if(month != 0){
			Table<Person, String, Integer> tableMonthRecap = TreeBasedTable.create(PersonNameComparator, AbsenceCodeComparator);
			int numberOfWorkingDays = 0;
			int timeAtWork = 0;
			int difference = 0;
			int justifiedAbsence = 0;
			int notJustifiedAbsence = 0;
			int mealTicketToRender = 0;
			LocalDate localDate = new LocalDate(year, month, 1);
			LocalDate today = new LocalDate();
			List<Person> persons = Person.getActivePersons(localDate);
			Logger.debug("Voglio il riepilogo di %d %d", month, year);		

			if(today.getMonthOfYear() == localDate.getMonthOfYear()){
				while(localDate.isBefore(today)){
					//Logger.debug("Il giorno è: %s", localDate);
					if(!DateUtility.isGeneralHoliday(localDate))
						numberOfWorkingDays = numberOfWorkingDays +1;
					localDate = localDate.plusDays(1);
				}
				Logger.debug("In questo mese ci sono fino ad oggi %d giorni lavorativi", numberOfWorkingDays);
			}
			else{
				while(localDate.isBefore(localDate.dayOfMonth().withMaximumValue())){
					//Logger.debug("Il giorno è: %s", localDate);
					if(!DateUtility.isGeneralHoliday(localDate))
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
				
				tableMonthRecap.put(p, "Giorni di presenza al lavoro nei giorni festivi".intern(), new Integer(PersonUtility.workDayInHoliday(p, localDate.dayOfMonth().withMinimumValue(), localDate.dayOfMonth().withMaximumValue())));
				tableMonthRecap.put(p, "Giorni di presenza al lavoro nei giorni lavorativi".intern(), new Integer(PersonUtility.workDayInWorkingDay(p, localDate.dayOfMonth().withMinimumValue(), localDate.dayOfMonth().withMaximumValue())));


				for(PersonDay pd : pdList){
					timeAtWork = timeAtWork + pd.timeAtWork;
					difference = difference + pd.difference;
					if(pd.absences.size() == 1 && pd.absences.get(0).absenceType.absenceTypeGroup == null){

						justifiedAbsence = justifiedAbsence++;

					}
					if(pd.absences.size() == 0 && (pd.stampings.size() == 0 || pd.stampings.size() == 1))
						notJustifiedAbsence = notJustifiedAbsence++;

				}
				//Logger.debug("Il numero di assenze giustificate è: %d le assenze ingiustificate invece sono: %d", justifiedAbsence, notJustifiedAbsence);
				CompetenceCode code = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "S1").first();
				Competence comp = Competence.find("Select comp from Competence comp where comp.person = ? and comp.month = ? and comp.year = ? " +
						"and comp.competenceCode = ?", p, localDate.getMonthOfYear(), localDate.getYear(), code).first();


				mealTicketToRender = mealTicketToRender + pm.numberOfMealTicketToRender();
				//Logger.debug("Il numero di buoni mensa per %s %s è %d", p.name, p.surname, mealTicketToRender);
				tableMonthRecap.put(p, "Ore di lavoro fatte".intern(), new Integer(timeAtWork));
				tableMonthRecap.put(p, "Differenza ore (Residuo a fine mese)".intern(), new Integer(difference));
				tableMonthRecap.put(p, "Assenze giustificate".intern(), new Integer(justifiedAbsence));
				tableMonthRecap.put(p, "Assenze non giustificate".intern(), new Integer(notJustifiedAbsence));
				tableMonthRecap.put(p, "Ore straord. pagate".intern(), new Integer(comp.valueApproved));
				tableMonthRecap.put(p, "Buoni mensa da restituire".intern(), new Integer(mealTicketToRender));
				timeAtWork = 0;
				difference = 0;
				mealTicketToRender = 0;

			}
			render(tableMonthRecap, numberOfWorkingDays, today, localDate);
		}
		else{
			Table<Person, String, Integer> tableMonthRecap = TreeBasedTable.create(PersonNameComparator, AbsenceCodeComparator);
			int numberOfWorkingDays = 0;
			int timeAtWork = 0;
			int difference = 0;
			int justifiedAbsence = 0;
			int notJustifiedAbsence = 0;
			int mealTicketToRender = 0;
			LocalDate localDate = new LocalDate(year, 1, 1);
			LocalDate today = new LocalDate();
			List<Person> persons = Person.getActivePersons(localDate);
			while(localDate.isBefore(today)){
				//Logger.debug("Il giorno è: %s", localDate);
				if(DateUtility.isGeneralHoliday(localDate))
					numberOfWorkingDays = numberOfWorkingDays +1;
				localDate = localDate.plusDays(1);
			}
			for(Person p : persons){
				List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
						p, localDate, localDate.dayOfMonth().withMaximumValue()).fetch();
				PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
						p, month, year).first();

				tableMonthRecap.put(p, "Giorni di presenza al lavoro nei giorni festivi".intern(), new Integer(PersonUtility.workDayInHoliday(p, localDate.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), today)));
				tableMonthRecap.put(p, "Giorni di presenza al lavoro nei giorni lavorativi".intern(), new Integer(PersonUtility.workDayInWorkingDay(p, localDate.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), today)));
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
				List<Competence> compList = Competence.find("Select comp from Competence comp where comp.person = ? and comp.year = ? and " +
						"comp.competenceCode = ?", p, year, code).fetch();
				int valueCompetence = 0;
				for(Competence c : compList){
					valueCompetence = valueCompetence + c.valueApproved;
				}
				mealTicketToRender = mealTicketToRender + pm.numberOfMealTicketToRender();
				tableMonthRecap.put(p, "Ore di lavoro fatte".intern(), new Integer(timeAtWork));
				tableMonthRecap.put(p, "Differenza ore (Residuo a fine mese)".intern(), new Integer(difference));
				tableMonthRecap.put(p, "Assenze giustificate".intern(), new Integer(justifiedAbsence));
				tableMonthRecap.put(p, "Assenze non giustificate".intern(), new Integer(notJustifiedAbsence));
				tableMonthRecap.put(p, "Ore straord. pagate".intern(), new Integer(valueCompetence));
				tableMonthRecap.put(p, "Buoni mensa da restituire".intern(), new Integer(mealTicketToRender));
				timeAtWork = 0;
				difference = 0;
				mealTicketToRender = 0;
			}
			render(tableMonthRecap, numberOfWorkingDays, today, localDate);
		}				

	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void notJustifiedAbsences(Long id, int year, int month){
		Person person = Person.findById(id);
		LocalDate date = new LocalDate(year, month, 1);
		List<PersonDay> notJustifiedAbsences = new ArrayList<PersonDay>();
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, date, date.dayOfMonth().withMaximumValue()).fetch();
		for(PersonDay pd : pdList){
			if((pd.stampings.size() == 0 && pd.absences.size() == 0) || (pd.stampings.size() == 1))
				notJustifiedAbsences.add(pd);
		}

		render(notJustifiedAbsences);
	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void justifiedAbsences(Long id, int year, int month){
		Person person = Person.findById(id);
		LocalDate date = new LocalDate(year, month, 1);
		List<PersonDay> justifiedAbsences = new ArrayList<PersonDay>();
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, date, date.dayOfMonth().withMaximumValue()).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences.size() == 1 && pd.absences.get(0).absenceType.justifiedTimeAtWork.minutesJustified == null){
				justifiedAbsences.add(pd);
			}
		}

		render(justifiedAbsences);
	}

}
