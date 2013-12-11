package controllers;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.MainMenu;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import models.Configuration;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.StampModificationType;
import models.StampType;
import models.Stamping;
import models.Stamping.WayType;
import models.exports.StampingFromClient;
import models.rendering.PersonStampingDayRecap;
import play.Logger;
import play.cache.Cache;
import play.mvc.Controller;

public class Clocks extends Controller{

	public static void show(){
		LocalDate data = new LocalDate();
		MainMenu mainMenu = new MainMenu(data.getYear(),data.getMonthOfYear());
		List<Person> personList = Person.getActivePersonsInMonth(data.getMonthOfYear(), data.getYear());
		render(data, personList,mainMenu);
	}
	
	
	public static void clockLogin(Long personId, String password)
	{
		LocalDate today = new LocalDate();
		
		if(personId==0)
		{
			flash.error("Utente non selezionato");
			Clocks.show();
		}
		
		
		
		Person person = Person.find("SELECT p FROM Person p where id = ? and password = md5(?)",personId, password).first();
		if(person == null)
		{
			flash.error("Password non corretta");
			Clocks.show();
		}
		
	
		//TODO 18/10 creare un metodo statico in models.person getPersonMonth(int year, int month)
		PersonMonth personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?",
				person, today.getMonthOfYear(), today.getYear()).first();
		
		//calcolo del valore valid per le stamping del mese
		personMonth.getDays();
		for(PersonDay pd : personMonth.days)
		{
			pd.computeValidStampings();
		}		

		//numero di colonne da visualizzare
		Configuration conf = Configuration.getCurrentConfiguration();
		int minInOutColumn = conf.numberOfViewingCoupleColumn;

	

		
		//Nuova struttura dati per stampare
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();							

		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, today).first();
		if(pd == null){
			Logger.debug("Prima timbratura per %s %s non c'è il personday quindi va creato.", person.name, person.surname);
			pd = new PersonDay(person, today);
			pd.save();
		}
		
		//numero di colonne da stampare
		int numberOfInOut = Math.max(minInOutColumn,  PersonUtility.numberOfInOutInPersonDay(pd));
		
	
		PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(pd,numberOfInOut);
		
		render(person, dayRecap, numberOfInOut);
	}
	
	

	/**
	 * 
	 * @param personId. Con questo metodo si permette l'inserimento della timbratura per la persona contrassegnata da id personId.
	 */
	public static void insertStamping(Long personId){
		Person person = Person.findById(personId);
		if(person == null)
			throw new IllegalArgumentException("Persona non trovata!!!! Controllare l'id!");
		LocalDateTime ldt = new LocalDateTime();
		LocalDateTime time = new LocalDateTime(ldt.getYear(),ldt.getMonthOfYear(),ldt.getDayOfMonth(),ldt.getHourOfDay(),ldt.getMinuteOfHour(),0);
		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, ldt.toLocalDate()).first();
		if(pd == null){
			Logger.debug("Prima timbratura per %s %s non c'è il personday quindi va creato.", person.name, person.surname);
			pd = new PersonDay(person, ldt.toLocalDate());
			pd.save();
		}
		
		//Se la stamping esiste già mostro il riepilogo
		int minNew = time.getMinuteOfHour();
		for(Stamping s : pd.stampings){
			int min = s.date.getMinuteOfHour();
			int minMinusOne = s.date.plusMinutes(1).getMinuteOfHour();
			if(minNew==min || minNew==minMinusOne)
			{
				
				flash.error("Timbratura ore %s gia' inserita, prossima timbratura accettata a partire da %s",
						DateUtility.fromLocalDateTimeHourTime(s.date),
						DateUtility.fromLocalDateTimeHourTime(s.date.plusMinutes(2)));
				
				Clocks.showRecap(personId);
			}
		}

		
		//Altrimenti la inserisco
		Stamping stamp = new Stamping();
		stamp.date = time;
		if(params.get("type").equals("true")){
			stamp.way = WayType.in;
		}
		else
			stamp.way = WayType.out;
		stamp.personDay = pd;
		stamp.markedByAdmin = false;
		stamp.save();
		pd.stampings.add(stamp);
		pd.save();
		
		Logger.debug("Faccio i calcoli per %s %s sul personday %s chiamando la populatePersonDay", person.name, person.surname, pd);
		pd.populatePersonDay();
		pd.updatePersonDay();
		//pd.save();
		flash.success("Aggiunta timbratura per %s %s", person.name, person.surname);
		
		Clocks.showRecap(personId);
	}
	
	public static void showRecap(Long personId)
	{
		Person person = Person.findById(personId);
		if(person == null)
			throw new IllegalArgumentException("Persona non trovata!!!! Controllare l'id!");
		
		LocalDate today = new LocalDate();
		//TODO 18/10 creare un metodo statico in models.person getPersonMonth(int year, int month)
		PersonMonth personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?",
				person, today.getMonthOfYear(), today.getYear()).first();
		
		//calcolo del valore valid per le stamping del mese
		personMonth.getDays();
		for(PersonDay pd : personMonth.days)
		{
			pd.computeValidStampings();
		}		

		//numero di colonne da visualizzare
		Configuration conf = Configuration.getCurrentConfiguration();
		int minInOutColumn = conf.numberOfViewingCoupleColumn;

	

		
		//Nuova struttura dati per stampare
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();							

		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, today).first();
		if(pd == null){
			Logger.debug("Prima timbratura per %s %s non c'è il personday quindi va creato.", person.name, person.surname);
			pd = new PersonDay(person, today);
			pd.save();
		}
		
		//numero di colonne da stampare
		int numberOfInOut = Math.max(minInOutColumn,  PersonUtility.numberOfInOutInPersonDay(pd));
		
	
		PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(pd,numberOfInOut);
		
		render(person, dayRecap, numberOfInOut);
		
	}

}
