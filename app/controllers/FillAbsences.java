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

/**
 * 
 * @author dario
 *
 */
public class FillAbsences extends Controller{
	protected static String mySqldriver = "com.mysql.jdbc.Driver";	
	public static void riempiAssenze() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		Connection mysqlconn = null;
		PreparedStatement stmt;

		try{
			Class.forName(mySqldriver).newInstance();
			mysqlconn = DriverManager.getConnection("jdbc:mysql://localhost:3306/IIT","root", "orologio");
			
			//conn.setAutoCommit(false);
			stmt = mysqlconn.prepareStatement("SELECT * FROM assenze limit 10");
			ResultSet rs = stmt.executeQuery();
			
			EntityManager em = JPA.em();
			Absences assenze = null;
			
			while(rs.next()){
				assenze = new Absences();
				
				assenze.matricola = rs.getInt("matricola");				
				assenze.mese = rs.getShort("mese");
				assenze.anno = rs.getShort("anno");
				assenze.codice = rs.getString("codice");
				assenze.g1 = rs.getShort("g1");
				assenze.g2 = rs.getShort("g2");
				
				assenze._save();
				
			}
			em.persist(assenze);
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
