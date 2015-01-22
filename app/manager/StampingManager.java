package manager;

import it.cnr.iit.epas.PersonUtility;

import java.util.List;

import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.exports.StampingFromClient;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;

import com.google.common.base.Optional;

import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;

public class StampingManager {

	/**
	 * Versione per inserimento amministratore.
	 * Costruisce la LocalDateTime della timbratura a partire dai parametri passati come argomento.
	 * @param year
	 * @param month
	 * @param day
	 * @param hourStamping N.B formato HHMM
	 * @return null in caso di formato ore non valido
	 */
	public static LocalDateTime buildStampingDateTime(int year, int month, int day, String hourStamping) {
		
		Integer hourNumber;
		Integer minNumber;
		
		try {
			hourNumber = Integer.parseInt(hourStamping.substring(0,2));
			minNumber = Integer.parseInt(hourStamping.substring(2,4));
		} catch(Exception e) {
			return null;
		}
		
		if(hourNumber < 0 || hourNumber > 23 || minNumber < 0 || minNumber > 59)  {
			return null;
		}
		
		return new LocalDateTime(year, month, day, hourNumber, minNumber, 0);
				
	}
	
	/**
	 * Crea e aggiunge una stamping al person day.
	 * @param pd
	 * @param time
	 * @param note
	 * @param service
	 * @param type
	 * @param markedByAdmin
	 */
	public static void addStamping(PersonDay pd, LocalDateTime time, String note,
			boolean service, boolean type, boolean markedByAdmin) {
		
		Stamping stamp = new Stamping();
		
		stamp.date = time; 
		stamp.markedByAdmin = markedByAdmin;
		
		if(service) {
			stamp.note = "timbratura di servizio";
			stamp.stampType = StampingDao.getStampTypeByCode("motiviDiServizio");
		}
		else {
			if(!note.equals(""))
				stamp.note = note;
			else
				stamp.note = "timbratura inserita dall'amministratore";
		}

		//in out: true->in false->out
		if(type){
			stamp.way = Stamping.WayType.in;
		}
		else{
			stamp.way = Stamping.WayType.out;
		}
		
		stamp.personDay = pd;
		stamp.save();
		pd.stampings.add(stamp);
		pd.save();
			
		PersonDayManager.updatePersonDaysFromDate(pd.person, pd.date);
	}
	
	/**
	 * Calcola il numero massimo di coppie ingresso/uscita nel personday di un giorno specifico 
	 * per tutte le persone presenti nella lista di persone attive a quella data.
	 * @param year
	 * @param month
	 * @param day
	 * @param activePersonsInDay
	 * @return 
	 */
	public static int maxNumberOfStampingsInMonth(Integer year, Integer month, Integer day, List<Person> activePersonsInDay){
		
		LocalDate date = new LocalDate(year, month, day);
		int max = 0;
			
		for(Person person : activePersonsInDay){
			PersonDay personDay = null;
			Optional<PersonDay> pd = PersonDayDao.getSinglePersonDay(person, date);

			if(pd.isPresent()) 
			{
				personDay = pd.get();
			
				if(max < PersonUtility.numberOfInOutInPersonDay(personDay))
					max = PersonUtility.numberOfInOutInPersonDay(personDay);
			}
		}
		return max;
	}
	

	/**
	 * metodo per la creazione di una timbratura a partire dall'oggetto stampModificationType che è stato costruito dal binder del Json
	 * passato dal client python
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static boolean createStamping(StampingFromClient stamping){

		if(stamping == null)
			return false;
		
		if(stamping.dateTime.isBefore(new LocalDateTime().minusMonths(1))){
			Logger.warn("La timbratura che si cerca di inserire è troppo precedente rispetto alla data odierna. Controllare il server!");
			return false;
		}
		Long id = stamping.personId;
		
		if(id == null){
			Logger.warn("L'id della persona passata tramite json non ha trovato corrispondenza nell'anagrafica del personale. Controllare id = null");
			return false;
		}
			
		Person person = PersonDao.getPersonById(id);
		//Person person = Person.findById(id);
		if(person == null){
			Logger.warn("L'id della persona passata tramite json non ha trovato corrispondenza nell'anagrafica del personale. Controllare id = %s", id);
			return false;
		}
		
		Logger.debug("Sto per segnare la timbratura di %s %s", person.name, person.surname);
		PersonDay personDay = null;
		Optional<PersonDay> pd = PersonDayDao.getSinglePersonDay(person, stamping.dateTime.toLocalDate());
//		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", 
//				person, stamping.dateTime.toLocalDate() ).first();
		if(!pd.isPresent()){
			/**
			 * non esiste un personDay per quella data, va creato e quindi salvato
			 */
			//Logger.debug("Non esiste il personDay...è il primo personDay per il giorno %s per %s %s", pd.date, person.name, person.surname);
			personDay = new PersonDay(person, stamping.dateTime.toLocalDate());
			personDay.save();		
			Logger.debug("Salvato il nuovo personDay %s", personDay);
			Stamping stamp = new Stamping();
			stamp.date = stamping.dateTime;
			stamp.markedByAdmin = false;
//			stamp.considerForCounting = true;
			if(stamping.inOut == 0)
				stamp.way = WayType.in;
			else
				stamp.way = WayType.out;
			stamp.stampType = stamping.stampType;
			stamp.badgeReader = stamping.badgeReader;
			stamp.personDay = personDay;
			stamp.save();
			personDay.stampings.add(stamp);
			personDay.save();

		}
		else{
			personDay = pd.get();
			if(checkDuplicateStamping(personDay, stamping) == false){
				Stamping stamp = new Stamping();
				stamp.date = stamping.dateTime;
				stamp.markedByAdmin = false;
//				stamp.considerForCounting = true;
				if(stamping.inOut == 0)
					stamp.way = WayType.in;
				else
					stamp.way = WayType.out;
				stamp.stampType = stamping.stampType;
				stamp.badgeReader = stamping.badgeReader;
				stamp.personDay = personDay;
				stamp.save();
				personDay.stampings.add(stamp);
				personDay.save();
			}
			else{
				Logger.info("All'interno della lista di timbrature di %s %s nel giorno %s c'è una timbratura uguale a quella passata dallo" +
						"stampingsFromClient: %s", person.name, person.surname, personDay.date, stamping.dateTime);
			}

			
		}
		Logger.debug("Chiamo la populatePersonDay per fare i calcoli sulla nuova timbratura inserita per il personDay %s", pd);
		PersonDayManager.populatePersonDay(personDay);

		personDay.save();
		return true;
	}
	
	


	/**
	 * 
	 * @param pd
	 * @param stamping
	 * @return true se all'interno della lista delle timbrature per quel personDay c'è una timbratura uguale a quella passata come parametro
	 * false altrimenti
	 */
	private static boolean checkDuplicateStamping(PersonDay pd, StampingFromClient stamping){
		for(Stamping s : pd.stampings){
			if(s.date.isEqual(stamping.dateTime)){
				return true;
			}
		}return false;
	}
	
}
