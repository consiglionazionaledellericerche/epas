package manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import manager.recaps.personStamping.PersonStampingDayRecap;
import manager.recaps.personStamping.PersonStampingDayRecapFactory;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.exports.StampingFromClient;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Table;
import com.google.inject.Inject;

import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;
import dao.wrapper.IWrapperFactory;

public class StampingManager {

	@Inject
	public StampingManager(StampingDao stampingDao, 
			PersonDayDao personDayDao,
			PersonDao personDao,
			PersonDayManager personDayManager, 
			IWrapperFactory wrapperFactory,
			PersonStampingDayRecapFactory stampingDayRecapFactory) {

		this.stampingDao = stampingDao;
		this.personDayDao = personDayDao;
		this.personDao = personDao;
		this.personDayManager = personDayManager;
		this.wrapperFactory = wrapperFactory;
		this.stampingDayRecapFactory = stampingDayRecapFactory;
	}

	private final static Logger log = LoggerFactory.getLogger(StampingManager.class);

	private final StampingDao stampingDao;
	private final PersonDayDao personDayDao;
	private final PersonDao personDao;
	private final PersonDayManager personDayManager;
	private final IWrapperFactory wrapperFactory;
	private final PersonStampingDayRecapFactory stampingDayRecapFactory;

	/**
	 * Versione per inserimento amministratore.
	 * Costruisce la LocalDateTime della timbratura a partire dai parametri passati come argomento.
	 * @param year
	 * @param month
	 * @param day
	 * @param hourStamping N.B formato HHMM
	 * @return null in caso di formato ore non valido
	 */
	public LocalDateTime buildStampingDateTime(int year, int month, int day, String hourStamping) {

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
	public void addStamping(PersonDay pd, LocalDateTime time, String note,
			boolean service, boolean type, boolean markedByAdmin) {

		Stamping stamp = new Stamping();

		stamp.date = time; 
		stamp.markedByAdmin = markedByAdmin;

		if(service) {
			stamp.note = "timbratura di servizio";
			stamp.stampType = stampingDao.getStampTypeByCode("motiviDiServizio");
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

		//		TODO implementare un ricalcolo asincrono, per il momento è stato spostato nel controlle utilizzando i job play
		//		personDayManager.updatePersonDaysFromDate(pd.person, pd.date);
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
	public int maxNumberOfStampingsInMonth(Integer year, Integer month, Integer day, List<Person> activePersonsInDay){

		LocalDate date = new LocalDate(year, month, day);
		int max = 0;

		for(Person person : activePersonsInDay){
			PersonDay personDay = null;
			Optional<PersonDay> pd = personDayDao.getPersonDay(person, date);

			if(pd.isPresent()) 
			{
				personDay = pd.get();

				if(max < personDayManager.numberOfInOutInPersonDay(personDay))
					max = personDayManager.numberOfInOutInPersonDay(personDay);
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
	public boolean createStamping(StampingFromClient stamping){

		if(stamping == null)
			return false;

		if(stamping.dateTime.isBefore(new LocalDateTime().minusMonths(1))){
			log.warn("La timbratura che si cerca di inserire è troppo precedente rispetto alla data odierna. Controllare il server!");
			return false;
		}
		Long id = stamping.personId;

		if(id == null){
			log.warn("L'id della persona passata tramite json non ha trovato corrispondenza nell'anagrafica del personale. Controllare id = null");
			return false;
		}

		Person person = personDao.getPersonById(id);

		if(person == null){
			log.warn("L'id della persona passata tramite json non ha trovato corrispondenza nell'anagrafica del personale. Controllare id = {}", id);
			return false;
		}

		log.debug("Sto per segnare la timbratura di {}", person.getFullname());
		PersonDay personDay = null;
		Optional<PersonDay> pd = personDayDao.getPersonDay(person, stamping.dateTime.toLocalDate());
		if(!pd.isPresent()){
			/**
			 * non esiste un personDay per quella data, va creato e quindi salvato
			 */
			//Logger.debug("Non esiste il personDay...è il primo personDay per il giorno %s per %s %s", pd.date, person.name, person.surname);
			personDay = new PersonDay(person, stamping.dateTime.toLocalDate());
			personDay.save();		
			log.debug("Salvato il nuovo personDay {}", personDay);
			Stamping stamp = new Stamping();
			stamp.date = stamping.dateTime;
			stamp.markedByAdmin = false;
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
				log.info("All'interno della lista di timbrature di {} nel giorno {} c'è una timbratura uguale a quella passata dallo" +
						"stampingsFromClient: {}", new Object[]{person.getFullname(), personDay.date, stamping.dateTime});
			}


		}

		log.debug("Chiamo la populatePersonDay per fare i calcoli sulla nuova timbratura inserita per il personDay {}", pd);
		personDayManager.populatePersonDay(wrapperFactory.create(personDay));

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

	/**
	 * 
	 * @param stamping
	 * @param note
	 * @param stampingHour
	 * @param stampingMinute
	 * @param service
	 */
	public void persistStampingForUpdate(Stamping stamping, String note, int stampingHour, int stampingMinute, boolean service){
		stamping.date = stamping.date.withHourOfDay(stampingHour);
		stamping.date = stamping.date.withMinuteOfHour(stampingMinute);

		//TODO rivedere questi if se possono essere semplificati  
		if(service == false && (stamping.stampType == null || !stamping.stampType.identifier.equals("s"))){
			stamping.note = note;
		}
		if(service == true && (stamping.stampType == null || !stamping.stampType.identifier.equals("s"))){
			stamping.note = "timbratura di servizio";
			stamping.stampType = stampingDao.getStampTypeByCode("motiviDiServizio");
		}
		if(service == false && (stamping.stampType != null)){
			stamping.stampType = null;
			stamping.note = "timbratura inserita dall'amministratore";
		}
		if(service == true && (stamping.stampType != null || stamping.stampType.identifier.equals("s"))){
			stamping.note = note;
		}

		stamping.markedByAdmin = true;

		stamping.save();
	}


	/**
	 * La lista dei PersonStampingDayRecap renderizzata da presenza giornaliera.
	 * @param activePersonsInDay
	 * @param dayPresence
	 * @param numberOfInOut
	 * @return  
	 */
	public List<PersonStampingDayRecap> populatePersonStampingDayRecapList(
			List<Person> activePersonsInDay,
			LocalDate dayPresence, int numberOfInOut) {

		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();
		for(Person person : activePersonsInDay){

			PersonDay personDay = null;
			person = personDao.getPersonById(person.id);
			Optional<PersonDay> pd = personDayDao.getPersonDay(person, dayPresence); 

			if(!pd.isPresent()){
				personDay = new PersonDay(person, dayPresence);
				personDay.create();
			}
			else{
				personDay = pd.get();
			}

			personDayManager.computeValidStampings(personDay);
			daysRecap.add( stampingDayRecapFactory
					.create(personDay, numberOfInOut, Optional.<List<Contract>>absent()) );

		}
		return daysRecap;
	}


	/**
	 * 
	 * @param activePersons
	 * @param beginMonth
	 * @return la tabella contenente la struttura persona-data-buonopasto per tutte le persone attive
	 */
	public Table<Person, LocalDate, String> populatePersonTicketTable(List<Person> activePersons, LocalDate beginMonth){
		Builder<Person, LocalDate, String> builder = ImmutableTable.<Person, LocalDate, String>builder().orderColumnsBy(new Comparator<LocalDate>() {
			public int compare(LocalDate date1, LocalDate date2) {
				return date1.compareTo(date2);
			}
		}).orderRowsBy(new Comparator<Person>(){
			public int compare(Person p1, Person p2) {

				return p1.surname.compareTo(p2.surname);
			}

		});
		for(Person p : activePersons)
		{
			List<PersonDay> pdList = personDayDao
					.getPersonDayInPeriod(p, beginMonth, Optional.fromNullable(beginMonth.dayOfMonth().withMaximumValue()));

			for(PersonDay pd : pdList){
				if(pd.isTicketForcedByAdmin) {

					if(pd.isTicketAvailable) {
						builder.put(p, pd.date, "siAd");
					}
					else {
						builder.put(p, pd.date, "noAd");
					}
				}
				else {

					if(pd.isTicketAvailable) {
						builder.put(p, pd.date, "si");
					}
					else {
						builder.put(p, pd.date, "");
					}
				}    			

			}

		}
		Table<Person, LocalDate, String> tablePersonTicket = builder.build();
		return tablePersonTicket;
	}
}
