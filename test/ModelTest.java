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
import models.DailyAbsenceType;
import models.HourlyAbsenceType;
import models.Person;
import models.PersonDay;
import models.StampType;
import models.Stamping;
import models.Stamping.WayType;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.Logger;
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
		
		Stamping s = new Stamping();
		s.person = p;
		s.way = WayType.in;
		s.stampType = st;
		em.persist(s);
		assertNotNull(s.id);
		
		System.out.println("nuovo stamping id = " + s.id);
		
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
		
		Person p2 = new Person();
		p2.name= "Andrea";
		p2.surname= "Bargnani";
		assertEquals(null, p2.id);
		em.persist(p2);
		assertNotNull(p2.id);
		
		HourlyAbsenceType hat = new HourlyAbsenceType();
		hat.ignoreStamping = false;
		hat.justifiedWorkTime = 3;
		hat.mealTicketCalculation = true;		
		
		DailyAbsenceType dat = new DailyAbsenceType();		
		dat.ignoreStamping = true;
		dat.mealTicketCalculation = false;		
		
		AbsenceType absenceType = new AbsenceType();
		absenceType.code = "09s";
		absenceType.dailyAbsenceType = dat;
		
		AbsenceType absenceType2 = new AbsenceType();
		absenceType2.code = "20t";
		absenceType2.hourlyAbsenceType = hat;
		
		dat.absenceType = absenceType;
		em.persist(dat);
		hat.absenceType = absenceType2;
		em.persist(hat);
		
		Absence absence = new Absence();	
		absence.date = GregorianCalendar.getInstance().getTime();
		absence.person = p;
		absence.person = p2;
		absence.absenceType = absenceType;
		absence.absenceType = absenceType2;
		em.persist(absence);
		em.persist(absenceType);
		em.persist(absenceType2);
	}
	
	
	@Test
	public void testPersonDayIsWorkingDay() {
		Person p = Person.find("name = ?", "Cristian").first();
		assertEquals("Lucchesi", p.surname);
		assertEquals("normal", p.workingTimeType.description);
		LocalDate aMonday = new LocalDate(2011, 12, 19);
		assertEquals(DateTimeConstants.MONDAY, aMonday.getDayOfWeek());
		PersonDay personMondayDay = new PersonDay(p, aMonday);
		assertTrue(personMondayDay.isWorkingDay());
		
		LocalDate aSaturday = new LocalDate(2011, 12, 17);
		assertEquals(DateTimeConstants.SATURDAY, aSaturday.getDayOfWeek());
		PersonDay personSaturdayDay = new PersonDay(p, aSaturday);
		assertFalse(personSaturdayDay.isWorkingDay());		
	}
	
	@Test 
	public void localDateAndTime() {
		Person p = Person.find("name = ?", "Cristian").first();
		Stamping s = new Stamping();
		s.stampType = StampType.findById(1l);
		s.person = p;
		s.way = WayType.in;
		LocalDateTime date = new LocalDateTime();
		s.date = date;
		s.save();
		assertEquals(date, s.date);
		assertNotNull(s.date.hourOfDay());
		
	}
//	@Test
//	public void testGetStampings() {
//		Fixtures.loadModels("data.yml");
//	}
}
