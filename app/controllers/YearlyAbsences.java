package controllers;

import helpers.ModelQuery.SimpleResults;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import models.Absence;
import models.AbsenceType;
import models.Person;
import models.User;
import models.rendering.YearlyAbsencesRecap;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dao.PersonDao;

@With( {Resecure.class, RequestInit.class} )
public class YearlyAbsences extends Controller{

	@Inject
	static SecurityRules rules;
	
	
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

	
	public static void yearlyAbsences(Long personId, int year) {
		//controllo sui parametri
		Person person = null;
		if(personId == null)
			person = Security.getUser().get().person;
		else
			person = Person.findById(personId);
		
		rules.checkIfPermitted("");
		Integer anno = params.get("year", Integer.class);
		Logger.debug("L'id della persona è: %s", personId);
		Logger.debug("La persona è: %s %s", person.name, person.surname);
		Logger.trace("Anno: "+anno);
		
		//rendering 
		if(anno==null){
			LocalDate now = new LocalDate();
			YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(person, (short)now.getYear());
			render(yearlyAbsencesRecap, year);
		}
		else{
			YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(person, (short)anno.intValue());
			render(yearlyAbsencesRecap, year, personId, person);
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

			if(name1.equals(name2))
				return person1.name.toUpperCase().compareTo(person2.name.toUpperCase());
			return name1.compareTo(name2);

		}

	};	

	private static Comparator<AbsenceType> AbsenceCodeComparator = new Comparator<AbsenceType>(){

		public int compare(AbsenceType absenceCode1, AbsenceType absenceCode2){
			return absenceCode1.code.compareTo(absenceCode2.code);

		}		

	};

	
	public static void showGeneralMonthlyAbsences(int year, int month, String name, Integer page) {

		rules.checkIfPermitted("");
		
		if(page==null)
			page=0;
				
		Table<Person, AbsenceType, Integer> tableMonthlyAbsences = TreeBasedTable.create(PersonNameComparator, AbsenceCodeComparator);
		AbsenceType abt = new AbsenceType();
		abt.code = "Totale";
		
		
		SimpleResults<Person> simpleResults = PersonDao.list(Optional.fromNullable(name), 
				Sets.newHashSet(Security.getOfficeAllowed()), 
				false, new LocalDate(year, month,1), 
				new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(), true);

		List<Person> persons = simpleResults.paginated(page).getResults();

		for(Person p : persons){
			List<Absence> absenceInMonth = Absence.find("Select abs from Absence abs where " +
					"abs.personDay.person = ? and abs.personDay.date >= ? and abs.personDay.date <= ?", 
					p, new LocalDate(year, month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).fetch();

			tableMonthlyAbsences.put(p, abt, absenceInMonth.size());
			for(Absence abs : absenceInMonth){
				Integer value = tableMonthlyAbsences.row(p).get(abs.absenceType);
				Logger.debug("Per la persona %s il codice %s vale: %s", p, abs.absenceType.code, value);
				if(value == null){
					Logger.debug("Inserisco in tabella nuova assenza per %s con codice %s", p, abs.absenceType.code);
					tableMonthlyAbsences.row(p).put(abs.absenceType, 1);
				}
				else{
					tableMonthlyAbsences.row(p).put(abs.absenceType, value+1);
					Logger.debug("Incremento il numero di giorni per l'assenza %s di %s al valore %s", abs.absenceType.code, p, value+1);

				}
			}
		}

		int numberOfDifferentAbsenceType = tableMonthlyAbsences.columnKeySet().size();
		
		if (!Strings.isNullOrEmpty(name)) {
			Logger.info("filtrare per nome qui... %s", name);
			// TODO: filtrare per nome tableMonthly...
		}

		render(tableMonthlyAbsences, year, month,numberOfDifferentAbsenceType, simpleResults, name, page);

	}
	//private final static ActionMenuItem actionMenuItem = ActionMenuItem.absencesperperson;

	/**
	 * 
	 * @param personId
	 * @param year
	 * Render della pagina absencePerPerson.html che riassume le assenze annuali di una persona
	 */
	
	public static void absencesPerPerson(Integer year){
		
		//controllo sui parametri
		Optional<User> currentUser = Security.getUser();
		if( !currentUser.isPresent() || currentUser.get().person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
		User user = currentUser.get();
		//rendering 
		if(year==null){
			LocalDate now = new LocalDate();
			YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(user.person, (short)now.getYear());
			render(yearlyAbsencesRecap);
		}
		else{
			YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(user.person, (short)year.intValue());
			render(yearlyAbsencesRecap);
		}
	}
	
	
	public static void showPersonMonthlyAbsences(Long personId, Integer year, Integer month, String absenceTypeCode) throws InstantiationException, IllegalAccessException
	{
		
		LocalDate monthBegin = new LocalDate(year, month, 1);
		LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
		
		Person person = Person.findById(personId);	
		if(person == null){
			flash.error("Persona inesistente");
			YearlyAbsences.showGeneralMonthlyAbsences(year, month, null, null);
		}
			
		rules.checkIfPermitted(person.office);
		
			
		
		List<Absence> absenceToRender = new ArrayList<Absence>();
		
		if(absenceTypeCode.equals("Totale"))
		{
			absenceToRender = Absence.find("Select ab from Absence ab, PersonDay pd where ab.personDay = pd and pd.date between ? and ? and pd.person = ? order by pd.date", 
					monthBegin, monthEnd, person).fetch();
		}
		else
		{
			absenceToRender = Absence.find("Select ab from Absence ab, PersonDay pd where ab.personDay = pd and pd.date between ? and ? and pd.person = ? and ab.absenceType.code = ? order by pd.date", 
					monthBegin, monthEnd, person, absenceTypeCode).fetch();
		}
		
		render(person, absenceToRender);
	}


}
