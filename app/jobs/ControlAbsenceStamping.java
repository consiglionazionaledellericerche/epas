package jobs;

import java.util.List;

import javax.persistence.Query;

import models.Person;
import models.PersonDay;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.jobs.*;

//@On("0 0 7 1 * ?")
//@On("0 /5 * * * ?")
public class ControlAbsenceStamping extends Job{

	/**
	 * 
	 * invia una mail al dipendente e al responsabile del personale nel caso in cui questa persona, nel mese precedente abbia avuto giornate in cui non ha 
	 * fatto timbrature e non ha presentato codici di assenza
	 * 
	 * @throws EmailException 
	 */
	public void doJob() throws EmailException{
//		Long id = new Long(Play.configuration.getProperty("mail.person.cc"));
//		Person p = null;
//		if(id != null)
//			p = Person.findById(id);
		//List<Person> personList = Person.find("Select p from Person p where p.surname in (?,?) ", "Tagliaferri", "Del Soldato").fetch();
	//	List<Person> personList = Person.getActivePersons(new LocalDate());
		List<Person> personList = Person.getPeopleForTest();
		LocalDate date = new LocalDate();
		
		for(Person person : personList){
			Query query = JPA.em().createQuery("Select pd from PersonDay pd where pd.person = :person and pd.date between :begin and :end");
			query.setParameter("person", person).setParameter("begin", date.minusMonths(1).dayOfMonth().withMinimumValue()).setParameter("end", date.minusMonths(1).dayOfMonth().withMaximumValue());
			List<PersonDay> pdList = query.getResultList();
			String daysInTrouble = "";
			
			Logger.debug("Inizio a preparare la stringa delle giornate in cui %s %s può avere dei problemi...", person.name, person.surname);
			for(PersonDay pd : pdList){
				if((pd.stampings.size() == 0 && pd.absences.size() == 0)|| (pd.stampings.size() %2 != 0) && pd.absences.size() == 0){
					daysInTrouble = daysInTrouble + ' '+pd.date.toString()+'\n';
				}
			}
			Logger.debug("La stringa completa è: %s", daysInTrouble);
			if(!daysInTrouble.equals("")){
				Logger.debug("Inizio a preparare la mail per %s %s...", person.name, person.surname);
				SimpleEmail email = new SimpleEmail();
//				if(person.contactData != null && (!person.contactData.email.trim().isEmpty())){
//					Logger.debug("L'indirizzo a cui inviare la mail è: %s", person.contactData.email);
//					email.addTo(person.contactData.email);
//				}
				if(person != null && (!person.email.trim().isEmpty())){
					Logger.debug("L'indirizzo a cui inviare la mail è: %s", person.email);
					email.addTo(person.email);
				}
					
				else
					email.addTo(person.name+"."+person.surname+"@"+"iit.cnr.it");
				email.setHostName(Play.configuration.getProperty("mail.smtp.host"));
				Integer port = new Integer(Play.configuration.getProperty("mail.smtp.port"));
				email.setSmtpPort(port.intValue());
				email.setAuthentication(Play.configuration.getProperty("mail.smtp.user"), Play.configuration.getProperty("mail.smtp.pass"));
				
				email.setFrom(Play.configuration.getProperty("mail.from.alias"));
//				if(p != null)
//					email.addCc(p.contactData.email);
				email.setSubject("controllo giorni del mese");
				email.setMsg("Salve, controllare i giorni: "+daysInTrouble+ " per "+person.name+' '+person.surname);
				email.send();
				Logger.debug("inviata mail a %s %s", person.name, person.surname);
			}
			else{
				Logger.debug("Nessun giorno da segnalare per %s %s", person.name, person.surname);
			}
			
		}
		
		
		
	}
	

}
