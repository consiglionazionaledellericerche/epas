import it.cnr.iit.epas.FromMysqlToPostgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import models.Person;

import org.junit.Test;

import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * 
 */

/**
 * @author cristian
 *
 */
public class ImportTest extends UnitTest {

	private final static short CRISTAN_LUCCHESI_OROLOGIO_ID = 146;
	
	@Test
	public void testImportDataPerson() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Connection mysqlCon = FromMysqlToPostgres.getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM Persone WHERE id = " + CRISTAN_LUCCHESI_OROLOGIO_ID);
		ResultSet rs = stmt.executeQuery();
		rs.next(); // exactly one result so allowed 
		
		EntityManager em = JPA.em();
		
		Person person = FromMysqlToPostgres.createPerson(rs, em);
		assertNotNull(person);
		assertNotNull(person.id);
		assertEquals("Cristian", person.name);
		assertEquals("Lucchesi", person.surname);
		
		FromMysqlToPostgres.createLocation(rs, person, em);
		FromMysqlToPostgres.createContactData(rs, person, em);
		
		FromMysqlToPostgres.createVacations(CRISTAN_LUCCHESI_OROLOGIO_ID, person, em);
//		
		FromMysqlToPostgres.createAbsences(CRISTAN_LUCCHESI_OROLOGIO_ID, person, em);
		FromMysqlToPostgres.createWorkingTimeTypes(CRISTAN_LUCCHESI_OROLOGIO_ID, person, em);
		FromMysqlToPostgres.createStampings(CRISTAN_LUCCHESI_OROLOGIO_ID, person, em);
//		
		FromMysqlToPostgres.createYearRecap(CRISTAN_LUCCHESI_OROLOGIO_ID, person, em);
//		
		FromMysqlToPostgres.createMonthRecap(CRISTAN_LUCCHESI_OROLOGIO_ID, person, em);
//		
		FromMysqlToPostgres.createCompetence(CRISTAN_LUCCHESI_OROLOGIO_ID, person, em);
//		
//		FromMysqlToPostgres.fillOtherTables();
		
//		long stampingsCount = Stamping.count("person = ?", person); 
//		assertTrue("Dovrebbe essere stato inserita almeno una timbratura, invece sono zero.", 
//			 stampingsCount > 0);
//		
//		System.out.println(String.format("Sono state inserite %d timbrature per l'utente con id = %d su orologio", stampingsCount, CRISTAN_LUCCHESI_OROLOGIO_ID));
	}
}
