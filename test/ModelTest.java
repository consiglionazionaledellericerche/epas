import it.cnr.iit.epas.FromMysqlToPostgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import models.Person;
import models.PersonDay;
import models.StampType;
import models.Stamping;
import models.Stamping.WayType;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
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
	public void testPersonDayIsWorkingDay() {
		Fixtures.loadModels("data.yml");
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
	public void testGetStampings() {
		Fixtures.loadModels("data.yml");
	}
}
