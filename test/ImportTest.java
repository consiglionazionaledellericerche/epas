import it.cnr.iit.epas.FromMysqlToPostgres;

import java.io.IOException;
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
	public void testImportDataPerson() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
		Connection mysqlCon = FromMysqlToPostgres.getMysqlConnection();
		EntityManager em = JPA.em();
		//PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM Persone WHERE ID = " + CRISTAN_LUCCHESI_OROLOGIO_ID);
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT ID, Nome, Cognome, DataNascita, Telefono," +
				"Fax, Email, Stanza, Matricola, passwordmd5, Dipartimento, Sede " +
				"FROM Persone order by ID");
		ResultSet rs = stmt.executeQuery();
		while(rs.next()){
			Logger.info("Creazione delle info per la persona: "+rs.getString("Nome").toString()+" "+rs.getString("Cognome").toString());
			//rs.next(); // exactly one result so allowed 
					
			Person person = FromMysqlToPostgres.createPerson(rs, em);
			assertNotNull(person);
			assertNotNull(person.id);	
			
			FromMysqlToPostgres.createContactData(rs, person, em);

			FromMysqlToPostgres.createCompetence(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createValuableCompetence(rs.getInt("Matricola"),person,em);
			
			FromMysqlToPostgres.createContract(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createVacations(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createVacationType(rs.getLong("ID"), person, em);
	
//			FromMysqlToPostgres.createAbsences(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createStampings(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createYearRecap(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createMonthRecap(rs.getLong("ID"), person, em);
			
			
		}
	}
}
