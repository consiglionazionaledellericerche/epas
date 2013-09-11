package controllers;

import it.cnr.iit.epas.ActionMenuItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import models.Absence;
import models.AbsenceType;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.YearRecap;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import controllers.rendering.YearlyAbsencesPerPerson;

@With( {Secure.class, NavigationMenu.class} )
public class YearlyAbsences extends Controller{

	public final static class AbsenceTypeDays{
		//public AbsenceType absenceCode;
		public String absenceCode;
		public Integer i;

		public AbsenceTypeDays(String absenceCode, Integer i){
			this.absenceCode = absenceCode;
			this.i = i;
		}

		public AbsenceTypeDays(String absenceCode){
			this.absenceCode = absenceCode;
			this.i = null;
		}

		public AbsenceTypeDays(){
			this.absenceCode = null;
			this.i = null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((absenceCode == null) ? 0 : absenceCode.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AbsenceTypeDays other = (AbsenceTypeDays) obj;
			if (absenceCode == null) {
				if (other.absenceCode != null)
					return false;
			} else if (!absenceCode.equals(other.absenceCode))
				return false;
			return true;
		}

	}

	public final static class AbsenceTypeDate{
		public AbsenceType absenceType;
		public LocalDate date;

		public AbsenceTypeDate(AbsenceType absenceType, LocalDate date){
			this.absenceType = absenceType;
			this.date = date;
		}
	}

	@Check(Security.VIEW_PERSON_LIST)
	public static void yearlyAbsences(Long personId, int year) {
		//controllo sui parametri
		Person person = null;
		if(personId == null)
			person = Security.getPerson();
		else
			person = Person.findById(personId);
		Integer anno = params.get("year", Integer.class);
		Logger.debug("L'id della persona è: %s", personId);
		Logger.debug("La persona è: %s %s", person.name, person.surname);
		Logger.trace("Anno: "+anno);
		
		//rendering 
		if(anno==null){
			LocalDate now = new LocalDate();
			YearlyAbsencesPerPerson yappRender = new YearlyAbsencesPerPerson(person, (short)now.getYear());
			render(yappRender);
		}
		else{
			YearlyAbsencesPerPerson yappRender = new YearlyAbsencesPerPerson(person, (short)anno.intValue());
			render(yappRender);
		}
		
		
		
	}


	/**
	 * 
	 * @param year
	 * @param month
	 * fa la render della mappa contenente come chiave la persona e come valore una lista di oggetti di tipo AbsenceTypeDays che contengono
	 * l'absenceType di una assenza e il numero di volte che quell'assenza viene fatta nel mese e nell'anno passati come parametri del metodo
	 */


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

	@Check(Security.VIEW_PERSON_LIST)
	public static void showGeneralMonthlyAbsences(int year, int month) throws InstantiationException, IllegalAccessException{

		Table<Person, String, Integer> tableMonthlyAbsences = TreeBasedTable.create(PersonNameComparator, AbsenceCodeComparator);
		if(month == 0){
			/**
			 * caso in cui si vogliono le assenze di tutti i mesi dell'anno in corso fino alla data attuale
			 */
			List<Person> activePersons = Person.getActivePersons(new LocalDate(year, 1, 1));
			for(Person p : activePersons){
				List<Absence> absenceInMonth = Absence.find("Select abs from Absence abs, PersonDay pd where abs.personDay = pd and " +
						"pd.person = ? and pd.date >= ? and pd.date <= ?", 
						p, new LocalDate(year, 1, 1), new LocalDate()).fetch();
				tableMonthlyAbsences.put(p, "Totale", absenceInMonth.size());
				for(Absence abs : absenceInMonth){
					Integer value = tableMonthlyAbsences.row(p).get(abs.absenceType.code);
					Logger.debug("Per la persona %s il codice %s vale: %s", p, abs.absenceType.code, value);
					if(value == null){
						Logger.debug("Inserisco in tabella nuova assenza per %s con codice %s", p, abs.absenceType.code);
						tableMonthlyAbsences.row(p).put(abs.absenceType.code, 1);
					}
					else{
						tableMonthlyAbsences.row(p).put(abs.absenceType.code, value+1);
						Logger.debug("Incremento il numero di giorni per l'assenza %s di %s al valore %s", abs.absenceType.code, p, value+1);

					}
				}
			}
		}
		else{
			List<Person> activePersons = Person.getActivePersons(new LocalDate(year, month, 1));
			//Table<Person, String, Integer> tableMonthlyAbsences = ArrayTable.create(activePersons, absenceInMonth);


			for(Person p : activePersons){
				List<Absence> absenceInMonth = Absence.find("Select abs from Absence abs, PersonDay pd where abs.personDay = pd and " +
						"pd.person = ? and pd.date >= ? and pd.date <= ?", 
						p, new LocalDate(year, month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).fetch();

				tableMonthlyAbsences.put(p, "Totale", absenceInMonth.size());
				for(Absence abs : absenceInMonth){
					Integer value = tableMonthlyAbsences.row(p).get(abs.absenceType.code);
					Logger.debug("Per la persona %s il codice %s vale: %s", p, abs.absenceType.code, value);
					if(value == null){
						Logger.debug("Inserisco in tabella nuova assenza per %s con codice %s", p, abs.absenceType.code);
						tableMonthlyAbsences.row(p).put(abs.absenceType.code, 1);
					}
					else{
						tableMonthlyAbsences.row(p).put(abs.absenceType.code, value+1);
						Logger.debug("Incremento il numero di giorni per l'assenza %s di %s al valore %s", abs.absenceType.code, p, value+1);

					}
				}
			}
		}
		int numberOfDifferentAbsenceType = tableMonthlyAbsences.columnKeySet().size();
		render(tableMonthlyAbsences, year, month,numberOfDifferentAbsenceType);

	}
	//private final static ActionMenuItem actionMenuItem = ActionMenuItem.absencesperperson;

	/**
	 * 
	 * @param personId
	 * @param year
	 * Render della pagina absencePerPerson.html che riassume le assenze annuali di una persona
	 */
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void absencesPerPerson(Long personId, Integer year){
		
		//controllo sui parametri
		Logger.debug("Anno: %d Id: %d", year, personId);
		Person person = null;
		if(personId == null || personId == 0)
			person = Security.getPerson();			//prende la persona collegata
		else
			person = Person.findById(personId);
		Integer anno = params.get("year", Integer.class);
		Logger.debug("La persona correntemente loggata è: %s", person);
		Logger.trace("Anno: "+anno);

		//rendering 
		if(anno==null){
			LocalDate now = new LocalDate();
			YearlyAbsencesPerPerson yappRender = new YearlyAbsencesPerPerson(person, (short)now.getYear());
			render(yappRender);
		}
		else{
			YearlyAbsencesPerPerson yappRender = new YearlyAbsencesPerPerson(person, (short)anno.intValue());
			render(yappRender);
		}


	}


}
