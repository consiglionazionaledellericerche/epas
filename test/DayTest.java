import java.util.ArrayList;
import java.util.List;

import models.AbsenceType;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;

import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;

import play.Logger;
import play.test.Fixtures;
import play.test.UnitTest;

/**
 * 
 * @author dario
 *
 */
public class DayTest extends UnitTest{
	
	@Before
	public void loadFixtures() {
		Fixtures.deleteDatabase();
		Fixtures.loadModels("data.yml");
	}
	
	@Test
	public void testWorkingDay() {
		
		LocalDateTime now = new LocalDateTime();
		now.now();
		
		LocalDateTime data = new LocalDateTime(2001,07,03,8,0);
		
		//LocalDateTime data = now.toLocalDate();
		System.out.println("La Localdata è: " +data);
				
		long id = 2;
		Person person = Person.findById(id);		
				
		assertNotNull(person);
		
		assertNotNull(person.workingTimeType);
		assertEquals(WorkingTimeType.findById(id), person.workingTimeType);
		System.out.println("La persona con id " +id+ "ha la seguente tipologia di lavoro: " + person.workingTimeType.description);

		PersonDay giorno = new PersonDay(person, data.toLocalDate());
		List<Stamping> timbrature = new ArrayList<Stamping>();

		timbrature = giorno.getStampings(data);
		assertNotNull(timbrature);
		
		System.out.println("Creo un personDay con data : " +now.toLocalDate());
		int giornoDiLavoro = giorno.timeAtWork(data); 
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
