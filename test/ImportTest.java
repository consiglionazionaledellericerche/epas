import it.cnr.iit.epas.FromMysqlToPostgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import models.Person;

import org.junit.Test;

import play.db.jpa.JPA;
import play.Logger;
import play.test.UnitTest;

/**
 * 
 */

/**
 * @author cristian
 *
 */
public class ImportTest extends UnitTest {

	private final static long CRISTAN_LUCCHESI_OROLOGIO_ID = 146;
	
	@Test
	public void testImportDataPerson() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Connection mysqlCon = FromMysqlToPostgres.getMysqlConnection();
		EntityManager em = JPA.em();
		//PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM Persone WHERE id = " + CRISTAN_LUCCHESI_OROLOGIO_ID);
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT ID, Nome, Cognome, DataNascita, Telefono," +
				"Fax, Email, Stanza, Matricola, Dipartimento, Sede FROM Persone order by ID");
		ResultSet rs = stmt.executeQuery();
		while(rs.next()){
			Logger.warn("Creazione delle info per la persona: "+rs.getString("Nome").toString()+" "+rs.getString("Cognome").toString());
			//rs.next(); // exactly one result so allowed 
					
			Person person = FromMysqlToPostgres.createPerson(rs, em);
			assertNotNull(person);
			assertNotNull(person.id);
			//assertEquals("Cristian", person.name);
			//assertEquals("Lucchesi", person.surname);
			
			FromMysqlToPostgres.createLocation(rs, person, em);
			FromMysqlToPostgres.createContactData(rs, person, em);
			
			FromMysqlToPostgres.createContract(person.id, person, em);
			
			FromMysqlToPostgres.createVacations(person.id, person, em);
	
			FromMysqlToPostgres.createAbsences(person.id, person, em);
			FromMysqlToPostgres.createWorkingTimeTypes(person.id, person, em);
			FromMysqlToPostgres.createStampings(person.id, person, em);
			
			FromMysqlToPostgres.createYearRecap(person.id, person, em);
			
			FromMysqlToPostgres.createMonthRecap(person.id, person, em);
			
			FromMysqlToPostgres.createCompetence(person.id, person, em);
		}
//		FromMysqlToPostgres.fillOtherTables();
		
//		long stampingsCount = Stamping.count("person = ?", person); 
//		assertTrue("Dovrebbe essere stato inserita almeno una timbratura, invece sono zero.", 
//			 stampingsCount > 0);
//		
//		System.out.println(String.format("Sono state inserite %d timbrature per l'utente con id = %d su orologio", stampingsCount, CRISTAN_LUCCHESI_OROLOGIO_ID));
	}
}
