package manager;

import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.exports.StampingFromClient;

import org.joda.time.LocalDateTime;

import play.Logger;

import com.google.common.base.Optional;

import dao.PersonDao;
import dao.PersonDayDao;

public class StampingManager {

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
		personDay.populatePersonDay();

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
