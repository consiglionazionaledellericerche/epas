import java.util.ArrayList;
import java.util.List;

import models.Absence;
import models.AbsenceType;
import models.Person;
import models.PersonDay;
import models.StampType;
import models.Stamping;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.Stamping.WayType;

import org.joda.time.LocalDate;
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
	
//	@Test
//	public void testStampings(){
//		long id = 1;
//		Person person = Person.findById(id);		
//		assertNotNull(person);	
//		initializeStamping(person);
//		for(Stamping timbrature : Stamping.<Stamping>findAll()){
//			System.out.println("timbrature: " +timbrature);
//			assertNotNull(timbrature.date);
//		}
//		LocalDate datao = new LocalDate(2001,07,03);
//		PersonDay giorno = new PersonDay(person, datao);
//		
//		boolean festa = giorno.isHoliday();
//		if (festa == true)
//			System.out.println("E' festa!");
//		else
//			System.out.println("Non è festa!");
//		
//		boolean holiday = giorno.isWorkingDay();
//		System.out.println("Workday = "+holiday);
//		if (holiday == false)
//			System.out.println("Si lavora!");
//		else
//			System.out.println("Festa!");
//		
//		
//		List<Stamping> timbrature = new ArrayList<Stamping>();
//		timbrature = giorno.getStampings();
//		assertNotNull(timbrature);
//		int giornoDiLavoro = giorno.timeAtWork(); 
//		assertNotNull(giornoDiLavoro);
//		System.out.println("Ho lavorato: " +giornoDiLavoro/60+ " ore e " +giornoDiLavoro%60+ " minuti in data " +datao);
//	}
//	
//	public void initializeStamping(Person person){
//		String causa = new String ("Timbratura di ingresso");
//		String causa2 = new String ("Timbratura d'uscita per pranzo");
//		String causa3 = new String ("Timbratura di ingresso dopo pausa pranzo");
//		String causa4 = new String ("Timbratura di uscita");
//		
//		
//		Stamping s = new Stamping();
//		s.date = new LocalDateTime(2001,7,3,8,15,0);
//		s.person = person;
//		s.way = WayType.in;
//		s.stampType = StampType.find("Select s from StampType s where description = ?",causa).first();
//		s.save();
//		Stamping s1 = new Stamping();
//		s1.date = new LocalDateTime(2001,07,03,12,22,0);
//		s1.person = person;
//		s1.way = WayType.out;
//		s1.stampType = StampType.find("Select s from StampType s where description = ?",causa2).first();
//		s1.save();
//		Stamping s2 = new Stamping();
//		s2.date = new LocalDateTime(2001,7,3,13,40,0);
//		s2.person = person;
//		s2.way = WayType.in;
//		s2.stampType = StampType.find("Select s from StampType s where description = ?",causa3).first();
//		s2.save();
//		Stamping s3 = new Stamping();
//		s3.date = new LocalDateTime(2001,7,3,18,29,0);
//		s3.person = person;
//		s3.way = WayType.out;
//		s3.stampType = StampType.find("Select s from StampType s where description = ?",causa4).first();
//		s3.save();
//	}
	
	@Test
	public void testWorkingDay() {
		
		LocalDateTime now = new LocalDateTime();
		now.now();
		
		LocalDate datao = new LocalDate(2012,06,03);
		LocalDateTime data = new LocalDateTime(2001,07,03,8,0);
		
		//LocalDateTime data = now.toLocalDate();
		System.out.println("La Localdata è: " +data);
				
		
		long id = 1;
		Person person = Person.findById(id);
		assertNotNull(person);
		assertNotNull(person.workingTimeType);
		assertEquals(WorkingTimeType.findById(id), person.workingTimeType);
		System.out.println("La persona con id " +id+ "ha la seguente tipologia di lavoro: " + person.workingTimeType.description);

		PersonDay giorno = new PersonDay(person, datao);
//		List<Stamping> timbrature = new ArrayList<Stamping>();
//
//		timbrature = giorno.getStampings();
//		assertNotNull(timbrature);
//		
//		System.out.println("Creo un personDay con data : " +data.toLocalDate());
//		int giornoDiLavoro = giorno.timeAtWork(); 
//		assertNotNull(giornoDiLavoro);
//		
//		System.out.println("Ho lavorato: " +giornoDiLavoro+ " minuti in data " +data.toLocalDate());
//		
//		boolean festa = giorno.isHoliday();
//		if (festa == true)
//			System.out.println("E' festa!");
//		else
//			System.out.println("Non è festa!");
//		
//		boolean workday = giorno.isWorkingDay();		
//		if (workday == true)
//			System.out.println("Si lavora!");
//		else
//			System.out.println("Festa!");
		
		
		List<Absence> listaAssenze = giorno.absenceList();
		assertNotNull(listaAssenze);
		if(listaAssenze != null){
			for (Absence abt : listaAssenze) {
				Logger.warn("Codice: " +abt.absenceType.code);
				Logger.warn("Codice: " +abt.absenceType.description);
			}
		}			
		else
			Logger.warn("Non ci sono assenze" );
		
	}
}
