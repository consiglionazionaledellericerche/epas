import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import org.junit.Test;

import it.cnr.iit.epas.FromMysqlToPostgres;
import play.db.jpa.JPA;
import play.test.UnitTest;


public class ParameterTest extends UnitTest{
	
	@Test
	public void testParameter() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
		System.out.println("Sono nel test di importdata person");
		FromMysqlToPostgres.createParameters();
	}
	
}
