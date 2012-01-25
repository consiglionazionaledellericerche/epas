import it.cnr.iit.epas.FromMysqlToPostgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import play.Logger;
import play.Play;
import play.test.UnitTest;
import models.AbsenceType;

import models.Absence;
import models.Person;
import models.PersonDay;
import models.Stamping;

/**
 * 
 * @author dario
 *
 */
public class DayTest extends UnitTest{
	
//	private final static long CRISTAN_LUCCHESI_OROLOGIO_ID = 146;
//	private static Connection postgresqlConn = null;
//	public static String myPostgresDriver = Play.configuration.getProperty("db.new.driver");
	
	
//	public static Connection getMyPostgresConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
//		if (postgresqlConn != null ) {
//			return postgresqlConn;
//		}
//		Class.forName(myPostgresDriver).newInstance();
//
//		return DriverManager.getConnection(
//				//Play.configuration.getProperty("db.new"));
//			Play.configuration.getProperty("db.new.url"),
//			Play.configuration.getProperty("db.new.user"),
//			Play.configuration.getProperty("db.new.password"));
//				
//	}
	
	@Test
	public void testWorkingDay() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		
	//	Connection postgresCon = getMyPostgresConnection();
		LocalDateTime now = new LocalDateTime();
		now.now();
		
		LocalDateTime data = new LocalDateTime(2000,07,03, 8,0);
		
		//LocalDateTime data = now.toLocalDate();
		System.out.println("La Localdata è: " +data);
				
		long id = 139;
		Person person = Person.findById(id);		
				
		//assertNotNull(person);
		PersonDay giorno = new PersonDay(person, data.toLocalDate());
		List<Stamping> timbrature = new ArrayList<Stamping>();

		
		String tipoLavoro = giorno.workingTimeType(person);
		System.out.println("La persona con id " +id+ "ha la seguente tipologia di lavoro: " +tipoLavoro);
		
		timbrature = giorno.getStampings(person, data);
		assertNotNull(timbrature);
		
		System.out.println("Creo un personDay con data : " +now.toLocalDate());
		int giornoDiLavoro = giorno.timeAtWork(person,data); 
		assertNotNull(giornoDiLavoro);
		
		System.out.println("Ho lavorato: " +giornoDiLavoro+ "minuti in data " +data);
		
		boolean festa = giorno.isHoliday(data.toLocalDate());
		if (festa == true)
			System.out.println("E' festa!");
		else
			System.out.println("Non è festa!");
		
		boolean workday = giorno.isWorkingDay();		
		if (workday == true)
			System.out.println("Si lavora!");
		else
			System.out.println("Festa!");
		
		
		List<AbsenceType> listaAssenze = giorno.absenceList();
		assertNotNull(listaAssenze);
		if(listaAssenze != null){
			Iterator iter = listaAssenze.iterator();
			while(iter.hasNext()){
				AbsenceType abt = (AbsenceType) iter.next(); 
				Logger.warn("Codice: " +abt.code);
			    //System.out.print("Codice: " +abt.code );
			}
		}			
		else
			Logger.warn("Non ci sono assenze" );
		
	}
}
