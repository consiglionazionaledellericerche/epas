package controllers;

import java.util.ArrayList;
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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

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
		Person person = null;
		if(personId == null)
			person = Security.getPerson();
		else
			person = Person.findById(personId);
		Integer anno = params.get("year", Integer.class);
		//Long personId = params.get("personId", Long.class);
		Logger.debug("L'id della persona è: %s", personId);
		Logger.debug("La persona è: %s %s", person.name, person.surname);
		//person = Person.findById(personId);
		Logger.trace("Anno: "+anno);

		if(anno==null){

			LocalDate now = new LocalDate();
			YearRecap yearRecap = YearRecap.byPersonAndYear(person, (short)now.getYear());
			render(yearRecap);
		}
		else{
			Logger.info("Sono dentro il ramo else della creazione dell'yearRecap");
			//Integer year = new Integer(params.get("year"));
		//	PersonMonth personMonth = PersonMonth.byPersonAndYearAndMonth(person, year, month);
			YearRecap yearRecap = YearRecap.byPersonAndYear(person, (short)year);

			render(yearRecap);
		}

	}


	/**
	 * 
	 * @param year
	 * @param month
	 * fa la render della mappa contenente come chiave la persona e come valore una lista di oggetti di tipo AbsenceTypeDays che contengono
	 * l'absenceType di una assenza e il numero di volte che quell'assenza viene fatta nel mese e nell'anno passati come parametri del metodo
	 */

	@Check(Security.VIEW_PERSON_LIST)
	public static void showGeneralMonthlyAbsences(int year, int month) throws InstantiationException, IllegalAccessException{
		Table<Person, String, Integer> tablePersonAbsences =  HashBasedTable.create();
		
		List<Person> activePersons = Person.getActivePersons(new LocalDate(year, month, 1));
		for(Person p : activePersons){
			List<Absence> absenceInMonth = Absence.find("Select abs from Absence abs, PersonDay pd where abs.personDay = pd and " +
					"pd.person = ? and pd.date >= ? and pd.date <= ?", 
					p, new LocalDate(year, month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).fetch();
			tablePersonAbsences.put(p, "Totale", absenceInMonth.size());
			Logger.debug("Inserisco la persona %s in tabella con il codice di assenza %s per la prima volta", p, "");
			for(Absence abs : absenceInMonth){
				Integer value = tablePersonAbsences.row(p).get(abs.absenceType.code);
				Logger.debug("Per la persona %s il codice %s vale: %s", p, abs.absenceType.code, value);
				if(value == null){
					Logger.debug("Inserisco in tabella nuova assenza per %s con codice %s", p, abs.absenceType.code);
					tablePersonAbsences.row(p).put(abs.absenceType.code, 1);
				}
				else{
					tablePersonAbsences.row(p).put(abs.absenceType.code, value+1);
					Logger.debug("Incremento il numero di giorni per l'assenza %s di %s al valore %s", abs.absenceType.code, p, value+1);
					
				}
			}

		}
		int numberOfDifferentAbsenceType = tablePersonAbsences.columnKeySet().size();
		
		//Logger.debug("Lista dei codici di assenza: %s", tablePersonAbsences.columnKeySet());
		render(tablePersonAbsences, year, month,numberOfDifferentAbsenceType);

	}

	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void absencesPerPerson(Long personId, int year){
		Person person = null;
		if(personId == null)
			person = Security.getPerson();
		Integer anno = params.get("year", Integer.class);
		Logger.debug("L'id della persona è: %s", personId);
		Logger.trace("Anno: "+anno);

		if(anno==null){
			LocalDate now = new LocalDate();
			YearRecap yearRecap = YearRecap.byPersonAndYear(person, (short)now.getYear());
			render(yearRecap);
		}
		else{
			Logger.info("Sono dentro il ramo else della creazione del month recap");
			YearRecap yearRecap = YearRecap.byPersonAndYear(person, (short)year);
			render(yearRecap);
		}
		
		//Logger.debug("I parametri passati alla funzione sono: %s %s %s %s", personId, absenceCode, month, year);
//		List<LocalDate> dateAbsenceList = new ArrayList<LocalDate>();
//		List<PersonDay> pdList = null;
//		Person person = Person.findById(personId);
//		List<AbsenceTypeDate> listaAssenzeDate = null;
//		int tipo;
//		if(absenceCode.equalsIgnoreCase("totale")){
//			tipo = 1;
//			listaAssenzeDate = new ArrayList<AbsenceTypeDate>();
//			pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date >= ? and pd.date <= ?", 
//					person, new LocalDate(year,month,1), new LocalDate(year,month,1).monthOfYear().withMaximumValue()).fetch();
//			for(PersonDay pd : pdList){
//				if(pd.absences.size()>0){
//					for(Absence abs : pd.absences){
//						AbsenceTypeDate abtd = new AbsenceTypeDate(abs.absenceType, pd.date);
//						listaAssenzeDate.add(abtd);
//					}
//				}
//			}
//			render(person,year,month,listaAssenzeDate, tipo);
//		}
//		else{
//			tipo = 0;
//			AbsenceType abt = AbsenceType.find("Select abt from AbsenceType abt where abt.code = ?", absenceCode).first();
//			pdList = PersonDay.find("Select pd from PersonDay pd, Absence abs where abs.personDay = pd and pd.person = ? and " +
//					"pd.date >= ? and pd.date <= ?", person, new LocalDate(year,month,1), new LocalDate(year, month, 1).monthOfYear().withMaximumValue()).fetch();
//			for(PersonDay pd : pdList){
//				dateAbsenceList.add(pd.date);
//			}
//			render(dateAbsenceList, abt, person, year, month, tipo);		
//		}
	}


}
