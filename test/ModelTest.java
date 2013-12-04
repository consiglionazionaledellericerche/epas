import it.cnr.iit.epas.FromMysqlToPostgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.persistence.EntityManager;

import models.Absence;
import models.AbsenceType;
import models.Configuration;
import models.Person;
import models.PersonDay;
import models.StampType;
import models.Stamping;
import models.WorkingTimeTypeDay;
import models.Stamping.WayType;
import models.WorkingTimeType;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.test.Fixtures;
import play.test.UnitTest;

/**
 * 
 */

/**
 * @author cristian
 *
 */
public class ModelTest extends UnitTest {
	
	@Before
	public void loadFixtures() {
		Fixtures.deleteDatabase();
		Fixtures.loadModels("data.yml");
	}
	
	@Test
	public void testPersonAssociation() {
		Person p = new Person();
		p.name = "Cristian";
		p.surname = "Lucchesi";
		EntityManager em = JPA.em();
		assertEquals(null, p.id);
		em.persist(p);
		assertNotNull(p.id);
		
		StampType st = new StampType();
		st.description = "assente per motivi miei";
		em.persist(st);
		assertNotNull(st.id);

		PersonDay pd = new PersonDay(p, LocalDate.now());
		pd.save();
		assertNotNull(pd.id);

		Stamping s = new Stamping();
		s.personDay = pd;
		s.way = WayType.in;
		s.stampType = st;
		em.persist(s);
		assertNotNull(s.id);
		
		Logger.debug("nuovo stamping id = %s", s.id);
		
	}

	@Test
	public void testAbsence(){
		Person p = new Person();
		p.name ="Dario";
		p.surname="Tagliaferri";
		EntityManager em = JPA.em();
		assertEquals(null, p.id);
		em.persist(p);
		assertNotNull(p.id);
				
		AbsenceType absenceType = new AbsenceType();
		absenceType.code = "09s";
		absenceType.save();
		assertNotNull(absenceType.id);
				
		PersonDay pd = new PersonDay(p, LocalDate.now());
		pd.save();
		assertNotNull(pd.id);
		
		Absence absence = new Absence();
		absence.personDay = pd;
		absence.absenceType = absenceType;

		em.persist(absence);
		assertNotNull(absence.id);
	}
	
	
	@Test
	public void testPersonDayIsWorkingDay() {
		Person p = Person.find("name = ?", "Cristian").first();
		assertEquals("Lucchesi", p.surname);
		
		assertEquals("normal", p.getCurrentWorkingTimeType().description);
		
		for(WorkingTimeTypeDay wttd : p.getCurrentWorkingTimeType().workingTimeTypeDays) {
			Logger.info("WorkingTimeTypeDay = %s", wttd);
		}
		
		LocalDate aMonday = new LocalDate(2011, 12, 19);
		assertEquals(DateTimeConstants.MONDAY, aMonday.getDayOfWeek());
//		PersonDay personMondayDay = new PersonDay(p, aMonday);
		
//		Logger.info("dayOfWeek = %s", personMondayDay.date.getDayOfWeek());
//		assertFalse(personMondayDay.isHoliday());
//		
//		LocalDate aSaturday = new LocalDate(2011, 12, 17);
//		assertEquals(DateTimeConstants.SATURDAY, aSaturday.getDayOfWeek());
//		PersonDay personSaturdayDay = new PersonDay(p, aSaturday);
//		assertTrue(personSaturdayDay.isHoliday());		
	}
	
	@Test 
	public void localDateAndTime() {
		Person p = Person.find("name = ?", "Cristian").first();
		Stamping s = new Stamping();
		s.stampType = StampType.findById(1l);
		PersonDay pd = new PersonDay(p, LocalDate.now());
		pd.save();
		assertNotNull(pd.id);
		s.personDay = pd;
		s.way = WayType.in;
		LocalDateTime date = new LocalDateTime();
		s.date = date;
		s.save();
		assertEquals(date, s.date);
		assertNotNull(s.date.hourOfDay());
		
	}

}
