package controllers;

import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import models.ContactData;
import models.Contract;
import models.Location;
import models.MonthRecap;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.StampType;
import models.Stamping;
import models.Stamping.WayType;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Before;
import play.mvc.Controller;

public class Stampings extends Controller {

	/* corrisponde alla voce di menu selezionata */
	private final static ActionMenuItem actionMenuItem = ActionMenuItem.stampings;
	
    @Before
    static void checkPerson() {
        if(session.get(Application.PERSON_ID_SESSION_KEY) == null) {
            flash.error("Please log in first");
            Application.index();
        }
    }

    /**
     * 
     * @param id
     * metodo privato invocato dal metodo pubblico show() che ritorna il monthRecap, il personMonth della person che viene recuperata
     * tramite id oltre al menuItem
     */
    private static void show(Long id) {
    	String menuItem = actionMenuItem.toString();
    	
    	Person person = Person.findById(id);
    	
    	LocalDate now = new LocalDate();
    	Integer year = params.get("year") != null ? Integer.parseInt(params.get("year")) : now.getYear();
    	Integer month = params.get("month") != null ? Integer.parseInt(params.get("month")) : now.getMonthOfYear();
    	
    	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year, month);
    	PersonMonth personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and " +
    			"pm.month = ? and pm.year = ?", person, month, year).first();
    	if (personMonth == null) {
			personMonth = new PersonMonth(person, year, month);
		}
    	Logger.debug("Month recap of person.id %s, year=%s, month=%s", person.id, year, month);
        render(monthRecap, personMonth, menuItem);
    	
    	
    }

    public static void show() {
    	show(Long.parseLong(session.get(Application.PERSON_ID_SESSION_KEY)));
    }
    
    /**
     * metodo che prende i parametri di persona, anno e mese dalla showAdmin e li passa alla personStamping relativa a quella persona per quel mese
     * e per quell'anno
     */
    public static void choosePersonAndDate(){
    	String s = params.get("person");

    	if(s == null)
    		throw new NullPointerException("Persona non popolata");
    	else{
    		Logger.debug("La person é: "+s.toString());
    		System.out.println("La person è: "+s);
    	}
    	
    	long l = Long.parseLong(s);
    	Long id = new Long(l);
    	Person person = Person.findById(id);
    	Logger.debug("La person é: "+person.name+' '+person.surname);
    	
    	LocalDate now = new LocalDate();
    	Integer year = params.get("year") != null ? Integer.parseInt(params.get("year")) : now.getYear();
    	Logger.debug("L'anno é: "+year);
    	Integer month = params.get("month") != null ? Integer.parseInt(params.get("month")) : now.getMonthOfYear();
    	Logger.debug("Il mese é: "+month);   	
    	session.put("year", year);
    	session.put("month", month);
    	session.put("person_id", person.id);
    	//render(person, year,month);
    	Stampings.personStamping();
    }
    
    /**
     * 
     * @param person
     * @param year
     * @param month
     */
    
    private static void personStamping(Person person, int year, int month){
    	String menuItem = actionMenuItem.toString();
    	Logger.debug("Sono dentro la personStamping of person.id %s, year=%s, month=%s", person.id, year, month);
    	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year, month);
    	List<Person> personList = Person.find("Select p from Person p order by p.surname").fetch();
    	PersonMonth personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and " +
    			"pm.month = ? and pm.year = ?", person, month, year).first();
    	if (personMonth == null) {
			personMonth = new PersonMonth(person, year, month);
		}
    	Logger.debug("Month recap of person.id %s, year=%s, month=%s", person.id, year, month);
        render(monthRecap, personMonth, menuItem, personList);
    }
    
    public static void personStamping(){
    	Person person = null;
    	personStamping(person = Person.findById(Long.parseLong(session.get("person_id"))), Integer.parseInt(session.get("year")), Integer.parseInt(session.get("month")));
    }

    
    private static void showAdmin(Long id) {
    	String menuItem = actionMenuItem.toString();
    	
    	Person person = Person.findById(id);
    	
    	List<Person> personList = Person.find("Select p from Person p order by p.surname").fetch();
    	
    	LocalDate now = new LocalDate();
    	Integer year = params.get("year") != null ? Integer.parseInt(params.get("year")) : now.getYear();
    	Integer month = params.get("month") != null ? Integer.parseInt(params.get("month")) : now.getMonthOfYear();
    	
    	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year, month);

    	render(menuItem, personList, monthRecap);
    	
    	
    }

    public static void showAdmin() {
    	showAdmin(Long.parseLong(session.get(Application.PERSON_ID_SESSION_KEY)));
    }
    
    public static void dailyStampings() {
    	Person person = Person.findById(Long.parseLong(params.get("id")));
    	LocalDate day = 
    			new LocalDate(
    				Integer.parseInt(params.get("year")),
    				Integer.parseInt(params.get("month")), 
    				Integer.parseInt(params.get("day")));
    	
    	Logger.trace("dailyStampings called for %s %s", person, day);
    	
    	PersonDay personDay = new PersonDay(person, day);
    	render(personDay);
    }
    
    @Check(Security.INSERT_AND_UPDATE_STAMPING)
    public static void insertStamping(){
    	Person person = Person.findById(Long.parseLong(params.get("id")));
    	LocalDate day = 
    			new LocalDate(
    				Integer.parseInt(params.get("year")),
    				Integer.parseInt(params.get("month")), 
    				Integer.parseInt(params.get("day")));
    	
    	Logger.trace("Insert stamping called for %s %s", person, day);
    	
    	PersonDay personDay = new PersonDay(person, day);
    	render(personDay);
    }
    
    @Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void save(@Valid @Required Person person, @Valid List<Stamping> stamping) {
		if(validation.hasErrors()) {
			if(request.isAjax()) error("Invalid value");
			render("@show", person, stamping);
		}
		
		person.save();
		/**
		 * guardo quante e quali timbrature sono state modificate e per queste genero i nuovi stamping o aggiorno i già esistenti 
		 * TODO: completare la select per il recupero dell'eventuale già presente timbratura da aggiornare
		 */
		
		Stamping stamp = null;
		int count = 0;
		LocalDateTime startOfDay = new LocalDateTime(stamping.get(count).date.getYear(),stamping.get(count).date.getMonthOfYear(),stamping.get(count).date.getDayOfMonth(),0,0);
		LocalDateTime endOfDay = new LocalDateTime(stamping.get(count).date.getYear(),stamping.get(count).date.getMonthOfYear(),stamping.get(count).date.getDayOfMonth(),23,59);
		List<Stamping> stamps = Stamping.find("Select st from Stamping st where st.person = ? " +
				"and st.date between ? and ? order by st.date", person,startOfDay,endOfDay).fetch();
		while(count <= stamping.size()){
			if(stamping.get(count) != null){
				stamp = stamps.get(count);				
				if(stamp == null){
					stamp = new Stamping();
					stamp.date = stamping.get(count).date;
					stamp.person = person;
					stamp.markedByAdmin = true;
					if(count == 0 || count == 2 || count == 4 || count == 6){
						stamp.way = WayType.in;
					}
					else
						stamp.way = WayType.out;
					stamp.save();
				}
				else{
					stamp.date = stamping.get(count).date;
					stamp.markedByAdmin = true;
					stamp.save();
				}				
														
			}
			count++;
		}		
		/**
		 * a questo punto devono essere ricalcolati tutti i valori del timeAtWork, del progressive e della difference oltre che 
		 * dell'assegnazione del buono pasto sul personDay comprendente la nuova timbratura
		 */
		boolean flag = false;
		int i = 0;
		Stamping stamp1 = null;
		while(flag == false && i<stamping.size()){
			stamp1 = stamping.get(i);
			if(stamp1 != null && stamp1.date != null){
				LocalDate date = new LocalDate(stamp1.date);
				PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person,date).first();
				pd.setStampings();
				pd.setTimeAtWork();
				pd.setDifference();
				pd.setProgressive();
				pd.setTicketAvailable();
				pd.save();
				/**
				 * TODO: applicare la logica del ricalcolo mensile e annuale se mi trovo nel primo giorno del nuovo mese o del nuovo anno
				 */
				flag = true;
			}
			else{
				i++;
			}
			
		}		
		
		showAdmin();
	}
    
    @Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void discard(){
		showAdmin();
	}
}
