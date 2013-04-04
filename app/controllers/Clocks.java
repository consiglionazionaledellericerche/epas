package controllers;

import it.cnr.iit.epas.MainMenu;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.Stamping;
import models.Stamping.WayType;
import play.Logger;
import play.cache.Cache;
import play.mvc.Controller;

public class Clocks extends Controller{

	public static void show(){
		LocalDate data = new LocalDate();
		List<Person> personList = new ArrayList<Person>();
		MainMenu mainMenu = new MainMenu(data.getYear(),data.getMonthOfYear());
		List<Person> genericPerson = Person.find("Select p from Person p order by p.surname").fetch();
		for(Person p : genericPerson){
			Logger.debug("Cerco il contratto per %s %s per stabilire se metterlo/a in lista", p.name, p.surname);
			Contract c = Contract.find("Select c from Contract c where c.person = ? and ((c.beginContract != null and c.expireContract = null) or " +
					"(c.expireContract > ?)) order by c.beginContract desc limit 1", p, new LocalDate()).first();
			//Logger.debug("Il contratto per %s %s è: %s", p.name, p.surname, c.toString());
			if(c != null && c.onCertificate == true){
				personList.add(p);
				Logger.debug("Il contratto rispecchia i criteri quindi %s %s va in lista", p.name, p.surname);
			}

		}
		
		render(data, personList,mainMenu);
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
			pd = new PersonDay(person, ldt.toLocalDate());
			pd.save();
		}
		Stamping stamp = new Stamping();
		stamp.date = time;
		if(params.get("type").equals("true")){
			stamp.way = WayType.in;
		}
		else
			stamp.way = WayType.out;
		stamp.personDay = pd;
		stamp.save();
		pd.stampings.add(stamp);
		pd.merge();
		Logger.debug("Faccio i calcoli per %s %s sul personday %s chiamando la populatePersonDay", person.name, person.surname, pd);
		pd.populatePersonDay();
		//pd.save();
		flash.success("Aggiunta timbratura per %s %s", person.name, person.surname);
		show();
	}
}
