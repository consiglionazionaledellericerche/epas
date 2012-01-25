import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import models.AbsenceType;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.WorkingTimeTypeDay;

import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;

import play.Logger;
import play.Play;
import play.test.Fixtures;
import play.test.UnitTest;

/**
 * 
 * @author dario
 *
 */
public class DayTest extends UnitTest{
	
//	private final static long CRISTAN_LUCCHESI_OROLOGIO_ID = 146;
	private static Connection postgresqlConn = null;
	public static String myPostgresDriver = Play.configuration.getProperty("db.new.driver");
	
	
	public static Connection getMyPostgresConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		if (postgresqlConn != null ) {
			return postgresqlConn;
		}
		Class.forName(myPostgresDriver).newInstance();

		return DriverManager.getConnection(
				//Play.configuration.getProperty("db.new"));
			Play.configuration.getProperty("db.new.url"),
			Play.configuration.getProperty("db.new.user"),
			Play.configuration.getProperty("db.new.password"));
				
	}
	
	@Before
	public void loadFixtures() {
		Fixtures.deleteDatabase();
		Fixtures.loadModels("data.yml");
	}
	
	@Test
	public void testWorkingDay() {
		
		LocalDateTime now = new LocalDateTime();
		now.now();
		
		LocalDateTime data = new LocalDateTime(2000,07,03, 8,0);
		
		//LocalDateTime data = now.toLocalDate();
		System.out.println("La Localdata è: " +data);
				
		long id = 1;
		Person person = Person.findById(1);		
				
		assertNotNull(person);
		
		assertNotNull(person.workingTimeType);
		assertEquals(WorkingTimeTypeDay.findById(1), person.workingTimeType);
		System.out.println("La persona con id " +id+ "ha la seguente tipologia di lavoro: " + person.workingTimeType.description);
		
		PersonDay giorno = new PersonDay(person, data.toLocalDate());
		List<Stamping> timbrature = new ArrayList<Stamping>();

		timbrature = giorno.getStampings();
		assertNotNull(timbrature);
		
		System.out.println("Creo un personDay con data : " +now.toLocalDate());
		int giornoDiLavoro = giorno.timeAtWork(); 
		assertNotNull(giornoDiLavoro);
		
		System.out.println("Ho lavorato: " +giornoDiLavoro+ "minuti in data " +data);
		
		boolean festa = giorno.isHoliday();
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
			for (AbsenceType abt : listaAssenze) {
				Logger.warn("Codice: " +abt.code);
			}
		}			
		else
			Logger.warn("Non ci sono assenze" );
		
	}
}
