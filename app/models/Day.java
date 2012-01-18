package models;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;


public class Day {

	private Person person;
	
	//private boolean mealTicket;
	
	//private List<AbsenceType> absenceTypeList;
	
	//private List<Stamping> stampingList;
	
	private String workingTimeType;
	
	private int timeAtWork;
	
	private int difference;
	
	private int progressive;
		
	private static Connection postgresqlConn = null;
	public static String myPostgresDriver = Play.configuration.getProperty("db.new.driver");
	
	
	public static Connection getMyPostgresConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		if (postgresqlConn != null ) {
			return postgresqlConn;
		}
		Class.forName(myPostgresDriver).newInstance();

		return DriverManager.getConnection(
				
			Play.configuration.getProperty("db.new.url"),
			Play.configuration.getProperty("db.new.user"),
			Play.configuration.getProperty("db.new.password"));
				
	}
	
	public int mealTicketToUse(){
		return 0;
		
	}
	
	public int mealTicketToReturn(){
		return 0;
	}
	
	public int officeWorkingDay(){
		return 0;
	}
	
	/**
	 * 
	 * @param person
	 * @param date
	 * @return la lista di codici di assenza fatti da quella persona in quella data
	 */
	public List<AbsenceType> absenceList(Person person, Date date){
				
		List<AbsenceType> listaAssenze = new ArrayList<AbsenceType>();
		listaAssenze = AbsenceType.find("Select at, abs from AbsenceType at, Absence abs where " +
				"at.Absence = abs and abs.person = ? and abs.date = ? ", person, date).fetch();
		return listaAssenze;
		
	}	
	
	/**
	 * 
	 * @param date
	 * @return calcola il numero di minuti di cui è composta la data passata come parametro (di cui considera solo
	 * ora e minuti
	 */
	@SuppressWarnings("deprecation")
	public int toMinute(LocalDate date){
		int dateToMinute = 0;
		
		if (date!=null){
			//int hour = date.getHours();
			//int minute = date.getMinutes();
			//dateToMinute = (60*hour)+minute;
		}
		return dateToMinute;
	}
	
	/**
	 * 
	 * @param date
	 * @return la LocalDate creata a partire dai parametri di giorno mese e anno presi dalla data passata come parametro.
	 * N.B.: questo metodo è creato per essere usato nei casi in cui sia necessario creare esclusivamente la data senza bisogno dell'ora
	 * (vedi metodo isWorkingDay(Person person, Date date) ).
	 */
	public LocalDate toLocalDate(Date date){
		String s = date.toString();		
		
		int anno = Integer.parseInt(s.substring(0, 4));
		
		int mese = Integer.parseInt(s.substring(5, 7));
		
		int giorno = Integer.parseInt(s.substring(8, 10));
		
		LocalDate dataLocale = new LocalDate(anno, mese, giorno);
		
		return dataLocale;
	}
	
	/**
	 * 
	 * @param data
	 * @return true se il giorno in questione è un giorno di festa. False altrimenti
	 */
	public boolean isHoliday(Date data){
		if (data!=null){
			
			LocalDate date = new LocalDate(); 
			date.fromDateFields(data);
			Logger.warn("Nel metodo isHoliday la data è: " +date);
			//System.out.println("Nel metodo idHoliday la data è: " +date);
			if(date.getDayOfWeek() == 7)
				return true;		
			if((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 25))
				return true;
			if((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 26))
				return true;
			if((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 8))
				return true;
			if((date.getMonthOfYear() == 6) && (date.getDayOfMonth() == 2))
				return true;
			if((date.getMonthOfYear() == 4) && (date.getDayOfMonth() == 25))
				return true;
			if((date.getMonthOfYear() == 5) && (date.getDayOfMonth() == 1))
				return true;
			if((date.getMonthOfYear() == 8) && (date.getDayOfMonth() == 15))
				return true;
			if((date.getMonthOfYear() == 1) && (date.getDayOfMonth() == 1))
				return true;
			if((date.getMonthOfYear() == 1) && (date.getDayOfMonth() == 6))
				return true;
			if((date.getMonthOfYear() == 11) && (date.getDayOfMonth() == 1))
				return true;			
		}
		return false;
	}
	
	/**
	 * 
	 * @param date
	 * @param person
	 * @return true se per quella data, quella persona aveva un giorno di lavoro. False altrimenti
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public boolean isWorkingDay(Date date, Person person) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		
		LocalDate data = toLocalDate(date);
		boolean isHoliday = false;
		int day = data.getDayOfWeek();
		//EntityManager em = JPA.em();
		Connection mypostgresCon = getMyPostgresConnection();
		PreparedStatement stmt = mypostgresCon.prepareStatement("Select wttd.holiday from WorkingTimeTypeDay wttd, WorkingTimeType w" +
				"where wttd.workingTimeType = w and w.person_id = " +person+ "and w.dayOfWeek = " +day);
		ResultSet rs = stmt.executeQuery();
		while(rs.next()){	
			if(rs.getBoolean("holiday") == true)
				isHoliday = true;
			
			else
				isHoliday = false;
			
		}
		return isHoliday;
	}
	
	/**
	 * 
	 * @param date
	 * @return numero di minuti in cui una persona è stata a lavoro in quella data
	 */
	public int timeAtWork(Person person, Date date){
		
		List<Stamping> listStamp = Stamping.find("select s from Stamping s " +
			    "where s.person = ? and s.date = ? order by date", person, date).fetch();
		
		int size = listStamp.size();
		timeAtWork = 0;
		if(size%2 != 0){
			/**
			 * vuol dire che ci sono più timbrature di ingresso rispetto alle uscite o viceversa,
			 * come devo gestire questa cosa???
			 */
		}
		else{
			
			Iterator<Stamping> iter = listStamp.iterator();
			while(iter.hasNext()){
				Stamping s = iter.next();
				if(s.way == Stamping.WayType.in){
				//	timeAtWork -= toMinute(s.date);		
					System.out.println("Timbratura di ingresso: "+timeAtWork);
				}
				if(s.way == Stamping.WayType.out){
				//	timeAtWork += toMinute(s.date);
					System.out.println("Timbratura di uscita: "+timeAtWork);
				}
			}
		}
		System.out.println("Totale: "+timeAtWork);
		return timeAtWork;
		
	}
	
	/**
	 * 
	 * @param date
	 * @return la differenza in minuti tra l'orario giornaliero e quello effettivamente lavorato dalla persona
	 */
	public int difference(Date date){
		if(date != null){
			LocalDate data = new LocalDate();
			data.fromDateFields(date);
			if((data.getDayOfMonth()==1) && (data.getDayOfWeek()==6 || data.getDayOfWeek()==7))
				return 0;
			if((data.getDayOfMonth()==2) && (data.getDayOfWeek()==7))
					return 0;			
			else{
				int minutiDiLavoro = timeAtWork(person,date);
				int orarioGiornaliero = 432; //valore in minuti di una giornata lavorativa
				difference = orarioGiornaliero-minutiDiLavoro;
			}
			
		}
		return difference;
	}
	
	/**
	 * 
	 * @param date
	 * @return il progressivo delle ore in più o in meno rispetto al normale orario previsto per quella data
	 */
	public int progressive(Date date){
		if(date != null){
			LocalDate data = new LocalDate();
			data.fromDateFields(date);
			if((data.getDayOfMonth()==1) && (data.getDayOfWeek()==6 || data.getDayOfWeek()==7))
				return 0;			
			if((data.getDayOfMonth()==2) && (data.getDayOfWeek()==7))
				return 0;
			else{
				progressive = progressive+difference;
			}
		}
		return progressive;
	}
	
	/**
	 * 
	 * @param person
	 * @return il nome del tipo di orario per quella persona
	 */
	public String workingTimeType(Person person){
		WorkingTimeType wtt = (WorkingTimeType) WorkingTimeType.find("Select wtt from WorkingTimeType where wtt.person = ? ", person).fetch();
		workingTimeType = wtt.description;
		
		return workingTimeType;
		
	}
	
	/**
	 * 
	 * @param date
	 * @param timeAtWork
	 * @param person
	 * @return se la persona può usufruire del buono pasto per quella data
	 */
	public boolean mealTicket(Date date, int timeAtWork, Person person){
		if(timeAtWork == 0){
			return true;
		}
			
		else{
			timeAtWork = timeAtWork(person, date);
		}
		return false;
	}
}
