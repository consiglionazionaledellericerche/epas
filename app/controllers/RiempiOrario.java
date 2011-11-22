package controllers;

import play.*;
import play.db.jpa.JPA;
import play.mvc.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import javax.persistence.EntityManager;

import models.*;

public class RiempiOrario extends Controller{

	protected static String mySqldriver = "com.mysql.jdbc.Driver";	
	public static void riempiOrario() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		Connection mysqlconn = null;
		PreparedStatement stmt;
	
		try{
			Class.forName(mySqldriver).newInstance();
			mysqlconn = DriverManager.getConnection("jdbc:mysql://localhost:3306/IIT","root", "orologio");
			mysqlconn.setAutoCommit(false);
			stmt = mysqlconn.prepareStatement("SELECT * FROM Orario limit 10");
			ResultSet rs = stmt.executeQuery();
			
			EntityManager em = JPA.em();
			Orario orario = null;
			
			while(rs.next()){
				orario = new Orario();
				
				orario.giorno = rs.getDate("Giorno");				
				orario.tipoGiorno = rs.getShort("TipoGiorno");
				orario.tipoTimbratura = rs.getShort("TipoTimbratura");
				orario.ora = rs.getTime("Ora");
				orario.ora1 = rs.getTime("Ora1");
				orario.causale = rs.getString("Causale");
				orario._save();
				
				
			}
			em.persist(orario);
			//em.close();
			//em.flush();
			mysqlconn.commit();
			mysqlconn.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
}
